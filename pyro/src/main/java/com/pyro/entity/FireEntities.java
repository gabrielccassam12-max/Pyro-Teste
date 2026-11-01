package com.pyro.entity;

import com.pyro.Pyro;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.EnumMap;
import java.util.Map;

/** One entity type per fire effect (each has its own GeckoLib model, animation and texture). */
public final class FireEntities {
    private FireEntities() {}

    private static final Map<FireFx.Kind, EntityType<FireFx>> TYPES = new EnumMap<>(FireFx.Kind.class);

    public static EntityType<FireFx> type(FireFx.Kind k) {
        return TYPES.get(k);
    }

    public static void init() {
        for (FireFx.Kind k : FireFx.Kind.values()) {
            EntityType<FireFx> type = EntityType.Builder.<FireFx>create((t, w) -> new FireFx(t, w, k), SpawnGroup.MISC)
                    .dimensions(k.w, k.h)
                    .maxTrackingRange(16)
                    .trackingTickInterval(1)
                    .makeFireImmune()
                    .build(Pyro.ID + ":" + k.id);
            Registry.register(Registries.ENTITY_TYPE, Pyro.id(k.id), type);
            TYPES.put(k, type);
        }
    }
}
