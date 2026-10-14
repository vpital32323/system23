package com.iwoho.visuals;

import com.iwoho.visuals.config.Config;
import com.iwoho.visuals.gui.ClickGuiScreen;
import com.iwoho.visuals.hud.Hud;
import com.iwoho.visuals.render.NametagRenderer;
import com.iwoho.visuals.render.TargetEsp;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class IwohoVisuals implements ClientModInitializer {
    public static KeyBinding OPEN_MENU;

    @Override
    public void onInitializeClient() {
        Config.load();

        OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Открыть меню Iwoho Visuals", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Iwoho Visuals"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_MENU.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
            }
        });

        HudRenderCallback.EVENT.register(Hud::render);
        WorldRenderEvents.AFTER_ENTITIES.register(NametagRenderer::render);
        WorldRenderEvents.LAST.register(TargetEsp::render);
    }
}
