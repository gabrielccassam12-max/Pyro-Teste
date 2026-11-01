package com.pyro.client;

import com.pyro.entity.FireEntities;
import com.pyro.entity.FireFx;
import com.pyro.net.ActionPayload;
import com.pyro.net.SyncPayload;
import com.pyro.particle.ModParticles;
import com.pyro.particle.ParticleProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.SimpleParticleType;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class PyroClient implements ClientModInitializer {
    public static final KeyBinding[] KEYS = new KeyBinding[5];

    @Override
    public void onInitializeClient() {
        // ---- the mod's own particles
        for (Map.Entry<String, SimpleParticleType> en : ModParticles.TYPES.entrySet()) {
            ParticleProfile pf = ModParticles.PROFILES.get(en.getKey());
            ParticleFactoryRegistry.getInstance().register(en.getValue(), sprites ->
                    (ParticleFactory<SimpleParticleType>) (type, world, x, y, z, vx, vy, vz) ->
                            new GlowParticle(world, x, y, z, vx, vy, vz, sprites, pf));
        }

        // ---- 3D fire effects (GeckoLib)
        for (FireFx.Kind k : FireFx.Kind.values()) {
            EntityRendererRegistry.register(FireEntities.type(k), ctx -> new FireFxRenderer(ctx, k.id));
        }

        // ---- keys Z X C V B
        int[] codes = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B};
        for (int i = 0; i < 5; i++) {
            KEYS[i] = KeyBindingHelper.registerKeyBinding(
                    new KeyBinding("key.pyro.s" + (i + 1), InputUtil.Type.KEYSYM, codes[i], "key.categories.pyro"));
        }

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> ClientState.set(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.set(null));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientState.tick(client);
            if (client.player == null || client.currentScreen != null) return;
            for (int i = 0; i < 5; i++) {
                while (KEYS[i].wasPressed()) ClientPlayNetworking.send(new ActionPayload(i + 1));
            }
        });

        HudRenderCallback.EVENT.register(PyroHud::render);
    }
}
