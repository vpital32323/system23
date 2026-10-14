package com.iwoho.visuals.iris;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** Soft integration with Iris via reflection: the mod works fine without Iris installed. */
public final class IrisBridge {
    private static final String[] ROOTS = {"net.irisshaders.iris", "net.coderbot.iris"};

    private IrisBridge() {}

    public static boolean loaded() { return FabricLoader.getInstance().isModLoaded("iris"); }

    public static List<String> listPacks() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            s.forEach(p -> {
                String n = p.getFileName().toString();
                if (Files.isDirectory(p) || n.toLowerCase().endsWith(".zip")) out.add(n);
            });
        } catch (Exception ignored) {}
        Collections.sort(out);
        return out;
    }

    /** Select the shader pack and enable shaders. Call on the render thread. */
    public static boolean apply(String pack) {
        if (!loaded() || pack == null || pack.isEmpty()) return false;
        for (String root : ROOTS) {
            try {
                Class<?> iris = Class.forName(root + ".Iris");
                Object cfg = iris.getMethod("getIrisConfig").invoke(null);
                cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, pack);
                if (setEnabled(root, true)) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean disable() {
        if (!loaded()) return false;
        for (String root : ROOTS) {
            if (setEnabled(root, false)) return true;
        }
        return false;
    }

    private static boolean setEnabled(String root, boolean on) {
        try {
            Class<?> api = Class.forName(root + ".api.v0.IrisApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            Object conf = api.getMethod("getConfig").invoke(inst);
            Class<?> confIface = Class.forName(root + ".api.v0.IrisApiConfig");
            confIface.getMethod("setShadersEnabledAndApply", boolean.class).invoke(conf, on);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Screen openIrisScreen(Screen parent) {
        if (!loaded()) return null;
        for (String root : ROOTS) {
            try {
                Class<?> api = Class.forName(root + ".api.v0.IrisApi");
                Object inst = api.getMethod("getInstance").invoke(null);
                Object scr = api.getMethod("openMainIrisScreenObj", Object.class).invoke(inst, parent);
                if (scr instanceof Screen) return (Screen) scr;
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
