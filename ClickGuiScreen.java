package com.iwoho.visuals.gui;

import com.iwoho.visuals.config.Config;
import com.iwoho.visuals.hud.Hud;
import com.iwoho.visuals.iris.IrisBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Animated, scrollable menu: sidebar categories + rows (toggles, sliders, buttons). */
public class ClickGuiScreen extends Screen {
    public static boolean editHud = false;

    private static final int W = 470, H = 290, SIDE = 112;
    private static final String[] CATS = {"HUD", "Визуал", "Шейдеры", "Система"};
    private static final String[][] PRESETS = {
            {"preset_newyear", "Новогодний"}, {"preset_evening", "Вечерний"},
            {"preset_winter", "Зимний / снег"}, {"preset_sky", "Небо"}
    };

    private float anim = 0f, ease = 0f;
    private boolean closing = false;
    private long last = Util.getMeasuringTimeMs();
    private int cat = 0;
    private float scroll = 0f, scrollTarget = 0f;
    private final List<Row> rows = new ArrayList<>();
    private Row activeDrag;
    private Hud.El dragEl;
    private float dragOffX, dragOffY;

    public ClickGuiScreen() {
        super(Text.literal("Iwoho Visuals"));
        rebuild();
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (editHud) { editHud = false; return; }
        closing = true;
    }

    // ---------- helpers ----------
    private int fade(int c) {
        int a = (int) (((c >>> 24) & 255) * ease);
        return (a << 24) | (c & 0xFFFFFF);
    }

    private void txt(DrawContext g, String s, int x, int y, int color) {
        int c = fade(color);
        if ((c >>> 24) < 4) return;
        g.drawText(textRenderer, s, x, y, c, false);
    }

    private int accent() { return Config.get().accentColor(); }

