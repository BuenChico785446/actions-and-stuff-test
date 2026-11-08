package dev.bedrockmotion.molang;

import java.util.HashMap;
import java.util.Map;

public final class EntitySnapshot {

    public final double[] position = new double[3];
    public final double[] positionDelta = new double[3];
    public double groundSpeed;
    public double verticalSpeed;
    public double modifiedMoveSpeed;
    public double modifiedDistanceMoved;
    public double yawSpeed;
    public double bodyYRotation;
    public double headYRotation;
    public double targetXRotation;
    public double targetYRotation;
    public double rideBodyYRotation;
    public double cameraRotationX;
    public double cameraRotationY;
    public double rotationToCameraX;
    public double rotationToCameraY;
    public double distanceFromCamera;
    public double health = 20;
    public double maxHealth = 20;
    public double deathTicks;
    public double hurtDirection;
    public double swellAmount;
    public double mainHandItemUseDuration;
    public double mainHandItemMaxDuration;
    public double itemRemainingUseDuration;
    public double itemMaxUseDuration;
    public double cooldownTimeRemaining;
    public double modelScale = 1;
    public double variant;
    public double markVariant;
    public double skinId;
    public double timeOfDay;
    public boolean alive = true;
    public boolean onGround = true;
    public boolean inWater;
    public boolean headInWater;
    public boolean inLava;
    public boolean riding;
    public boolean hasRider;
    public boolean sneaking;
    public boolean sprinting;
    public boolean swimming;
    public boolean gliding;
    public boolean jumping;
    public boolean sleeping;
    public boolean sitting;
    public boolean baby;
    public boolean tamed;
    public boolean spectator;
    public boolean localPlayer;
    public boolean inUi;
    public boolean onFire;
    public boolean hasTarget;
    public boolean blocking;
    public boolean emoting;
    public boolean shaking;
    public boolean attached;
    public boolean charged;
    public boolean avoidingMobs;
    public boolean roaring;
    public boolean sonicBoom;
    public boolean moving;
    public boolean usingItem;
    public String name = "";
    public String identifier = "";
    public String itemName = "";
    public final Map<String, Double> properties = new HashMap<>();
    public final Map<String, Double> extra = new HashMap<>();

    public final Map<String, Double> engineVars = new HashMap<>();

    public EntitySnapshot() {
        resetEngineVars();
    }

    public void resetEngineVars() {
        engineVars.clear();
        engineVars.put("attack_time", 0.0);
        engineVars.put("swim_amount", 0.0);
        engineVars.put("gliding_speed_value", 1.0);
        engineVars.put("is_first_person", 0.0);
        engineVars.put("is_paperdoll", 0.0);
        engineVars.put("map_face_icon", 0.0);
        engineVars.put("player_x_rotation", 0.0);
    }
}
