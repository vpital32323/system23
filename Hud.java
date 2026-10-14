package com.iwoho.visuals.hud;

import com.iwoho.visuals.config.Config;
import com.iwoho.visuals.gui.ClickGuiScreen;
import com.iwoho.visuals.gui.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lightweight HUD. Strings and widths are cached and refreshed 4x/sec,
 * every element is drawn with a handful of fills only.
 */
public final class Hud {
    public static final class El {
        public final String id, name;
        public final int dx, dy;
        public final boolean def;
        public String[] lines = {""};
        public int[] lw = {0};
        public int x, y, w, h;

        El(String id, String name, int dx, int dy, boolean def) {
            this.id = id; this.name = name; this.dx = dx; this.dy = dy; this.def = def;
        }
    }

    public static final List<El> ELEMENTS = List.of(
            new El("hud_watermark", "Ватермарк", 6, 6, true),
            new El("hud_fps", "FPS", 6, 26, true),
            new El("hud_ping", "Пинг", 6, 46, true),
            new El("hud_coords", "Координаты", 6, 66, true),
            new El("hud_time", "Время", 6, 86, false),
            new El("hud_potions", "Зелья", 6, 110, true),
            new El("hud_armor", "Броня", 6, 190, true)
    );

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static long lastRefresh = 0;
    private static int frames = 0, fps = 0;
    private static long fpsStart = 0;

    private Hud() {}

    public static void render(DrawContext g, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        Config cfg = Config.get();
        TextRenderer tr = mc.textRenderer;
        long now = Util.getMeasuringTimeMs();

        frames++;
        if (now - fpsStart >= 500) {
            fps = (int) (frames * 1000L / Math.max(1, now - fpsStart));
            frames = 0;
            fpsStart = now;
        }
        boolean editing = mc.currentScreen instanceof ClickGuiScreen && ClickGuiScreen.editHud;
        if (now - lastRefresh >= 250) {
            refresh(mc, tr, editing);
            lastRefresh = now;
        }

        int accent = cfg.accentColor();
        MatrixStack ms = g.getMatrices();
        ms.push();
        ms.scale(cfg.hudScale, cfg.hudScale, 1f);

        for (El e : ELEMENTS) {
            if (!cfg.on(e.id, e.def)) continue;
            int[] p = cfg.pos(e.id, e.dx, e.dy);
            e.x = p[0];
            e.y = p[1];

            if (e.id.equals("hud_armor")) {
                drawArmor(g, tr, mc, e, accent, editing);
            } else {
                if (e.id.equals("hud_fps")) { e.lines[0] = fps + " FPS"; e.lw[0] = tr.getWidth(e.lines[0]); }
                if (e.lines.length == 0 || (e.lines.length == 1 && e.lines[0].isEmpty())) {
                    if (!editing) continue;
                }
                drawLines(g, tr, e, accent);
            }
        }
        ms.pop();
    }

    private static void drawLines(DrawContext g, TextRenderer tr, El e, int accent) {
        int maxW = 0;
        for (int w : e.lw) maxW = Math.max(maxW, w);
        e.w = maxW + 18;
        e.h = e.lines.length * 11 + 5;
        Draw.rrect(g, e.x, e.y, e.x + e.w, e.y + e.h, 4, 0xAA0C0C10);
        g.fill(e.x + 4, e.y + 4, e.x + 6, e.y + e.h - 4, accent);
        for (int i = 0; i < e.lines.length; i++) {
            g.drawText(tr, e.lines[i], e.x + 11, e.y + 4 + i * 11, 0xFFFFFFFF, false);
        }
    }

    private static void drawArmor(DrawContext g, TextRenderer tr, MinecraftClient mc, El e, int accent, boolean editing) {
        boolean any = false;
        for (ItemStack s : mc.player.getInventory().armor) if (!s.isEmpty()) { any = true; break; }
        e.w = 4 * 18 + 8;
        e.h = 26;
        if (!any && !editing) return;
        Draw.rrect(g, e.x, e.y, e.x + e.w, e.y + e.h, 4, 0xAA0C0C10);
        g.fill(e.x + 4, e.y + e.h - 3, e.x + e.w - 4, e.y + e.h - 2, accent);
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getInventory().armor.get(3 - i);
            if (s.isEmpty()) continue;
            int ix = e.x + 4 + i * 18, iy = e.y + 4;
            g.drawItem(s, ix, iy);
            g.drawItemInSlot(tr, s, ix, iy);
        }
    }

    private static void refresh(MinecraftClient mc, TextRenderer tr, boolean editing) {
        for (El e : ELEMENTS) {
            switch (e.id) {
                case "hud_watermark" -> set(e, tr, "Iwoho Visuals");
                case "hud_ping" -> {
                    int ping = 0;
                    if (mc.getNetworkHandler() != null && mc.player != null) {
                        PlayerListEntry pe = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                        if (pe != null) ping = pe.getLatency();
                    }
                    set(e, tr, ping + " ms");
                }
                case "hud_coords" -> set(e, tr, mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ());
                case "hud_time" -> set(e, tr, LocalTime.now().format(TIME));
                case "hud_potions" -> {
                    Collection<StatusEffectInstance> fx = mc.player.getStatusEffects();
                    List<String> out = new ArrayList<>();
                    for (StatusEffectInstance fi : fx) {
                        String n = net.minecraft.client.resource.language.I18n.translate(fi.getTranslationKey());
                        if (fi.getAmplifier() > 0) n += " " + (fi.getAmplifier() + 1);
                        int d = fi.getDuration();
                        String t = (d < 0 || d > 99999) ? "**" : (d / 20 / 60) + ":" + String.format("%02d", (d / 20) % 60);
                        out.add(n + " " + t);
                    }
                    if (out.isEmpty() && editing) out.add("Зелья");
                    if (out.isEmpty()) out.add("");
                    e.lines = out.toArray(new String[0]);
                    e.lw = new int[e.lines.length];
                    for (int i = 0; i < e.lines.length; i++) e.lw[i] = tr.getWidth(e.lines[i]);
                }
                default -> {}
            }
        }
    }

    private static void set(El e, TextRenderer tr, String s) {
        if (e.lines.length != 1) { e.lines = new String[1]; e.lw = new int[1]; }
        if (!s.equals(e.lines[0])) {
            e.lines[0] = s;
            e.lw[0] = tr.getWidth(s);
        }
    }
}
