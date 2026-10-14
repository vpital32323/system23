package com.iwoho.visuals.render;

import com.iwoho.visuals.config.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Animated ring that rises and falls around the entity under the crosshair. */
public final class TargetEsp {
    private static Entity target;
    private static long lastSeen;
    private static float fade;

    private TargetEsp() {}

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config c = Config.get();
        if (!c.on("target_esp", true) || mc.world == null || mc.player == null) return;

        long now = Util.getMeasuringTimeMs();
        HitResult hr = mc.crosshairTarget;
        if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le && le != mc.player) {
            target = le;
            lastSeen = now;
        }
        boolean active = target != null && target.isAlive() && now - lastSeen < 1200;
        fade += ((active ? 1f : 0f) - fade) * 0.15f;
        if (fade < 0.02f || target == null) return;

        float td = ctx.tickDelta();
        Vec3d cp = ctx.camera().getPos();
        double x = MathHelper.lerp(td, target.prevX, target.getX()) - cp.x;
        double y = MathHelper.lerp(td, target.prevY, target.getY()) - cp.y;
        double z = MathHelper.lerp(td, target.prevZ, target.getZ()) - cp.z;

        double time = now / 1000.0;
        float h = target.getHeight();
        float yy = (float) (y + (Math.sin(time * 2.2) * 0.5 + 0.5) * h);
        float radius = target.getWidth() * 0.9f + 0.2f;

        int col = c.accentColor();
        float r = ((col >> 16) & 255) / 255f, g = ((col >> 8) & 255) / 255f, b = (col & 255) / 255f;

        MatrixStack ms = ctx.matrixStack();
        ms.push();
        Matrix4f m = ms.peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();
        bb.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int seg = 40;
        float inner = radius - 0.07f;
        float a = 0.85f * fade;
        for (int i = 0; i <= seg; i++) {
            double ang = i * (Math.PI * 2 / seg) + time;
            float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            bb.vertex(m, (float) x + cs * radius, yy, (float) z + sn * radius).color(r, g, b, a).next();
            bb.vertex(m, (float) x + cs * inner, yy, (float) z + sn * inner).color(r, g, b, a * 0.25f).next();
        }
        t.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }
}
