package com.pyro.entity;

import com.pyro.particle.Fx;
import com.pyro.particle.ModParticles;
import com.pyro.power.FireOps;
import com.pyro.util.Snd;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One animated 3D fire effect (a GeckoLib entity). The Kind decides the model, the animation and the behaviour:
 * the server moves it and deals damage, the client only draws it (plus a few accent particles).
 */
public class FireFx extends Entity implements GeoEntity {

    public enum Kind {
        FIREBALL("fireball", 80, 1.6f, 1.6f, true, "animation.fireball.idle"),
        PILLAR("pillar", 36, 4.0f, 8.0f, false, "animation.pillar.main"),
        RING("ring", 24, 14.0f, 3.0f, false, "animation.ring.main"),
        AURA("aura", 200, 1.6f, 2.2f, true, "animation.aura.idle"),
        METEOR("meteor", 90, 5.0f, 9.0f, true, "animation.meteor.idle");

        public final String id;
        public final int life;
        public final float w, h;
        public final RawAnimation anim;

        Kind(String id, int life, float w, float h, boolean loop, String animName) {
            this.id = id;
            this.life = life;
            this.w = w;
            this.h = h;
            this.anim = loop ? RawAnimation.begin().thenLoop(animName) : RawAnimation.begin().thenPlayAndHold(animName);
        }
    }

    private final Kind kind;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private UUID owner;
    private Vec3d dir = Vec3d.ZERO;
    private float power = 1f;
    private int t = 0;
    private int life;
    private final Set<Integer> hit = new HashSet<>();

    public FireFx(EntityType<?> type, World world, Kind kind) {
        super(type, world);
        this.kind = kind;
        this.life = kind.life;
        this.noClip = true;
        this.setNoGravity(true);
    }

    public Kind kind() {
        return kind;
    }

    /** Spawns an effect on the server. dir is the movement per tick (fireball, meteor). */
    public static FireFx spawn(ServerWorld w, Kind k, Vec3d pos, PlayerEntity owner, Vec3d dir, float power, int lifeTicks) {
        FireFx e = FireEntities.type(k).create(w);
        if (e == null) return null;
        e.setPosition(pos);
        e.owner = owner == null ? null : owner.getUuid();
        e.dir = dir == null ? Vec3d.ZERO : dir;
        e.power = power;
        if (lifeTicks > 0) e.life = lifeTicks;
        e.setVelocity(e.dir);
        w.spawnEntity(e);
        return e;
    }

