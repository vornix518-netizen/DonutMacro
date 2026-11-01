package com.donutmacro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DonutMacroClient implements ClientModInitializer {
    public static KeyBinding openKey;
    private static boolean swapKeyChecked = false;

    @Override
    public void onInitializeClient() {
        MacroConfig.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutmacro.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "category.donutmacro"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            freeSwapHandsKey(mc);
            while (openKey.wasPressed()) {
                if (mc.currentScreen == null && mc.player != null) mc.setScreen(new MacroScreen());
            }
            MacroRunner.tick(mc);
        });
    }

    /**
     * Minecraft's "swap item with offhand" is also F by default. When two actions share a key only one
     * of them fires, so unbind swap-hands once if it collides with our key.
     */
    private static void freeSwapHandsKey(MinecraftClient mc) {
        if (swapKeyChecked || mc.options == null) return;
        swapKeyChecked = true;
        KeyBinding swap = mc.options.swapHandsKey;
        if (swap.getBoundKeyTranslationKey().equals(openKey.getBoundKeyTranslationKey())) {
            swap.setBoundKey(InputUtil.UNKNOWN_KEY);
            KeyBinding.updateKeysByCode();
            mc.options.write();
        }
    }
}
