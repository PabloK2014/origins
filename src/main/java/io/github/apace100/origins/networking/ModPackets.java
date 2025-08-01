package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import net.minecraft.util.Identifier;

public class ModPackets {

    public static final Identifier HANDSHAKE = Origins.identifier("handshake");

    public static final Identifier OPEN_ORIGIN_SCREEN = new Identifier(Origins.MODID, "open_origin_screen");
    public static final Identifier CHOOSE_ORIGIN = new Identifier(Origins.MODID, "choose_origin");
    public static final Identifier USE_ACTIVE_POWERS = new Identifier(Origins.MODID, "use_active_powers");
    public static final Identifier ORIGIN_LIST = new Identifier(Origins.MODID, "origin_list");
    public static final Identifier LAYER_LIST = new Identifier(Origins.MODID, "layer_list");
    public static final Identifier POWER_LIST = new Identifier(Origins.MODID, "power_list");
    public static final Identifier CHOOSE_RANDOM_ORIGIN = new Identifier(Origins.MODID, "choose_random_origin");
    public static final Identifier CONFIRM_ORIGIN = Origins.identifier("confirm_origin");
    public static final Identifier PLAYER_LANDED = Origins.identifier("player_landed");
    public static final Identifier BADGE_LIST = Origins.identifier("badge_list");
    public static final Identifier ACTIVE_COOK_SKILL = new Identifier(Origins.MODID, "active_cook_skill");
    public static final Identifier SYNC_SKILLS = new Identifier(Origins.MODID, "sync_skills");
    public static final Identifier LEARN_TREE_SKILL = new Identifier(Origins.MODID, "learn_tree_skill");
    public static final Identifier ACTIVATE_GLOBAL_SKILL = new Identifier(Origins.MODID, "activate_global_skill");
    public static final Identifier ACTIVATE_ACTIVE_SKILL = new Identifier(Origins.MODID, "activate_active_skill");
}
