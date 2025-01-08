package com.morehp.mod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.server.ServerStartingEvent;

import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.Reader;
import java.io.Writer;

import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

@Mod(MoreHpMod.MODID)
public class MoreHpMod {

    public static final String MODID = "morehp";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int UPDATE_INTERVAL = 6000; // 5 minutes (20 ticks per second * 60 seconds * 5 minutes)
    private int tickCounter = 0;
    
    public MoreHpMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Config Data Json ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ //

    // Config data structure
    private static class ConfigData {
        // ModHealth object has as keys the mods, each mod has as value another object of mobs, with the key as the parameter here called "key" and as value their health
        // MobHealth: { "alex_mobs": {"snake":20.0} }

        @SerializedName("READ ME")
        public String readMe = "all mobs hp will go from 1.0 to 1000.0, as 1024.0 is the mojang hp limit. Hp multiplier is optional and has a value from 1.0 to 100.0, leave on 1.0 by default and I recommend a multiplier of 5. I RECOMMEND TO NOT CHANGE THE ORIGINAL HP, only the 'hp to use' value.";

        private static class MobHealthData {
            @SerializedName("original")
            public double originalHealth;
            @SerializedName("hp to use")
            public double targetHealth;

            public MobHealthData(double health) {
                this.originalHealth = health;
                this.targetHealth = health;
            }
        }

        public double healthMultiplier = 1.0;
        public HashMap<String, HashMap<String, MobHealthData>> mobsHealth = new HashMap<>();

        // Helper method to add/update mob health
        public void setMobHealth(String modId, String mobId, double health) {
            // Only add if the mob doesn't exist
            mobsHealth.computeIfAbsent(modId, k -> new HashMap<>())
                    .putIfAbsent(mobId, new MobHealthData(health));
        }

        // Get mob health
        public Double getMobHealth(String modId, String mobId) {
            var mobData = mobsHealth.getOrDefault(modId, new HashMap<>()).get(mobId);
            return mobData != null ? mobData.targetHealth : null;
        }

        public Double getOriginalMobHealth(String modId, String mobId) {
            var mobData = mobsHealth.getOrDefault(modId, new HashMap<>()).get(mobId);
            return mobData != null ? mobData.originalHealth : null;
        }
    }

    private ConfigData configData;
    private Path configFile;
    private Gson gson;

    private void setupJson() {
        // Creates a JSON file inside config folder if not present

        // Get config path
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path configFile = configDir.resolve("morehp-config.json");

        String jsonString = """
        {
            "READ ME": "all mobs hp will go from 1.0 to 1000.0, as 1024.0 is the mojang hp limit. Hp multiplier is optional and has a value from 1.0 to 100.0, leave on 1.0 by default and I recommend a multiplier of 5.",
            "Health Multiplier": 1.0,
            "Mobs health": {}
        }
        """;

        try {
            // Create config directory if it doesn't exist
            Files.createDirectories(configDir);
            
            // Write JSON file if it doesn't exist
            if (!Files.exists(configFile)) {
                Files.writeString(configFile, jsonString);
                LOGGER.info("Created default config file at: " + configFile);
            } else {
                LOGGER.info("setupJson(): Config file already exists at: " + configFile);
            }
        } catch (IOException e) {
            LOGGER.error("Could not create config file: " + e.getMessage());
        }
        
    }

    private void loadConfig() {
        try {
            if (Files.exists(configFile)) {
                Reader reader = Files.newBufferedReader(configFile);
                configData = gson.fromJson(reader, ConfigData.class);
                reader.close();
            } else {
                configData = new ConfigData();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config: " + e.getMessage());
            configData = new ConfigData();
        }
    }

    private void saveConfig() {
        try {
            Writer writer = Files.newBufferedWriter(configFile);
            gson.toJson(configData, writer);
            writer.close();
            LOGGER.info("Saved config to: " + configFile);
        } catch (IOException e) {
            LOGGER.error("Failed to save config: " + e.getMessage());
        }
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Events ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ //

    @SubscribeEvent
    public void onServerStart(ServerStartingEvent event) {
        // Verify that the mod is on server only
        if (event.getServer().isDedicatedServer()) {

            // Set config file
            LOGGER.info("Setting up config file json");
            configFile = FMLPaths.CONFIGDIR.get().resolve("morehp-config.json");
            gson = new GsonBuilder().setPrettyPrinting().create();
            setupJson();
            loadConfig();

            // Set mobs health
            LOGGER.info("Loading mob health values :D");
            ServerLevel level = event.getServer().overworld();
            for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
                try {
                    if (entityType.create(level) instanceof Mob mob) {
                        String key = mob.getType().getDescriptionId().replace("entity.", "");
                        double health = mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                        String mod = key.split("\\.")[0];
                        String mobId = key.split("\\.")[1];
                        configData.setMobHealth(mod, mobId, health);
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
            saveConfig();
            int totalEntities = 0;
            for (String modId : configData.mobsHealth.keySet()) {
                totalEntities += configData.mobsHealth.get(modId).size();
            }
            LOGGER.info("Total entities saved: " + totalEntities);
            LOGGER.info("Updating existing mobs health");
            updateExistingMobs(level);
        } else {
            LOGGER.error("This mod is server-side only and should not be loaded on the client. Nothing will be done.");
        }
    }

    // Event function to modify the hp of ALL mobs in the world
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            String key = mob.getType().getDescriptionId().replace("entity.", "");
            String modId = key.split("\\.")[0];
            String mobId = key.split("\\.")[1];
            
            // Get configured health for this mob
            Double configuredHealth = configData.getMobHealth(modId, mobId);
            if (configuredHealth != null) {
                // Get the health attribute
                AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null) {
                    // Apply health multiplier
                    double finalHealth = configuredHealth * configData.healthMultiplier;
                    // Set base value (permanent change)
                    healthAttr.setBaseValue(finalHealth);
                    // Set current health to max
                    mob.setHealth((float)finalHealth);
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= UPDATE_INTERVAL) {
                tickCounter = 0;
                LOGGER.info("Periodic mob health: update on-going, happens every 5 minutes");
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    for (ServerLevel level : server.getAllLevels()) {
                        updateExistingMobs(level);
                    }
                }
            }
        }
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Helpers ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ //

    // For existing mobs, add this method:
    public void updateExistingMobs(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob mob) {
                String key = mob.getType().getDescriptionId().replace("entity.", "");
                String modId = key.split("\\.")[0];
                String mobId = key.split("\\.")[1];
                
                Double configuredHealth = configData.getMobHealth(modId, mobId);
                if (configuredHealth != null) {
                    AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
                    if (healthAttr != null) {
                        double targetHealth = configuredHealth * configData.healthMultiplier;
                        if (healthAttr.getBaseValue() != targetHealth) {
                            healthAttr.setBaseValue(targetHealth);
                            mob.setHealth((float)targetHealth);
                            LOGGER.debug("Updated {} health from {} to {}", 
                                key, healthAttr.getBaseValue(), targetHealth);
                        }
                    }
                }
            }
        }
    }

}