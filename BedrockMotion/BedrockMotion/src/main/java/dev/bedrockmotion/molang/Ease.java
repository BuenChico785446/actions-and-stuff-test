package dev.bedrockmotion.molang;

import java.util.function.DoubleUnaryOperator;

final class Ease {

    private Ease() {}

    static DoubleUnaryOperator byName(String n) {
        return switch (n) {
            case "in_sine" -> t -> 1 - Math.cos(t * Math.PI / 2);
            case "out_sine" -> t -> Math.sin(t * Math.PI / 2);
            case "in_out_sine" -> t -> -(Math.cos(Math.PI * t) - 1) / 2;
            case "in_quad" -> t -> t * t;
            case "out_quad" -> t -> 1 - (1 - t) * (1 - t);
            case "in_out_quad" -> t -> t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            case "in_cubic" -> t -> t * t * t;
            case "out_cubic" -> t -> 1 - Math.pow(1 - t, 3);
            case "in_out_cubic" -> t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
            case "in_quart" -> t -> t * t * t * t;
            case "out_quart" -> t -> 1 - Math.pow(1 - t, 4);
            case "in_out_quart" -> t -> t < 0.5 ? 8 * t * t * t * t : 1 - Math.pow(-2 * t + 2, 4) / 2;
            case "in_quint" -> t -> Math.pow(t, 5);
            case "out_quint" -> t -> 1 - Math.pow(1 - t, 5);
            case "in_out_quint" -> t -> t < 0.5 ? 16 * Math.pow(t, 5) : 1 - Math.pow(-2 * t + 2, 5) / 2;
            case "in_expo" -> t -> t == 0 ? 0 : Math.pow(2, 10 * t - 10);
            case "out_expo" -> t -> t == 1 ? 1 : 1 - Math.pow(2, -10 * t);
            case "in_out_expo" -> t -> t == 0 ? 0 : t == 1 ? 1
                    : t < 0.5 ? Math.pow(2, 20 * t - 10) / 2 : (2 - Math.pow(2, -20 * t + 10)) / 2;
            case "in_circ" -> t -> 1 - Math.sqrt(1 - t * t);
            case "out_circ" -> t -> Math.sqrt(1 - Math.pow(t - 1, 2));
            case "in_out_circ" -> t -> t < 0.5
                    ? (1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2
                    : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
            case "in_back" -> t -> 2.70158 * t * t * t - 1.70158 * t * t;
            case "out_back" -> t -> 1 + 2.70158 * Math.pow(t - 1, 3) + 1.70158 * Math.pow(t - 1, 2);
            case "in_out_back" -> t -> {
                double c2 = 1.70158 * 1.525;
                return t < 0.5
                        ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                        : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
            };
            case "in_elastic" -> t -> t == 0 ? 0 : t == 1 ? 1
                    : -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * (2 * Math.PI / 3));
            case "out_elastic" -> t -> t == 0 ? 0 : t == 1 ? 1
                    : Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1;
            case "in_out_elastic" -> t -> {
                if (t == 0) return 0;
                if (t == 1) return 1;
                double c5 = 2 * Math.PI / 4.5;
                return t < 0.5
                        ? -(Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * c5)) / 2
                        : (Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * c5)) / 2 + 1;
            };
            case "in_bounce" -> t -> 1 - bounceOut(1 - t);
            case "out_bounce" -> Ease::bounceOut;
            case "in_out_bounce" -> t -> t < 0.5
                    ? (1 - bounceOut(1 - 2 * t)) / 2
                    : (1 + bounceOut(2 * t - 1)) / 2;
            default -> null;
        };
    }

    private static double bounceOut(double t) {
        double n1 = 7.5625;
        double d1 = 2.75;
        if (t < 1 / d1) return n1 * t * t;
        if (t < 2 / d1) return n1 * (t -= 1.5 / d1) * t + 0.75;
        if (t < 2.5 / d1) return n1 * (t -= 2.25 / d1) * t + 0.9375;
        return n1 * (t -= 2.625 / d1) * t + 0.984375;
    }
}
