package com.morehp.mod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;

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
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            double maxHealth = mob.getMaxHealth();
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
               .setBaseValue(maxHealth * Config.healthMultiplier.get());
            mob.setHealth(mob.getMaxHealth());
        }
    }
}