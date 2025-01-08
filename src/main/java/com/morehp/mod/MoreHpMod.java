package com.morehp.mod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.server.ServerStartingEvent;

import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attributes;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;

@Mod(MoreHpMod.MODID)
public class MoreHpMod {
    public static final String MODID = "morehp";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MoreHpMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Verify that the mod is on server only
        if (event.getServer().isDedicatedServer()) {
            ServerLevel level = event.getServer().overworld();
            for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
                try {
                    if (entityType.create(level) instanceof Mob mob) {
                        String key = mob.getType().getDescriptionId().replace("entity.minecraft.", "");
                        double health = mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                        // Config.updateMobHealth(key, health);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to process entity: {} - Error: {}", 
                        entityType.getDescriptionId(), 
                        e.getMessage());
                    if (LOGGER.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
            LOGGER.info("Finished loading mob health values. Total entries: {}", Config.mobHealths.size());
        } 
    }

}