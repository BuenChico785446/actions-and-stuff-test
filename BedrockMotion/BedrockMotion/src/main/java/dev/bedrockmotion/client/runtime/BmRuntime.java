package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.bedrock.BedrockPack;
import dev.bedrockmotion.bedrock.GeometryDef;
import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.EntityAnimator;
import dev.bedrockmotion.bedrock.EntityIds;
import dev.bedrockmotion.bedrock.GeometryDef;
import dev.bedrockmotion.bedrock.LocatorSolver;
import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.bedrock.PoseHierarchy;
import dev.bedrockmotion.bedrock.RenderPlan;
import dev.bedrockmotion.bedrock.RenderPlanner;
import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.client.render.BmFrame;
import dev.bedrockmotion.molang.BoneSolver;
import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.Struct;
import dev.bedrockmotion.molang.EntitySnapshot;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class BmRuntime {

    private static final double MODEL_UNIT = 1.0 / 16.0;

    static final class Entry {
        EntityAnimator animator;
        BmFrame frame;
        long lastNanos;
        long usedNanos;
        double walked;
        double walkSpeed;
        double lastAttack;
        double kineticHitAt = Double.NaN;
        PoseHierarchy hierarchy;
        boolean hierarchyResolved;
        boolean failed;
        ParticleSystem particles;
        GeometryDef lastGeometry;
        final BmProbe probe = new BmProbe();
    }

    public static final class Features {
        public volatile boolean entities = true;
        public volatile boolean driver = true;
        public volatile boolean attachables = true;
        public volatile boolean equipment = true;
        public volatile boolean particles = true;
        public volatile boolean textureMeshes = true;
        public volatile boolean sounds = true;

        public boolean get(String name) {
            return switch (name) {
                case "entities" -> entities;
                case "driver" -> driver;
                case "attachables" -> attachables;
                case "equipment" -> equipment;
                case "particles" -> particles;
                case "textureMeshes" -> textureMeshes;
                case "sounds" -> sounds;
                default -> false;
            };
        }

        public void set(String name, boolean value) {
            switch (name) {
                case "entities" -> entities = value;
                case "driver" -> driver = value;
                case "attachables" -> attachables = value;
                case "equipment" -> equipment = value;
                case "particles" -> particles = value;
                case "textureMeshes" -> textureMeshes = value;
                case "sounds" -> sounds = value;
                default -> {}
            }
        }
    }

    public static final Features features = new Features();
    public static volatile boolean replacePlayers;

    private static volatile Map<String, ClientEntityDef> entityDefs = Map.of();
    private static volatile Map<String, ClientEntityDef> attachableDefs = Map.of();
    private static volatile List<BedrockPack> packs = List.of();
    private static final Map<EntityType<?>, ClientEntityDef> TYPE_CACHE = new IdentityHashMap<>();
    private static final Map<GeometryDef, PoseHierarchy> HIERARCHIES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
    private static final Map<String, Entry> ATTACHMENTS = new HashMap<>();
    private static long lastCleanup;
    private static long extractCalls;
    private static long replacedCalls;
    private static boolean warnedNoHook;
    private static long lastExtractNanos;
    private static long lastWorldNanos;
    private static long worldFrame;
    private static final EquipmentSlot[] EQUIP_SLOTS = {
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String[] EQUIP_NAMES = {"main_hand", "off_hand", "head", "torso", "legs", "feet"};

    private BmRuntime() {}

    public static void apply(List<BedrockPack> loaded) {
        BedrockPack.stack(loaded);
        dev.bedrockmotion.bedrock.EntityStack.Result stacked = dev.bedrockmotion.bedrock.EntityStack.build(loaded);
        Map<String, ClientEntityDef> defs = stacked.entities();
        Map<String, ClientEntityDef> attachables = stacked.attachables();
        entityDefs = defs;
        attachableDefs = attachables;
        packs = loaded;
        TYPE_CACHE.clear();
        ENTRIES.clear();
        ATTACHMENTS.clear();
        BmParticleWorld.clear();
        BmTextures.clear();
        BedrockMotionClient.LOGGER.info(
                "BedrockMotion: {} entity definitions and {} attachables active from {} pack(s)",
                defs.size(), attachables.size(), loaded.size());
        for (BedrockPack p : loaded) {
            BedrockMotionClient.LOGGER.info("BedrockMotion: stack entry {} (base pack: {})", p.name, p.baseOnly);
        }
        List<dev.bedrockmotion.bedrock.ClientEntityDef> all = new ArrayList<>(defs.values());
        all.addAll(attachables.values());
        for (String line : dev.bedrockmotion.bedrock.StackReport.unresolved(all, 12)) {
            BedrockMotionClient.LOGGER.info("BedrockMotion: {}", line);
        }
    }

    public static boolean hasAttachables() {
        return features.attachables && !attachableDefs.isEmpty();
    }

    static ClientEntityDef attachable(String itemId) {
        return attachableDefs.get(itemId.toLowerCase(Locale.ROOT));
    }

    private static void runParentSetups(LivingEntity living, Env ownerEnv) {
        for (EquipmentSlot slot : EQUIP_SLOTS) {
            ItemStack stack = living.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            ClientEntityDef def = attachable(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            if (def != null && def.parentSetup != null) def.parentSetup.run(ownerEnv);
        }
    }

    static void linkOwner(Env env, Entry owner) {
        env.owner = owner == null ? null : owner.animator.env;
        env.ownerPublic = owner == null ? Set.of() : owner.animator.def.publicVariables;
        if (env.owner != null) env.context.put("owning_entity", env.owner);
        else env.context.remove("owning_entity");
    }

    static void linkOwner(Env env, int entityId) {
        linkOwner(env, ENTRIES.get(entityId));
    }

    static Entry attachmentEntry(String key, ClientEntityDef def) {
        Entry e = ATTACHMENTS.get(key);
        if (e == null || e.animator.def != def) {
            e = new Entry();
            e.animator = new EntityAnimator(def);
            ATTACHMENTS.put(key, e);
        }
        e.usedNanos = System.nanoTime();
        return e;
    }

    public static void extract(Entity entity, EntityRenderState state, float partialTick) {
        BmStateHolder holder = (BmStateHolder) (Object) state;
        holder.bm$setFrame(null);
        Vec3 renderPos = entity.getPosition(partialTick);
        holder.bm$setOrigin(new double[] {renderPos.x, renderPos.y, renderPos.z});
        frameGate(System.nanoTime());
        extractCalls++;
        Map<String, ClientEntityDef> defs = entityDefs;
        if (defs.isEmpty()) return;
        ClientEntityDef def = lookup(entity, defs);
        if (def == null) return;
        long now = System.nanoTime();
        Entry e = ENTRIES.get(entity.getId());
        if (e == null || e.animator.def != def) {
            e = new Entry();
            e.animator = new EntityAnimator(def);
            installBones(e);
            ENTRIES.put(entity.getId(), e);
        }
        e.usedNanos = now;
        if (e.failed) return;
        try {
            boolean paused = Minecraft.getInstance().isPaused();
            double dt = paused || e.lastNanos == 0 ? 0 : Math.min(0.1, Math.max(0, (now - e.lastNanos) / 1e9));
            if (paused) e.lastNanos = now;
            if (e.frame == null || dt >= 0.0005) {
                e.probe.entity = entity;
                e.animator.env.probe = e.probe;
                float yaw = fill(entity, e, e.animator.env.entity, partialTick, dt);
                e.animator.update(dt, (entity.tickCount + partialTick) / 20.0, partialTick);
                if (features.equipment && entity instanceof LivingEntity worn) runParentSetups(worn, e.animator.env);
                RenderPlan plan = RenderPlanner.plan(e.animator);
                if (!features.entities || (def.identifier.equals("minecraft:player") && !replacePlayers)) plan.layers.clear();
                BmModelPose driver = plan.layers.isEmpty() && features.driver ? BmModelPose.capture(e.animator.pose, hierarchyOf(e, def)) : null;
                boolean hurt = entity instanceof LivingEntity living && (living.hurtTime > 0 || living.deathTime > 0);
                e.lastGeometry = plan.layers.isEmpty() ? null : plan.layers.get(0).geometry;
                Identifier skin = def.identifier.equals("minecraft:player") && !plan.layers.isEmpty() ? BmPlayerSkin.resolve(entity) : null;
                List<ParticleSystem.EmitterView> views = updateParticles(entity, e, plan, yaw, dt);
                BmFrame.Context ctx = new BmFrame.Context(hurt, entity.isOnFire(), skin);
                e.frame = BmFrame.build(e.animator, plan, yaw, ctx, driver, views, null);
                if (features.equipment && !plan.layers.isEmpty() && entity instanceof LivingEntity living) {
                    e.frame.setAttachments(buildEquipment(entity, living, e, plan, yaw, dt, partialTick));
                }
                BedrockMotionClient.once(
                        "mode:" + def.identifier + (e.animator.pose.touchedCount() > 0),
                        "BedrockMotion: {} uses layers={} poseBones={} (replacePlayers={}, driver={}, equipment={})",
                        def.identifier, plan.layers.size(), e.animator.pose.touchedCount(), replacePlayers, features.driver, features.equipment);
                if (!plan.layers.isEmpty()) {
                    BedrockMotionClient.once(
                            "replace:" + def.identifier,
                            "BedrockMotion: replacing {} with geometry {} ({} layer(s), texture {})",
                            def.identifier, plan.layers.get(0).geometry.id, plan.layers.size(), plan.layers.get(0).texturePath);
                } else if (driver != null) {
                    BedrockMotionClient.once("drive:" + def.identifier, "BedrockMotion: applying pack animations to the vanilla model of {}", def.identifier);
                }
                if (e.frame.hasParticles()) {
                    BedrockMotionClient.once("particles:" + def.identifier, "BedrockMotion: {} is spawning pack particles", def.identifier);
                }
                e.lastNanos = now;
                playSounds(entity, e.animator);
            }
            if (e.frame.replacesModel() || e.frame.driver() != null || e.frame.hasParticles()) {
                holder.bm$setFrame(e.frame);
                replacedCalls++;
            }
        } catch (RuntimeException ex) {
            e.failed = true;
            BedrockMotionClient.LOGGER.error("BedrockMotion: {} falls back to vanilla rendering", def.identifier, ex);
        }
        cleanup(now);
    }

    public static long worldFrame() {
        return worldFrame;
    }

    static boolean ownerReplaced(int entityId) {
        Entry e = ENTRIES.get(entityId);
        return e != null && e.frame != null && e.frame.replacesModel();
    }

    private static void frameGate(long now) {
        if (now - lastExtractNanos > 1_500_000L) {
            worldFrame++;
            double dt = lastWorldNanos == 0 || Minecraft.getInstance().isPaused() ? 0 : Math.min(0.1, (now - lastWorldNanos) / 1e9);
            lastWorldNanos = now;
            BmParticleWorld.update(dt);
            adoptStale(now);
        }
        lastExtractNanos = now;
    }

    private static void adoptStale(long now) {
        if (ENTRIES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Iterator<Map.Entry<Integer, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Entry> me = it.next();
            Entry e = me.getValue();
            long idle = now - e.usedNanos;
            if (idle < 250_000_000L || e.particles == null || e.particles.isEmpty()) continue;
            Entity live = mc.level == null ? null : mc.level.getEntity(me.getKey());
            boolean gone = live == null || live.isRemoved();
            if (gone || idle > 10_000_000_000L) {
                BmParticleWorld.adopt(e.particles, e.animator.def.pack);
                e.particles = null;
                if (gone) it.remove();
            }
        }
    }

    private static List<BmFrame> buildEquipment(
            Entity entity, LivingEntity living, Entry owner, RenderPlan plan, float yaw, double dt, float partialTick) {
        BmFrame.Owner ownerBones = owner.frame.owner();
        if (ownerBones == null) return List.of();
        GeometryDef ownerGeometry = ownerBones.geometry();
        Function<String, String> resolver = slot -> boneForSlot(ownerGeometry, String.valueOf(slot));
        List<BmFrame> out = new ArrayList<>();
        for (int i = 0; i < EQUIP_SLOTS.length; i++) {
            ItemStack stack = living.getItemBySlot(EQUIP_SLOTS[i]);
            if (stack.isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            ClientEntityDef def = attachable(itemId);
            if (def == null) continue;
            Entry ae = attachmentEntry(entity.getId() + "|" + EQUIP_NAMES[i] + "|" + itemId, def);
            if (ae.failed) continue;
            try {
                Env env = ae.animator.env;
                env.entity = owner.animator.env.entity;
                env.probe = owner.probe;
                env.context.put("item_slot", EQUIP_NAMES[i]);
                env.context.put("is_first_person", 0.0);
                linkOwner(env, owner);
                env.context.put("slot_bone", resolver);
                ae.animator.update(dt, entity.tickCount / 20.0, partialTick);
                RenderPlan attachmentPlan = RenderPlanner.plan(ae.animator);
                if (attachmentPlan.layers.isEmpty()) continue;
                attachmentPlan.scaleX = plan.scaleX;
                attachmentPlan.scaleY = plan.scaleY;
                attachmentPlan.scaleZ = plan.scaleZ;
                out.add(BmFrame.build(ae.animator, attachmentPlan, yaw, BmFrame.Context.NONE, null, List.of(), ownerBones));
                BedrockMotionClient.once("equip:" + itemId, "BedrockMotion: attaching {} to the Bedrock model of {}", itemId, owner.animator.def.identifier);
            } catch (RuntimeException ex) {
                ae.failed = true;
                BedrockMotionClient.LOGGER.error("BedrockMotion: equipment attachable {} disabled", itemId, ex);
            }
        }
        return out;
    }

    private static String boneForSlot(GeometryDef geometry, String slot) {
        String[] candidates = switch (slot) {
            case "main_hand" -> new String[] {"rightitem", "right_hand", "righthand", "rightarm"};
            case "off_hand" -> new String[] {"leftitem", "left_hand", "lefthand", "leftarm"};
            case "head" -> new String[] {"head"};
            case "torso" -> new String[] {"body"};
            case "legs" -> new String[] {"leftleg", "rightleg"};
            case "feet" -> new String[] {"leftleg", "rightleg"};
            default -> new String[0];
        };
        for (String c : candidates) if (geometry.indexOf(c) >= 0) return c;
        return "";
    }

    private static List<ParticleSystem.EmitterView> updateParticles(Entity entity, Entry e, RenderPlan plan, float yaw, double dt) {
        EntityAnimator an = e.animator;
        if (!features.particles) {
            an.pendingParticles.clear();
            return List.of();
        }
        if (e.particles == null) {
            if (an.pendingParticles.isEmpty()) return List.of();
            e.particles = new ParticleSystem(an.def.pack::particle, an.env);
        }
        double[] pos = {entity.getX(), entity.getY(), entity.getZ()};
        for (EntityAnimator.ParticleRequest r : an.pendingParticles) {
            double[] local = {0, 0, 0};
            if (r.locator() != null && !plan.layers.isEmpty()) {
                double[] solved = LocatorSolver.position(plan.layers.get(0).geometry, an.pose, r.locator());
                if (solved != null) local = solved;
            }
            e.particles.spawn(r.particleId(), local, r.preEffect(), pos, yaw);
        }
        an.pendingParticles.clear();
        e.particles.update(dt, pos, yaw);
        for (String event : e.particles.pendingSounds) BmSounds.play(entity, an.def.pack, event);
        e.particles.pendingSounds.clear();
        if (e.particles.isEmpty()) return List.of();
        List<ParticleSystem.EmitterView> views = new ArrayList<>();
        e.particles.collect(pos, yaw, views);
        return views;
    }

    private static void installBones(Entry e) {
        e.animator.env.bones = new BoneSolver() {
            @Override
            public Struct origin(String bone) {
                return e.lastGeometry == null
                        ? LocatorSolverEmpty.struct()
                        : dev.bedrockmotion.bedrock.LocatorSolver.originStruct(e.lastGeometry, e.animator.pose, bone);
            }

            @Override
            public Struct aabb(String bone) {
                return e.lastGeometry == null
                        ? new Struct()
                        : dev.bedrockmotion.bedrock.LocatorSolver.aabbStruct(e.lastGeometry, bone);
            }

            @Override
            public Struct trs(String bone) {
                if (e.lastGeometry == null) return LocatorSolverEmpty.struct();
                double[] pivot = dev.bedrockmotion.bedrock.LocatorSolver.bonePivot(e.lastGeometry, e.animator.pose, bone);
                return dev.bedrockmotion.bedrock.LocatorSolver.trsStruct(e.animator.pose, pivot, bone);
            }
        };
    }

    private static PoseHierarchy hierarchyOf(Entry entry, ClientEntityDef def) {
        if (!entry.hierarchyResolved) {
            entry.hierarchyResolved = true;
            String id = def.geometry.get("default");
            if (id == null && !def.geometry.isEmpty()) id = def.geometry.values().iterator().next();
            GeometryDef geometry = id == null ? null : def.pack.geometry(id.toLowerCase(Locale.ROOT));
            if (geometry != null) entry.hierarchy = HIERARCHIES.computeIfAbsent(geometry, PoseHierarchy::new);
        }
        return entry.hierarchy;
    }

    static void playSounds(Entity entity, EntityAnimator animator) {
        if (animator.pendingSounds.isEmpty()) return;
        for (String event : animator.pendingSounds) BmSounds.play(entity, animator.def.pack, event);
        animator.pendingSounds.clear();
    }

    private static ClientEntityDef lookup(Entity entity, Map<String, ClientEntityDef> defs) {
        EntityType<?> type = entity.getType();
        if (TYPE_CACHE.containsKey(type)) return TYPE_CACHE.get(type);
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().toLowerCase(Locale.ROOT);
        ClientEntityDef def = EntityIds.find(defs, key);
        TYPE_CACHE.put(type, def);
        return def;
    }

    static float fill(Entity entity, Entry entry, EntitySnapshot s, float pt, double dt) {
        Vec3 d = entity.getDeltaMovement();
        s.position[0] = entity.getX();
        s.position[1] = entity.getY();
        s.position[2] = entity.getZ();
        s.positionDelta[0] = d.x;
        s.positionDelta[1] = d.y;
        s.positionDelta[2] = d.z;
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        s.groundSpeed = horizontal * 20.0;
        s.verticalSpeed = d.y * 20.0;
        // These two queries are Java's limbSwingAmount / limbSwing, not blocks per second.
        Object walkState = BmWalk.state(entity);
        if (walkState != null) {
            s.modifiedMoveSpeed = BmReflect.numberFloat(walkState, "speed", pt);
            s.modifiedDistanceMoved = BmReflect.numberFloat(walkState, "position", pt);
        } else {
            double target = Math.min(horizontal * 4.0, 1.0);
            entry.walkSpeed += (target - entry.walkSpeed) * Math.min(1.0, 0.4 * dt * 20.0);
            entry.walked += entry.walkSpeed * dt * 20.0;
            s.modifiedMoveSpeed = entry.walkSpeed;
            s.modifiedDistanceMoved = entry.walked;
        }
        s.moving = horizontal > 1.0e-4;
        s.onGround = entity.onGround();
        s.inWater = entity.isInWater();
        s.inLava = entity.isInLava();
        s.sneaking = entity.isShiftKeyDown();
        s.sprinting = entity.isSprinting();
        s.swimming = entity.isSwimming();
        s.onFire = entity.isOnFire();
        s.riding = entity.isPassenger();
        s.hasRider = entity.isVehicle();
        s.name = entity.hasCustomName() ? entity.getName().getString() : "";
        Minecraft mc = Minecraft.getInstance();
        s.localPlayer = entity == mc.player;
        s.distanceFromCamera = mc.player == null ? 0 : entity.distanceTo(mc.player);
        s.targetXRotation = entity.getXRot();
        s.extra.clear();
        s.resetEngineVars();
        s.extra.put("is_invisible", entity.isInvisible() ? 1.0 : 0.0);
        s.extra.put("is_sheared", BmReflect.flag(entity, "isSheared"));
        s.extra.put("is_saddled", BmReflect.flag(entity, "isSaddled"));
        s.extra.put("is_powered", BmReflect.flag(entity, "isPowered"));
        s.extra.put("is_angry", BmReflect.flag(entity, "isAngry"));
        s.extra.put("is_charging", BmReflect.flag(entity, "isCharging"));
        s.extra.put("is_wall_climbing", BmReflect.flag(entity, "onClimbable"));
        s.extra.put("is_carrying_block", BmReflect.flag(entity, "getCarriedBlock"));
        fillMobStates(entity, s, pt);
        s.spectator = BmReflect.flag(entity, "isSpectator") > 0;
        s.headInWater = BmReflect.flag(entity, "isUnderWater") > 0;
        s.jumping = !s.onGround && d.y > 0.0;
        s.engineVars.put("player_x_rotation", s.localPlayer ? (double) entity.getXRot() : 0.0);
        float yaw = entity.getYRot();
        if (entity instanceof LivingEntity living) {
            float body = Mth.rotLerp(pt, living.yBodyRotO, living.yBodyRot);
            float head = Mth.rotLerp(pt, living.yHeadRotO, living.yHeadRot);
            yaw = body;
            s.bodyYRotation = body;
            s.headYRotation = head;
            s.targetYRotation = Mth.clamp(Mth.wrapDegrees(head - body), -85f, 85f);
            s.yawSpeed = Mth.wrapDegrees(living.yBodyRot - living.yBodyRotO) * 20.0;
            s.health = living.getHealth();
            s.maxHealth = living.getMaxHealth();
            s.baby = living.isBaby();
            double size = BmReflect.number(living, "getScale");
            double ageScale = BmReflect.number(living, "getAgeScale");
            s.modelScale = MODEL_UNIT * (size > 0 ? size : 1.0) * (ageScale > 0 ? ageScale : s.baby ? 0.5 : 1.0);
            s.sleeping = living.isSleeping();
            s.gliding = living.isFallFlying();
            s.alive = living.isAlive();
            s.deathTicks = living.deathTime;
            s.usingItem = living.isUsingItem();
            s.extra.put("hurt_time", (double) living.hurtTime);
            s.extra.put("has_head_gear", living.getItemBySlot(EquipmentSlot.HEAD).isEmpty() ? 0.0 : 1.0);
            fillLivingStates(living, s, pt, d.x * d.x + d.y * d.y + d.z * d.z);
        } else {
            s.bodyYRotation = yaw;
            s.headYRotation = yaw;
        }
        fillSpecialStates(entity, entry, s, pt);
        BmMobStates.fill(entity, s, pt);
        return yaw;
    }

    private static void fillLivingStates(LivingEntity living, EntitySnapshot s, float pt, double speedSquared) {
        s.engineVars.put("attack_time", BmReflect.numberFloat(living, "getAttackAnim", pt));
        double swim = BmReflect.numberFloat(living, "getSwimAmount", pt);
        s.engineVars.put("swim_amount", swim);
        if (s.gliding) {
            double f = speedSquared / 0.2;
            f = f * f * f;
            s.engineVars.put("gliding_speed_value", f < 1.0 ? 1.0 : f);
        }
        s.blocking = BmReflect.flag(living, "isBlocking") > 0;
        s.extra.put("is_crawling", BmReflect.flag(living, "isVisuallyCrawling"));
        s.mainHandItemUseDuration = 0;
        s.mainHandItemMaxDuration = 0;
        s.itemRemainingUseDuration = 0;
        s.itemMaxUseDuration = 0;
        if (s.usingItem) {
            double remaining = BmReflect.number(living, "getUseItemRemainingTicks");
            double used = BmReflect.number(living, "getTicksUsingItem");
            double max = remaining + used;
            s.itemRemainingUseDuration = remaining / 20.0;
            s.itemMaxUseDuration = max / 20.0;
            Object hand = BmReflect.call(living, "getUsedItemHand");
            if (hand == null || String.valueOf(hand).contains("MAIN")) {
                s.mainHandItemUseDuration = remaining;
                s.mainHandItemMaxDuration = max;
            }
        }
    }

    private static void fillMobStates(Entity entity, EntitySnapshot s, float pt) {
        s.hasTarget = BmReflect.flag(entity, "isAggressive") > 0;
        s.sitting = BmReflect.firstFlag(entity, "isInSittingPose", "isSitting") > 0;
        s.tamed = BmReflect.firstFlag(entity, "isTame", "isTamed") > 0;
        s.extra.put("is_leashed", BmReflect.flag(entity, "isLeashed"));
        s.extra.put("is_resting", BmReflect.flag(entity, "isResting"));
        s.extra.put("is_standing", BmReflect.flag(entity, "isStanding"));
        s.extra.put("is_dancing", BmReflect.flag(entity, "isDancing"));
        s.extra.put("is_celebrating", BmReflect.firstFlag(entity, "isCelebrating", "isDancing"));
        s.extra.put("is_interested", BmReflect.flag(entity, "isInterested"));
        s.extra.put("is_stalking", BmReflect.flag(entity, "isCrouching"));
        s.extra.put("is_digging", BmReflect.flag(entity, "isDigging"));
        s.extra.put("is_emerging", BmReflect.flag(entity, "isEmerging"));
        s.extra.put("is_searching", BmReflect.flag(entity, "isSearching"));
        s.extra.put("is_croaking", BmReflect.flag(entity, "isCroaking"));
        s.extra.put("is_charged", BmReflect.flag(entity, "isCharged"));
        s.extra.put("is_shaking_wetness", BmReflect.flag(entity, "isShaking"));
        s.extra.put("invulnerable_ticks", BmReflect.number(entity, "getInvulnerableTicks"));
        s.swellAmount = BmReflect.numberFloat(entity, "getSwelling", pt);
    }

    private static void fillSpecialStates(Entity entity, Entry entry, EntitySnapshot s, float pt) {
        Object skin = BmReflect.call(entity, "getSkin");
        s.extra.put("has_cape", skin != null && BmReflect.call(skin, "cape") != null ? 1.0 : 0.0);
        s.extra.put("cape_flap_amount", capeFlap(s));
        s.extra.put("has_dash_cooldown", BmReflect.number(entity, "getJumpCooldown") > 0 ? 1.0 : BmReflect.flag(entity, "isDashing"));
        s.extra.put("is_delayed_attacking", Math.max(BmReflect.flag(entity, "isDelayedAttacking"), s.engineVars.getOrDefault("attack_time", 0.0) > 0 ? 1.0 : 0.0));
        boolean elder = BmReflect.flag(entity, "isElder") > 0 || entity.getClass().getSimpleName().equals("ElderGuardian");
        s.extra.put("is_elder", elder ? 1.0 : 0.0);
        s.extra.put("is_levitating", hasEffect(entity, "levitation"));
        boolean sniffing = entity.getClass().getSimpleName().equals("Sniffer") && String.valueOf(BmReflect.call(entity, "getState")).contains("SNIFFING");
        s.extra.put("is_sniffing", sniffing ? 1.0 : 0.0);
        s.extra.put("sneeze_counter", BmReflect.number(entity, "getSneezeCounter"));
        s.extra.put("unhappy_counter", BmReflect.number(entity, "getUnhappyCounter"));
        double[] kinetic = BmKinetic.read(entity);
        s.extra.put("kinetic_weapon_delay", kinetic == null ? 0.0 : kinetic[0]);
        s.extra.put("kinetic_weapon_dismount_duration", kinetic == null ? 0.0 : kinetic[1]);
        s.extra.put("kinetic_weapon_knockback_duration", kinetic == null ? 0.0 : kinetic[2]);
        s.extra.put("kinetic_weapon_damage_duration", kinetic == null ? 0.0 : kinetic[3]);
        double attack = s.engineVars.getOrDefault("attack_time", 0.0);
        double now = entity.tickCount + pt;
        if (kinetic != null && attack > 0 && entry.lastAttack <= 0) entry.kineticHitAt = now;
        entry.lastAttack = attack;
        s.extra.put("ticks_since_last_kinetic_weapon_hit", Double.isNaN(entry.kineticHitAt) ? -1.0 : now - entry.kineticHitAt);
        spearStates(entity, s, kinetic);
    }

    private static void spearStates(Entity entity, EntitySnapshot s, double[] kinetic) {
        boolean using = kinetic != null && BmReflect.flag(entity, "isUsingItem") > 0;
        double ticks = using ? BmReflect.number(entity, "getTicksUsingItem") : 0.0;
        double delay = kinetic == null ? 0.0 : kinetic[0];
        boolean brandishing = kinetic != null && (using || BmReflect.flag(entity, "isAggressive") > 0);
        s.engineVars.put("is_brandishing_spear", brandishing ? 1.0 : 0.0);
        s.engineVars.put("use_item_startup_progress", using && delay > 0 && ticks < delay ? (ticks + 1.0) / delay : 0.0);
        s.engineVars.put("use_item_interval_progress", using && ticks >= delay ? 1.0 : 0.0);
        s.engineVars.put("damage_nearby_mobs", using && ticks >= delay && ticks <= delay + kinetic[3] ? 1.0 : 0.0);
    }

    private static double capeFlap(EntitySnapshot s) {
        double yaw = Math.toRadians(s.bodyYRotation);
        double forward = -s.positionDelta[0] * Math.sin(yaw) + s.positionDelta[2] * Math.cos(yaw);
        double lift = Math.max(0.0, Math.min(150.0, forward * 300.0));
        double vertical = Math.max(-6.0, Math.min(32.0, -s.positionDelta[1] * 30.0));
        double horizontal = Math.sqrt(s.positionDelta[0] * s.positionDelta[0] + s.positionDelta[2] * s.positionDelta[2]);
        double bob = Math.sin(s.modifiedDistanceMoved * 0.9) * 32.0 * Math.min(0.1, horizontal);
        return Math.max(0.0, Math.min(1.0, (lift / 2.0 + vertical + bob) / 126.0));
    }

    private static double hasEffect(Entity entity, String name) {
        if (!(BmReflect.call(entity, "getActiveEffects") instanceof Collection<?> effects) || effects.isEmpty()) return 0.0;
        for (Object instance : effects) {
            Object effect = BmReflect.call(instance, "getEffect");
            if (effect != null && effect.toString().contains(name)) return 1.0;
        }
        return 0.0;
    }

    private static void cleanup(long now) {
        if (now - lastCleanup < 5_000_000_000L) return;
        lastCleanup = now;
        removeStale(ENTRIES.values().iterator(), now);
        removeStale(ATTACHMENTS.values().iterator(), now);
        if (!warnedNoHook && !entityDefs.isEmpty() && replacedCalls == 0 && extractCalls > 2000) {
            warnedNoHook = true;
            BedrockMotionClient.LOGGER.warn(
                    "BedrockMotion: packs are loaded but no entity has been replaced yet. If matching entities are nearby, "
                            + "check the EntityRenderDispatcher.submit mixin in the log.");
        }
    }

    private static void removeStale(Iterator<Entry> it, long now) {
        while (it.hasNext()) {
            if (now - it.next().usedNanos > 10_000_000_000L) it.remove();
        }
    }

    public static boolean hasPacks() {
        return !packs.isEmpty();
    }
}
