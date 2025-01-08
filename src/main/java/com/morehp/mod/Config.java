package com.morehp.mod;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = MoreHpMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    // Start builder and get logger
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final Logger LOGGER = LoggerFactory.getLogger("MoreHP-Mobs");

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ //

    // Config value for health multiplier
    public static final ForgeConfigSpec.DoubleValue healthMultiplier = BUILDER
            .comment("Health multiplier for all mobs (recommended: 5.0)")
            .defineInRange("healthMultiplier", 1.0, 1.0, 100.0);

    // Config values for mobs health
    public static Map<String, ForgeConfigSpec.ConfigValue<Double>> mobHealths = new HashMap<>();

    public static void updateMobHealth(String mobId, double health) {
        // Updates or adds a new mob health value
        try {
            if (mobHealths.containsKey(mobId)) {
                mobHealths.get(mobId).set(health);
            } else {
                // Add a new mob health value
                mobHealths.put(mobId, BUILDER.define(mobId, health));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update mob health value: {}", e.getMessage());
        }
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ //

    // Finish building the config
    static final ForgeConfigSpec SPEC = BUILDER.build();

}