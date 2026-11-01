package com.donutmacro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;

public class MacroConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("donutmacro.json");

    public static MacroConfig INSTANCE = new MacroConfig();

    /** Option 1: item to sell, e.g. minecraft:diamond */
    public String item = "minecraft:diamond";
    /** Option 2: auction house price (Donut accepts 1000, 1k, 2.5m ...) */
    public String price = "1000";
    /** Option 3: fill orders on/off */
    public boolean ordersEnabled = false;
    /** Option 3: max items to move into orders per run */
    public int maxOrderAmount = 64;
    /** Pick up matching items lying on the ground */
    public boolean pickupGround = true;
    /** Ticks to wait between actions (20 ticks = 1 second) */
    public int delayTicks = 20;

    public Item targetItem() {
        Identifier id = Identifier.tryParse(item.trim().contains(":") ? item.trim() : "minecraft:" + item.trim());
        if (id == null) return null;
        return Registries.ITEM.getOptionalValue(id).orElse(null);
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                MacroConfig c = GSON.fromJson(Files.readString(FILE), MacroConfig.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception ignored) {}
    }
}
