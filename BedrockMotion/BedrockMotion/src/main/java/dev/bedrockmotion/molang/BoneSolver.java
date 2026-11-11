package dev.bedrockmotion.molang;

public interface BoneSolver {

    Struct origin(String bone);

    Struct trs(String bone);

    Struct aabb(String bone);
}
