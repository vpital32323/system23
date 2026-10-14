package com.iwoho.visuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("iwohovisuals.json");
    private static Config instance = new Config();

    public static final int[] ACCENTS = {0xFF654DC4, 0xFF3358D4, 0xFFE0443E, 0xFF2E9E6B, 0xFFF0B429, 0xFF4FC3F7};
    public static final String[] ACCENT_NAMES = {"Фиолетовый", "Синий", "Красный", "Зелёный", "Жёлтый", "Голубой"};

    public Map<String, Boolean> toggles = new HashMap<>();
    public Map<String, int[]> hudPos = new HashMap<>();
    public Map<String, String> packs = new LinkedHashMap<>();
    public float hudScale = 1.0f;
    public int accent = 0;

    public static Config get() { return instance; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    Config c = GSON.fromJson(r, Config.class);
                    if (c != null) instance = c;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (instance.toggles == null) instance.toggles = new HashMap<>();
        if (instance.hudPos == null) instance.hudPos = new HashMap<>();
        if (instance.packs == null) instance.packs = new LinkedHashMap<>();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean on(String id, boolean def) {
        Boolean b = toggles.get(id);
        return b == null ? def : b;
    }

    public void set(String id, boolean v) { toggles.put(id, v); }

    public int[] pos(String id, int dx, int dy) {
        int[] p = hudPos.get(id);
        if (p == null || p.length < 2) {
            p = new int[]{dx, dy};
            hudPos.put(id, p);
        }
        return p;
    }

    public int accentColor() { return ACCENTS[Math.floorMod(accent, ACCENTS.length)]; }
}
