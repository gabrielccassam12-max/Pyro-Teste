package com.pyro.particle;

import com.pyro.Pyro;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;

/** The mod's own accent particles (not vanilla). Names match assets/pyro/particles/<name>.json. */
public final class ModParticles {
    private ModParticles() {}

    public static final Map<String, ParticleProfile> PROFILES = new LinkedHashMap<>();
    public static final Map<String, SimpleParticleType> TYPES = new LinkedHashMap<>();

    private static void def(String name, String tex, int rgb, int life, float size, float grow, float gravity,
                            float drag, float spin, float rise, boolean sizeFromVx, boolean glow) {
        PROFILES.put(name, new ParticleProfile(tex, rgb, life, size, grow, gravity, drag, spin, rise, sizeFromVx, glow));
    }

    static {
        //   name          texture  colour    life size  grow  grav  drag  spin  rise  sizeVx glow
        def("flame",      "flame", 0xFFFFFF, 16, 0.62f, 0.96f, 0.0f, 0.92f, 0.00f, 0.014f, false, true);
        def("ember",      "spark", 0xFF9628, 24, 0.17f, 0.97f, 0.3f, 0.94f, 0.10f, 0.000f, false, true);
        def("smoke",      "soft",  0x3C373C, 34, 0.55f, 1.03f, 0.0f, 0.90f, 0.02f, 0.010f, false, false);
        def("glow_fire",  "soft",  0xFF5A14, 14, 0.36f, 0.96f, 0.0f, 0.92f, 0.00f, 0.010f, false, true);
        def("ring_fire",  "ring",  0xFF6E1E, 9,  0.50f, 1.28f, 0.0f, 1.00f, 0.00f, 0.000f, true, true);
        def("flash_fire", "flash", 0xFFA03C, 6,  0.90f, 1.12f, 0.0f, 1.00f, 0.20f, 0.000f, true, true);
    }

    public static SimpleParticleType get(String name) {
        return TYPES.get(name);
    }

    public static void init() {
        for (String name : PROFILES.keySet()) {
            SimpleParticleType t = FabricParticleTypes.simple(true);
            Registry.register(Registries.PARTICLE_TYPE, Pyro.id(name), t);
            TYPES.put(name, t);
        }
    }
}
