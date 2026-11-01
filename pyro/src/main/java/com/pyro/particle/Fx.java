package com.pyro.particle;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Server-side helpers that spawn the mod's accent particles. */
public final class Fx {
    private Fx() {}

    private static final Random R = new Random();

    public static void burst(ServerWorld w, String type, Vec3d p, int count, double spread, double speed) {
        w.spawnParticles(ModParticles.get(type), p.x, p.y, p.z, count, spread, spread, spread, speed);
    }

    public static void vel(ServerWorld w, String type, double x, double y, double z, double vx, double vy, double vz) {
        w.spawnParticles(ModParticles.get(type), x, y, z, 0, vx, vy, vz, 1.0);
    }

    public static void ring(ServerWorld w, Vec3d p, double mul) {
        w.spawnParticles(ModParticles.get("ring_fire"), p.x, p.y, p.z, 0, mul, 0, 0, 1.0);
    }

    public static void flash(ServerWorld w, Vec3d p, double mul) {
        w.spawnParticles(ModParticles.get("flash_fire"), p.x, p.y, p.z, 0, mul, 0, 0, 1.0);
    }

    /** Fiery explosion of accent particles around the 3D effect. */
    public static void explode(ServerWorld w, Vec3d p, int count, double speed) {
        for (int i = 0; i < count; i++) {
            Vec3d d = new Vec3d(R.nextGaussian(), R.nextGaussian() * 0.8, R.nextGaussian()).normalize()
                    .multiply(speed * (0.4 + R.nextDouble() * 0.8));
            vel(w, "flame", p.x, p.y, p.z, d.x, d.y, d.z);
            if (i % 2 == 0) vel(w, "ember", p.x, p.y, p.z, d.x * 1.4, d.y * 1.4 + 0.1, d.z * 1.4);
            if (i % 5 == 0) vel(w, "smoke", p.x, p.y, p.z, d.x * 0.5, d.y * 0.5 + 0.05, d.z * 0.5);
        }
        flash(w, p, 1.2 + count / 40.0);
        ring(w, p, 0.6 + count / 60.0);
    }
}
