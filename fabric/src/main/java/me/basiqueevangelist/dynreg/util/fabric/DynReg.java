package me.basiqueevangelist.dynreg.util.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

public class DynReg implements ModInitializer {
    public static final String MODID = "dynreg";
    public static MinecraftServer SERVER;

    public static Identifier id(String path) {
        return Identifier.of(MODID, path);
    }

    public void onInitialize() {
        LoggerFactory.getLogger("DynReg").info("I have become DynReg, destroyer of immutability");
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SERVER = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SERVER = null);
    }
}
