package dev.bedrockmotion.molang;

import java.util.ArrayList;
import java.util.List;

final class MolangParser {

    private static final int EOF = 0, NUM = 1, STR = 2, ID = 3, OP = 4;

    private record Tok(int kind, String text, double num) {}

    private final Symbols symbols;
    private final List<String> warnings;
    private final List<Tok> toks = new ArrayList<>();
    private int p;

    private MolangParser(String src, Symbols symbols, List<String> warnings) {
        this.symbols = symbols;
        this.warnings = warnings;
        lex(src);
    }

    static Expr compile(String src, Symbols symbols, List<String> warnings) {
        MolangParser parser = new MolangParser(src, symbols, warnings);
        Expr e = parser.script(false);
        if (parser.peek().kind != EOF) throw new MolangException("unexpected token '" + parser.peek().text + "'");
        return e;
    }

    private void lex(String s) {
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                i++;
            } else if ((c >= '0' && c <= '9') || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                int st = i;
                while (i < n && Character.isDigit(s.charAt(i))) i++;
                if (i < n && s.charAt(i) == '.') {
                    i++;
                    while (i < n && Character.isDigit(s.charAt(i))) i++;
                }
                if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                    int j = i + 1;
                    if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) j++;
                    if (j < n && Character.isDigit(s.charAt(j))) {
                        while (j < n && Character.isDigit(s.charAt(j))) j++;
                        i = j;
                    }
                }
                double value = Double.parseDouble(s.substring(st, i));
                if (i < n && (s.charAt(i) == 'f' || s.charAt(i) == 'F')
                        && (i + 1 >= n || !(Character.isLetterOrDigit(s.charAt(i + 1)) || s.charAt(i + 1) == '_'))) {
                    i++;
                }
                toks.add(new Tok(NUM, "", value));
            } else if (Character.isLetter(c) || c == '_') {
                int st = i;
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
                toks.add(new Tok(ID, s.substring(st, i).toLowerCase(), 0));
            } else if (c == '\'') {
                int e = s.indexOf('\'', i + 1);
                if (e < 0) throw new MolangException("string not closed");
                toks.add(new Tok(STR, s.substring(i + 1, e), 0));
                i = e + 1;
            } else {
                String two = i + 1 < n ? s.substring(i, i + 2) : "";
                switch (two) {
                    case "==", "!=", "<=", ">=", "&&", "||", "??", "->" -> {
                        toks.add(new Tok(OP, two, 0));
                        i += 2;
                        continue;
                    }
                    default -> {}
                }
                if ("+-*/(),;?:{}[]=<>!.".indexOf(c) < 0) throw new MolangException("unexpected character '" + c + "'");
                toks.add(new Tok(OP, String.valueOf(c), 0));
                i++;
            }
        }
        toks.add(new Tok(EOF, "<end>", 0));
    }

    private Tok peek() {
        return toks.get(p);
    }

    private Tok peekAt(int off) {
        return toks.get(Math.min(p + off, toks.size() - 1));
    }

    private Tok next() {
        return toks.get(p++);
    }

    private boolean isOp(String op) {
        Tok t = peek();
        return t.kind == OP && t.text.equals(op);
    }

    private boolean accept(String op) {
        if (isOp(op)) {
            p++;
            return true;
        }
        return false;
    }

    private void expect(String op) {
        if (!accept(op)) throw new MolangException("'" + op + "' expected, found '" + peek().text + "'");
    }

    private Expr script(boolean inBlock) {
        List<Expr> stmts = new ArrayList<>();
        boolean trailingSemicolon = false;
        while (true) {
            while (accept(";")) trailingSemicolon = true;
            Tok t = peek();
            if (t.kind == EOF || (inBlock && isOp("}"))) break;
            stmts.add(assign());
            trailingSemicolon = false;
            if (!isOp(";")) break;
        }
        if (stmts.isEmpty()) return new Nodes.Const(0);
        if (stmts.size() == 1 && !trailingSemicolon) return stmts.get(0);
        return new Nodes.Seq(stmts.toArray(new Expr[0]), !trailingSemicolon);
    }

    private Expr assign() {
        Expr left = coalesce();
        if (isOp("=")) {
            if (!(left instanceof Nodes.Ref ref)) throw new MolangException("assignment to a non-variable");
            p++;
            return new Nodes.Assign(ref, assign());
        }
        return left;
    }

    private Expr coalesce() {
        Expr l = ternary();
        while (accept("??")) l = new Nodes.Coalesce(l, ternary());
        return l;
    }

    private Expr ternary() {
        Expr cond = or();
        if (accept("?")) {
            Expr a = assign();
            Expr b = null;
            if (accept(":")) b = assign();
            return new Nodes.Ternary(cond, a, b);
        }
        return cond;
    }

    private Expr or() {
        Expr l = and();
        while (accept("||")) l = new Nodes.Or(l, and());
        return l;
    }

    private Expr and() {
        Expr l = equality();
        while (accept("&&")) l = new Nodes.And(l, equality());
        return l;
    }

    private Expr equality() {
        Expr l = relational();
        while (true) {
            if (accept("==")) l = new Nodes.Eq(false, l, relational());
            else if (accept("!=")) l = new Nodes.Eq(true, l, relational());
            else return l;
        }
    }

    private Expr relational() {
        Expr l = additive();
        while (true) {
            int op;
            if (accept("<=")) op = Nodes.LE;
            else if (accept(">=")) op = Nodes.GE;
            else if (accept("<")) op = Nodes.LT;
            else if (accept(">")) op = Nodes.GT;
            else return l;
            l = fold(new Nodes.Arith(op, l, additive()));
        }
    }

    private Expr additive() {
        Expr l = multiplicative();
        while (true) {
            int op;
            if (accept("+")) op = Nodes.ADD;
            else if (accept("-")) op = Nodes.SUB;
            else return l;
            l = fold(new Nodes.Arith(op, l, multiplicative()));
        }
    }

    private Expr multiplicative() {
        Expr l = unary();
        while (true) {
            int op;
            if (accept("*")) op = Nodes.MUL;
            else if (accept("/")) op = Nodes.DIV;
            else return l;
            l = fold(new Nodes.Arith(op, l, unary()));
        }
    }

    private Expr fold(Nodes.Arith a) {
        return a.l.isConst() && a.r.isConst() ? new Nodes.Const(a.num(null)) : a;
    }

    private Expr unary() {
        if (accept("-")) {
            Expr e = unary();
            return e instanceof Nodes.Const c ? new Nodes.Const(-c.v) : new Nodes.Neg(e);
        }
        if (accept("+")) return unary();
        if (accept("!")) return new Nodes.Not(unary());
        return postfix();
    }

    private Expr postfix() {
        Expr e = primary();
        while (true) {
            if (accept("->")) {
                e = new Nodes.Arrow(e, primary());
            } else {
                return e;
            }
        }
    }

    private Expr primary() {
        Tok t = next();
        switch (t.kind) {
            case NUM:
                return new Nodes.Const(t.num);
            case STR:
                return new Nodes.Str(t.text);
            case ID:
                return identifier(t.text);
            case OP:
                if (t.text.equals("(")) {
                    Expr e = assign();
                    expect(")");
                    return e;
                }
                if (t.text.equals("{")) {
                    Expr e = script(true);
                    expect("}");
                    return e;
                }
                break;
            default:
                break;
        }
        throw new MolangException("unexpected token '" + t.text + "'");
    }

    private String[] fieldPath() {
        List<String> path = new ArrayList<>();
        while (isOp(".") && peekAt(1).kind == ID) {
            p++;
            path.add(next().text);
        }
        return path.toArray(new String[0]);
    }

    private String fieldName() {
        expect(".");
        Tok t = next();
        if (t.kind != ID) throw new MolangException("name expected after '.'");
        return t.text;
    }

    private Expr identifier(String id) {
        switch (id) {
            case "this":
                return new Nodes.This();
            case "true":
                return new Nodes.Const(1);
            case "false":
                return new Nodes.Const(0);
            case "return": {
                Tok t = peek();
                boolean empty = t.kind == EOF || isOp(";") || isOp("}");
                return new Nodes.Flow(Env.CTL_RETURN, empty ? null : assign());
            }
            case "break":
                return new Nodes.Flow(Env.CTL_BREAK, null);
            case "continue":
                return new Nodes.Flow(Env.CTL_CONTINUE, null);
            case "loop": {
                expect("(");
                Expr count = assign();
                expect(",");
                Expr body = assign();
                expect(")");
                return new Nodes.Loop(count, body);
            }
            case "for_each": {
                expect("(");
                Expr target = assign();
                if (!(target instanceof Nodes.Ref ref)) throw new MolangException("for_each: variable expected");
                expect(",");
                Expr array = assign();
                expect(",");
                Expr body = assign();
                expect(")");
                return new Nodes.ForEach(ref, array, body);
            }
            case "v":
            case "variable": {
                String name = fieldName();
                return new Nodes.VarRef(symbols, name, fieldPath());
            }
            case "t":
            case "temp": {
                String name = fieldName();
                return new Nodes.TempRef(symbols, name, fieldPath());
            }
            case "c":
            case "context":
                return new Nodes.ContextRef(fieldName());
            case "q":
            case "query": {
                String name = fieldName();
                Expr[] args = isOp("(") ? arguments() : new Expr[0];
                return new Nodes.QueryCall(Queries.get(name), args);
            }
            case "math": {
                String name = fieldName();
                Expr[] args = isOp("(") ? arguments() : new Expr[0];
                Expr m = MathFns.build(name, args);
                if (m == null) {
                    warnings.add("unknown function math." + name);
                    return new Nodes.Const(0);
                }
                return foldMath(m);
            }
            case "array": {
                String name = "array." + fieldName();
                Expr index = null;
                if (accept("[")) {
                    index = assign();
                    expect("]");
                }
                return new Nodes.ArrayRef(name, index);
            }
            case "geometry":
            case "texture":
            case "material":
                return new Nodes.Str(id + "." + fieldName());
            default:
                warnings.add("unknown identifier " + id);
                fieldPath();
                return new Nodes.Const(0);
        }
    }

    private Expr foldMath(Expr m) {
        boolean allConst;
        if (m instanceof MathFns.M1 a) allConst = a.a.isConst();
        else if (m instanceof MathFns.M2 a) allConst = a.a.isConst() && a.b.isConst();
        else if (m instanceof MathFns.M3 a) allConst = a.a.isConst() && a.b.isConst() && a.c.isConst();
        else return m;
        return allConst ? new Nodes.Const(m.num(null)) : m;
    }

    private Expr[] arguments() {
        expect("(");
        List<Expr> args = new ArrayList<>();
        if (!isOp(")")) {
            do {
                args.add(assign());
            } while (accept(","));
        }
        expect(")");
        return args.toArray(new Expr[0]);
    }
}