    private int cx() { return width / 2; }
    private int cy() { return height / 2; }
    private float sc() { return 0.9f + 0.1f * ease; }
    private int px() { return cx() - W / 2; }
    private int py() { return cy() - H / 2; }
    private int contentX() { return px() + SIDE + 16; }
    private int contentW() { return W - SIDE - 28; }
    private int contentTop() { return py() + 36; }
    private int contentBottom() { return py() + H - 12; }
    private int lx(double mx) { return (int) (cx() + (mx - cx()) / sc()); }
    private int ly(double my) { return (int) (cy() + (my - cy()) / sc()); }
    private int sx(int x) { return (int) (cx() + (x - cx()) * sc()); }
    private int sy(int y) { return (int) (cy() + (y - cy()) * sc()); }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void msg(String s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Iwoho] " + s), true);
    }

    // ---------- rows ----------
    private abstract class Row {
        int h;
        abstract void render(DrawContext g, int x, int y, int w, int mx, int my);
        boolean click(int x, int y, int w, int mx, int my) { return false; }
        void drag(int x, int w, int mx) {}
    }

    private class Toggle extends Row {
        final String name, desc, id; final boolean def; float t;
        Toggle(String name, String desc, String id, boolean def) {
            this.name = name; this.desc = desc; this.id = id; this.def = def; this.h = 32;
            this.t = Config.get().on(id, def) ? 1f : 0f;
        }
        void render(DrawContext g, int x, int y, int w, int mx, int my) {
            boolean on = Config.get().on(id, def);
            t += ((on ? 1f : 0f) - t) * 0.3f;
            boolean hov = in(mx, my, x, y, w, h - 4);
            Draw.rrect(g, x, y, x + w, y + h - 4, 6, fade(hov ? 0xFF1F1F27 : 0xFF17171D));
            if (desc != null) {
                txt(g, name, x + 10, y + 6, 0xFFFFFFFF);
                txt(g, desc, x + 10, y + 17, 0xFF8A8A96);
            } else {
                txt(g, name, x + 10, y + 10, 0xFFFFFFFF);
            }
            int swx = x + w - 38, swy = y + (h - 4) / 2 - 7;
            Draw.rrect(g, swx, swy, swx + 28, swy + 14, 7, fade(Draw.lerpColor(0xFF3A3A44, accent(), t)));
            int kx = swx + 2 + (int) (14 * t);
            Draw.rrect(g, kx, swy + 2, kx + 10, swy + 12, 5, fade(0xFFFFFFFF));
        }
        boolean click(int x, int y, int w, int mx, int my) {
            if (!in(mx, my, x, y, w, h - 4)) return false;
            Config c = Config.get();
            c.set(id, !c.on(id, def));
            Config.save();
            return true;
        }
    }

    private class Button extends Row {
        final Supplier<String> label; final Runnable action;
        Button(Supplier<String> label, Runnable action) {
            this.label = label; this.action = action; this.h = action == null ? 22 : 28;
        }
        void render(DrawContext g, int x, int y, int w, int mx, int my) {
            if (action == null) { txt(g, label.get(), x + 4, y + 6, 0xFF8A8A96); return; }
            boolean hov = in(mx, my, x, y, w, h - 4);
            Draw.rrect(g, x, y, x + w, y + h - 4, 6, fade(hov ? 0xFF24242E : 0xFF17171D));
            g.fill(x + 4, y + 5, x + 6, y + h - 9, fade(accent()));
            txt(g, label.get(), x + 12, y + 8, 0xFFFFFFFF);
            txt(g, "›", x + w - 14, y + 8, 0xFF8A8A96);
        }
        boolean click(int x, int y, int w, int mx, int my) {
            if (action == null || !in(mx, my, x, y, w, h - 4)) return false;
            action.run();
            return true;
        }
    }

    private class Slider extends Row {
        final String name; final float min, max;
        Slider(String name, float min, float max) { this.name = name; this.min = min; this.max = max; this.h = 36; }
        void render(DrawContext g, int x, int y, int w, int mx, int my) {
            Draw.rrect(g, x, y, x + w, y + h - 4, 6, fade(0xFF17171D));
            float v = Config.get().hudScale;
            txt(g, name, x + 10, y + 6, 0xFFFFFFFF);
            String vs = String.format("%.2fx", v);
            txt(g, vs, x + w - 10 - textRenderer.getWidth(vs), y + 6, 0xFFB5B5C0);
            int tx1 = x + 10, tx2 = x + w - 10, ty = y + 21;
            Draw.rrect(g, tx1, ty, tx2, ty + 4, 2, fade(0xFF3A3A44));
            float f = (v - min) / (max - min);
            int kx = tx1 + (int) ((tx2 - tx1) * f);
            Draw.rrect(g, tx1, ty, Math.max(tx1 + 2, kx), ty + 4, 2, fade(accent()));
            Draw.rrect(g, kx - 4, ty - 3, kx + 4, ty + 7, 4, fade(0xFFFFFFFF));
        }
        boolean click(int x, int y, int w, int mx, int my) {
            if (!in(mx, my, x, y, w, h - 4)) return false;
            activeDrag = this;
            drag(x, w, mx);
            return true;
        }
        void drag(int x, int w, int mx) {
            float f = MathHelper.clamp((mx - (x + 10)) / (float) (w - 20), 0f, 1f);
            Config.get().hudScale = Math.round((min + (max - min) * f) * 20f) / 20f;
        }
    }

    // ---------- tabs ----------
    private void rebuild() {
        rows.clear();
        Config cfg = Config.get();
        switch (cat) {
            case 0 -> {
                for (Hud.El e : Hud.ELEMENTS) rows.add(new Toggle(e.name, null, e.id, e.def));
                rows.add(new Slider("Масштаб HUD", 0.7f, 1.5f));
                rows.add(new Button(() -> "Редактор позиций (перетаскивание)", () -> editHud = true));
                rows.add(new Button(() -> "Сбросить позиции HUD", () -> { cfg.hudPos.clear(); Config.save(); }));
            }
            case 1 -> {
                rows.add(new Toggle("Теги над игроками", "Ник, HP и предметы над головой", "nametags", true));
                rows.add(new Toggle("HP в теге", "Показывать здоровье цветом", "tag_hp", true));
                rows.add(new Toggle("Броня в теге", "Иконки брони над игроком", "tag_armor", true));
                rows.add(new Toggle("Предмет в руке", "Иконка предмета в теге", "tag_hand", true));
                rows.add(new Toggle("Target ESP", "Анимированное кольцо на цели", "target_esp", true));
                rows.add(new Button(() -> "Цвет акцента: " + Config.ACCENT_NAMES[Math.floorMod(cfg.accent, Config.ACCENTS.length)],
                        () -> { cfg.accent = (cfg.accent + 1) % Config.ACCENTS.length; Config.save(); }));
            }
            case 2 -> {
                if (!IrisBridge.loaded()) {
                    rows.add(new Button(() -> "Iris не найден. Поставь Iris Shaders (Fabric 1.20.1).", null));
                } else {
                    for (String[] p : PRESETS) {
                        rows.add(new Button(() -> "▶ " + p[1], () -> applyPreset(p[0])));
                        rows.add(new Button(() -> "     Пак: " + packName(p[0]) + "  (клик — сменить)", () -> cyclePack(p[0])));
                    }
                    rows.add(new Button(() -> "Выключить шейдеры", () -> {
                        MinecraftClient.getInstance().execute(() -> msg(IrisBridge.disable() ? "Шейдеры выключены" : "Не удалось"));
                    }));
                    rows.add(new Button(() -> "Открыть меню Iris", () -> {
                        Screen s = IrisBridge.openIrisScreen(this);
                        if (s != null) MinecraftClient.getInstance().setScreen(s);
                    }));
                    rows.add(new Button(() -> "Паки берутся из папки shaderpacks.", null));
                }
            }
            default -> {
                rows.add(new Toggle("Анимация меню", "Плавное открытие и закрытие", "gui_anim", true));
                rows.add(new Button(() -> "Lite-настройки для слабого ПК", this::applyLite));
                rows.add(new Button(() -> "Для макс. FPS поставь Sodium + Lithium.", null));
            }
        }
        scroll = scrollTarget = 0;
    }

    private String packName(String id) {
        String n = Config.get().packs.get(id);
        return n == null || n.isEmpty() ? "не выбран" : (n.length() > 26 ? n.substring(0, 26) + "…" : n);
    }

    private void cyclePack(String id) {
        List<String> all = new ArrayList<>();
        all.add("");
        all.addAll(IrisBridge.listPacks());
        String cur = Config.get().packs.getOrDefault(id, "");
        int i = all.indexOf(cur);
        Config.get().packs.put(id, all.get((i + 1) % all.size()));
        Config.save();
    }

    private void applyPreset(String id) {
        String pack = Config.get().packs.get(id);
        if (pack == null || pack.isEmpty()) { msg("Сначала выбери пак для этого пресета"); return; }
        MinecraftClient.getInstance().execute(() -> msg(IrisBridge.apply(pack) ? "Шейдер: " + pack : "Не удалось применить пак"));
    }

    private void applyLite() {
        GameOptions o = MinecraftClient.getInstance().options;
        o.getParticles().setValue(ParticlesMode.MINIMAL);
        o.getEntityShadows().setValue(false);
        o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        o.getBobView().setValue(false);
        o.getGraphicsMode().setValue(GraphicsMode.FAST);
        o.write();
        msg("Lite-настройки применены");
    }

    private int totalHeight() { int t = 0; for (Row r : rows) t += r.h; return t; }

    // ---------- render ----------
    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        float dt = Math.min(0.05f, (now - last) / 1000f);
        last = now;
        float target = closing ? 0f : 1f;
        if (!Config.get().on("gui_anim", true)) anim = target;
        else anim = MathHelper.clamp(anim + (closing ? -dt : dt) / 0.18f, 0f, 1f);
        ease = 1f - (float) Math.pow(1f - anim, 3);
        if (closing && anim <= 0.001f) { MinecraftClient.getInstance().setScreen(null); return; }

        if (editHud) { renderEdit(g, mouseX, mouseY); return; }

        scroll += (scrollTarget - scroll) * Math.min(1f, dt * 14f);
        int mx = lx(mouseX), my = ly(mouseY);

        g.fill(0, 0, width, height, fade(0x80000000));

        MatrixStack ms = g.getMatrices();
        ms.push();
        ms.translate(cx(), cy(), 0);
        ms.scale(sc(), sc(), 1f);
        ms.translate(-cx(), -cy(), 0);

        int px = px(), py = py();
        Draw.rrect(g, px - 1, py - 1, px + W + 1, py + H + 1, 11, fade(0x30FFFFFF));
        Draw.rrect(g, px, py, px + W, py + H, 10, fade(0xF00D0D11));
        Draw.rrect(g, px + 6, py + 6, px + SIDE + 6, py + H - 6, 8, fade(0xFF131318));

        txt(g, "✦ Iwoho Visuals", px + 16, py + 16, 0xFFFFFFFF);
        for (int i = 0; i < CATS.length; i++) {
            int by = py + 42 + i * 26;
            boolean sel = i == cat, hov = in(mx, my, px + 10, by, SIDE - 8, 22);
            if (sel) Draw.rrect(g, px + 10, by, px + SIDE + 2, by + 22, 6, fade((accent() & 0xFFFFFF) | 0x55000000));
            else if (hov) Draw.rrect(g, px + 10, by, px + SIDE + 2, by + 22, 6, fade(0x22FFFFFF));
            txt(g, CATS[i], px + 20, by + 7, sel ? 0xFFFFFFFF : 0xFFA0A0AC);
        }
        txt(g, "RSHIFT — меню", px + 16, py + H - 22, 0xFF5E5E6A);

        txt(g, CATS[cat], contentX(), py + 16, 0xFFFFFFFF);
        g.fill(contentX(), py + 28, contentX() + contentW(), py + 29, fade(0x22FFFFFF));

        g.enableScissor(sx(contentX() - 4), sy(contentTop()), sx(contentX() + contentW() + 4), sy(contentBottom()));
        int y = contentTop() - (int) scroll;
        for (Row r : rows) {
            if (y + r.h > contentTop() - 2 && y < contentBottom()) {
                boolean inside = my >= contentTop() && my < contentBottom();
                r.render(g, contentX(), y, contentW(), mx, inside ? my : -1000);
            }
            y += r.h;
        }
        g.disableScissor();
        ms.pop();
    }

    private void renderEdit(DrawContext g, int mouseX, int mouseY) {
        Config cfg = Config.get();
        g.fill(0, 0, width, height, fade(0x50000000));
        String hint = "Перетаскивай элементы мышью. ESC — назад в меню";
        g.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, 12, fade(0xFFFFFFFF) | 0xFF000000, true);
        MatrixStack ms = g.getMatrices();
        ms.push();
        ms.scale(cfg.hudScale, cfg.hudScale, 1f);
        for (Hud.El e : Hud.ELEMENTS) {
            if (!cfg.on(e.id, e.def) || e.w == 0) continue;
            Draw.border(g, e.x - 1, e.y - 1, e.w + 2, e.h + 2, e == dragEl ? 0xFFFFFFFF : (accent() | 0xFF000000));
        }
        ms.pop();
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing || button != 0) return false;
        Config cfg = Config.get();
        if (editHud) {
            float s = cfg.hudScale;
            float hx = (float) (mouseX / s), hy = (float) (mouseY / s);
            for (int i = Hud.ELEMENTS.size() - 1; i >= 0; i--) {
                Hud.El e = Hud.ELEMENTS.get(i);
                if (!cfg.on(e.id, e.def) || e.w == 0) continue;
                if (hx >= e.x && hx < e.x + e.w && hy >= e.y && hy < e.y + e.h) {
                    dragEl = e; dragOffX = hx - e.x; dragOffY = hy - e.y;
                    return true;
                }
            }
            return true;
        }
        int mx = lx(mouseX), my = ly(mouseY);
        int px = px(), py = py();
        for (int i = 0; i < CATS.length; i++) {
            if (in(mx, my, px + 10, py + 42 + i * 26, SIDE - 8, 22)) {
                if (cat != i) { cat = i; rebuild(); }
                return true;
            }
        }
        if (my >= contentTop() && my < contentBottom()) {
            int y = contentTop() - (int) scroll;
            for (Row r : rows) {
                if (r.click(contentX(), y, contentW(), mx, my)) return true;
                y += r.h;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        Config cfg = Config.get();
        if (editHud && dragEl != null) {
            float s = cfg.hudScale;
            int nx = (int) (mouseX / s - dragOffX), ny = (int) (mouseY / s - dragOffY);
            nx = MathHelper.clamp(nx, 0, Math.max(0, (int) (width / s) - dragEl.w));
            ny = MathHelper.clamp(ny, 0, Math.max(0, (int) (height / s) - dragEl.h));
            int[] p = cfg.pos(dragEl.id, dragEl.dx, dragEl.dy);
            p[0] = nx; p[1] = ny;
            return true;
        }
        if (activeDrag != null) {
            activeDrag.drag(contentX(), contentW(), lx(mouseX));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragEl != null || activeDrag != null) Config.save();
        dragEl = null;
        activeDrag = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (editHud) return true;
        float max = Math.max(0, totalHeight() - (contentBottom() - contentTop()));
        scrollTarget = MathHelper.clamp(scrollTarget - (float) amount * 24f, 0f, max);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && !editHud) { closing = true; return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() { editHud = false; Config.save(); }
}
