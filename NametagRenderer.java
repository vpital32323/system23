package com.iwoho.visuals.render;

import com.iwoho.visuals.config.Config;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Draws "Name 20HP" plus armor/hand item icons above every player's head. */
public final class NametagRenderer {
    private static final int FULL_BRIGHT = 0xF000F0;

    private NametagRenderer() {}

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config c = Config.get();
        if (!c.on("nametags", true) || mc.world == null || mc.player == null) return;
        VertexConsumerProvider vc = ctx.consumers();
        if (vc == null) return;

        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        MatrixStack ms = ctx.matrixStack();
        float td = ctx.tickDelta();
        TextRenderer tr = mc.textRenderer;
        boolean showHp = c.on("tag_hp", true);
        boolean showArmor = c.on("tag_armor", true);
        boolean showHand = c.on("tag_hand", true);

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player && !cam.isThirdPerson()) continue;
            if (p.isSpectator()) continue;

            double x = MathHelper.lerp(td, p.prevX, p.getX()) - cp.x;
            double y = MathHelper.lerp(td, p.prevY, p.getY()) - cp.y;
            double z = MathHelper.lerp(td, p.prevZ, p.getZ()) - cp.z;
            double dist = Math.sqrt(x * x + y * y + z * z);
            if (dist > 96) continue;

            y += p.getHeight() + (p.isSneaking() ? 0.35 : 0.6);
            float s = 0.022f * (float) Math.max(1.0, dist / 9.0);

            ms.push();
            ms.translate(x, y, z);
            ms.multiply(cam.getRotation());
            ms.scale(-s, -s, s);
            Matrix4f m = ms.peek().getPositionMatrix();

            MutableText t = Text.empty().append(p.getDisplayName());
            if (showHp) {
                float hp = p.getHealth() + p.getAbsorptionAmount();
                float ratio = MathHelper.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0f, 1f);
                int col = ratio > 0.66f ? 0x55FF55 : ratio > 0.33f ? 0xFFFF55 : 0xFF5555;
                t.append(Text.literal(" " + (int) Math.ceil(hp) + "HP").styled(st -> st.withColor(col)));
            }

            float hx = -tr.getWidth(t) / 2f;
            tr.draw(t, hx, 0, 0x20FFFFFF, false, m, vc, TextRenderer.TextLayerType.SEE_THROUGH, 0x66000000, FULL_BRIGHT);
            tr.draw(t, hx, 0, 0xFFFFFFFF, false, m, vc, TextRenderer.TextLayerType.NORMAL, 0, FULL_BRIGHT);

            List<ItemStack> items = new ArrayList<>(5);
            if (showHand && !p.getMainHandStack().isEmpty()) items.add(p.getMainHandStack());
            if (showArmor) {
                for (int i = 3; i >= 0; i--) {
                    ItemStack a = p.getInventory().armor.get(i);
                    if (!a.isEmpty()) items.add(a);
                }
            }
            float ix = -items.size() * 18 / 2f;
            for (ItemStack st : items) {
                ms.push();
                ms.translate(ix + 8, 20, 0);
                ms.scale(16f, -16f, 16f);
                mc.getItemRenderer().renderItem(st, ModelTransformationMode.GUI, FULL_BRIGHT,
                        OverlayTexture.DEFAULT_UV, ms, vc, mc.world, 0);
                ms.pop();
                ix += 18;
            }
            ms.pop();
        }
    }
}