    // ------------------------------------------------------------------ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(kind.anim)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // ------------------------------------------------------------------ plain entity plumbing
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.t = 100000; // effects never survive a reload: they end on the first tick
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        t++;
        World w = getWorld();
        if (w.isClient) {
            clientTick(w);
            return;
        }
        ServerWorld sw = (ServerWorld) w;
        if (t > life) {
            discard();
            return;
        }
        PlayerEntity own = owner == null ? null : sw.getPlayerByUuid(owner);
        switch (kind) {
            case FIREBALL -> tickFireball(sw, own);
            case PILLAR -> tickPillar(sw, own);
            case RING -> tickRing(sw, own);
            case AURA -> tickAura(sw, own);
            case METEOR -> tickMeteor(sw, own);
        }
    }

    private void tickFireball(ServerWorld sw, PlayerEntity own) {
        Vec3d old = getPos();
        Vec3d next = old.add(dir);
        BlockHitResult bh = sw.raycast(new RaycastContext(old, next, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, this));
        if (bh.getType() != HitResult.Type.MISS) {
            setPosition(bh.getPos().subtract(dir.normalize().multiply(0.3)));
            blastFireball(sw, own);
            return;
        }
        List<LivingEntity> near = FireOps.targets(sw, new Box(old, next).expand(0.9), own);
        setPosition(next);
        if (!near.isEmpty()) blastFireball(sw, own);
    }

    private void blastFireball(ServerWorld sw, PlayerEntity own) {
        FireOps.blast(sw, getPos().add(0, 0.4, 0), own, 4.4, 9f * power, 140, true);
        discard();
    }

    private void tickPillar(ServerWorld sw, PlayerEntity own) {
        Vec3d p = getPos();
        if (t == 1) {
            Snd.play(sw, p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.4f, 0.6f);
            Snd.play(sw, p, SoundEvents.ITEM_FIRECHARGE_USE, 1.2f, 0.5f);
            Fx.flash(sw, p.add(0, 0.5, 0), 1.6);
            Fx.ring(sw, p.add(0, 0.2, 0), 0.9);
        }
        if (t >= 5 && t <= 30 && t % 4 == 1) {
            Box box = new Box(p.x - 2.6, p.y - 0.5, p.z - 2.6, p.x + 2.6, p.y + 8.0, p.z + 2.6);
            for (LivingEntity e : FireOps.targets(sw, box, own)) {
                double dx = e.getX() - p.x, dz = e.getZ() - p.z;
                if (dx * dx + dz * dz > 2.7 * 2.7) continue;
                FireOps.hurt(sw, e, own, 4.5f * power, 6);
                e.addVelocity(0, 0.75, 0);
                e.velocityModified = true;
            }
        }
    }

    private void tickRing(ServerWorld sw, PlayerEntity own) {
        Vec3d p = getPos();
        if (t == 1) {
            Snd.play(sw, p, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0f, 1.3f);
            Snd.play(sw, p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.3f, 0.5f);
            Fx.flash(sw, p.add(0, 0.5, 0), 1.8);
        }
        if (t >= 2 && t <= 18) {
            double r = ringRadius(t);
            Box box = new Box(p.x - r - 2, p.y - 1, p.z - r - 2, p.x + r + 2, p.y + 2.5, p.z + r + 2);
            for (LivingEntity e : FireOps.targets(sw, box, own)) {
                if (hit.contains(e.getId())) continue;
                double dx = e.getX() - p.x, dz = e.getZ() - p.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(d - r) > 1.8) continue;
                hit.add(e.getId());
                FireOps.hurt(sw, e, own, 7f * power, 6);
                double n = Math.max(0.2, d);
                e.addVelocity(dx / n * 1.2, 0.45, dz / n * 1.2);
                e.velocityModified = true;
            }
        }
    }

    /** Radius of the nova's flame ring at a given tick (same on server and client). */
    public static double ringRadius(int t) {
        return 1.5 + t * 0.5;
    }

    private void tickAura(ServerWorld sw, PlayerEntity own) {
        if (own == null || !own.isAlive()) {
            discard();
            return;
        }
        setPosition(own.getPos());
        own.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
        if (t % 10 == 0) {
            Vec3d p = getPos();
            for (LivingEntity e : FireOps.targets(sw, new Box(p.x - 3.4, p.y - 1, p.z - 3.4, p.x + 3.4, p.y + 3, p.z + 3.4), own)) {
                if (e.squaredDistanceTo(p) > 3.4 * 3.4) continue;
                FireOps.hurt(sw, e, own, 2.5f * power, 4);
            }
        }
        if (t % 40 == 1) Snd.play(sw, getPos(), SoundEvents.BLOCK_FIRE_AMBIENT, 0.8f, 0.8f);
    }

    private void tickMeteor(ServerWorld sw, PlayerEntity own) {
        Vec3d old = getPos();
        Vec3d next = old.add(dir);
        if (t % 6 == 1) Snd.play(sw, old, SoundEvents.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);
        BlockHitResult bh = sw.raycast(new RaycastContext(old, next.add(dir.normalize().multiply(1.5)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (bh.getType() != HitResult.Type.MISS) {
            Vec3d at = bh.getPos();
            setPosition(at);
            FireOps.blast(sw, at.add(0, 0.6, 0), own, 7.5, 20f * power, 320, true);
            Fx.ring(sw, at.add(0, 0.3, 0), 2.6);
            Fx.ring(sw, at.add(0, 0.3, 0), 1.6);
            Snd.play(sw, at, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.0f, 0.6f);
            discard();
            return;
        }
        setPosition(next);
    }

    // ------------------------------------------------------------------ client accents
    private void p(World w, String type, double x, double y, double z, double vx, double vy, double vz) {
        ParticleEffect e = ModParticles.get(type);
        if (e != null) w.addParticle(e, x, y, z, vx, vy, vz);
    }

    private void clientTick(World w) {
        Random r = this.random;
        double x = getX(), y = getY(), z = getZ();
        switch (kind) {
            case FIREBALL -> {
                for (int i = 0; i < 2; i++)
                    p(w, "flame", x + r.nextGaussian() * 0.18, y + 0.4 + r.nextGaussian() * 0.18, z + r.nextGaussian() * 0.18, 0, 0.02, 0);
                if (t % 2 == 0)
                    p(w, "ember", x, y + 0.4, z, r.nextGaussian() * 0.04, r.nextDouble() * 0.06, r.nextGaussian() * 0.04);
                if (t % 3 == 0) p(w, "smoke", x, y + 0.4, z, 0, 0.02, 0);
            }
            case PILLAR -> {
                if (t < life - 6) {
                    for (int i = 0; i < 3; i++) {
                        double a = r.nextDouble() * Math.PI * 2, rad = 0.3 + r.nextDouble() * 1.5;
                        p(w, "glow_fire", x + Math.cos(a) * rad, y + r.nextDouble() * 7.0, z + Math.sin(a) * rad, 0, 0.18 + r.nextDouble() * 0.2, 0);
                    }
                    double a = r.nextDouble() * Math.PI * 2;
                    p(w, "ember", x + Math.cos(a) * 1.8, y + 0.2, z + Math.sin(a) * 1.8, 0, 0.3 + r.nextDouble() * 0.25, 0);
                    if (t % 3 == 0) p(w, "smoke", x, y + 7.5, z, r.nextGaussian() * 0.05, 0.1, r.nextGaussian() * 0.05);
                }
            }
            case RING -> {
                double rad = ringRadius(Math.min(t, 18));
                for (int i = 0; i < 5; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    p(w, "flame", x + Math.cos(a) * rad, y + 0.15, z + Math.sin(a) * rad, Math.cos(a) * 0.05, 0.08, Math.sin(a) * 0.05);
                }
                if (t % 2 == 0) {
                    double a = r.nextDouble() * Math.PI * 2;
                    p(w, "ember", x + Math.cos(a) * rad, y + 0.3, z + Math.sin(a) * rad, Math.cos(a) * 0.1, 0.25, Math.sin(a) * 0.1);
                }
            }
            case AURA -> {
                for (int i = 0; i < 2; i++) {
                    double a = r.nextDouble() * Math.PI * 2, rad = 0.45 + r.nextDouble() * 0.35;
                    p(w, "glow_fire", x + Math.cos(a) * rad, y + r.nextDouble() * 1.9, z + Math.sin(a) * rad, 0, 0.04 + r.nextDouble() * 0.04, 0);
                }
                if (t % 3 == 0) {
                    double a = r.nextDouble() * Math.PI * 2;
                    p(w, "ember", x + Math.cos(a) * 0.7, y + r.nextDouble() * 1.2, z + Math.sin(a) * 0.7, 0, 0.1, 0);
                }
            }
            case METEOR -> {
                for (int i = 0; i < 6; i++)
                    p(w, "flame", x + r.nextGaussian() * 0.6, y + 1.2 + r.nextDouble() * 2.5, z + r.nextGaussian() * 0.6, 0, 0.05, 0);
                for (int i = 0; i < 2; i++)
                    p(w, "smoke", x + r.nextGaussian() * 0.5, y + 2 + r.nextDouble() * 3, z + r.nextGaussian() * 0.5, 0, 0.03, 0);
                p(w, "ember", x + r.nextGaussian() * 0.6, y + 1.5, z + r.nextGaussian() * 0.6, r.nextGaussian() * 0.05, 0.15, r.nextGaussian() * 0.05);
            }
        }
    }
}
