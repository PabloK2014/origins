package net.xach.testmod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static final Map<String, List<EffectConfig>> classEffects;

    public static class EffectConfig {
        public MobEffect effect;
        public int level;
        public int duration;

        public EffectConfig(MobEffect effect, int level, int duration) {
            this.effect = effect;
            this.level = level;
            this.duration = duration;
        }
    }

    static {
        classEffects = new HashMap<>();
        // Воин: Сила I
        classEffects.put("war", List.of(
                new EffectConfig(MobEffects.DAMAGE_BOOST, 1, 200)
        ));
        // Повар: Замедление I, Насыщение I
        classEffects.put("cook", List.of(
                new EffectConfig(MobEffects.SATURATION, 1, 200)
        ));
        // Курьер Yandex.Go: Скорость I
        classEffects.put("yandex.go", List.of(

        ));
        // Пивовар: Прыгучесть I
        classEffects.put("pivo", List.of(
                new EffectConfig(MobEffects.JUMP, 1, 200)
        ));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
    }
}