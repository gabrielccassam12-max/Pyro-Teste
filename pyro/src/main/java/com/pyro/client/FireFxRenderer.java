package com.pyro.client;

import com.pyro.Pyro;
import com.pyro.entity.FireFx;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws a fire effect from its GeckoLib model. Files used (for effect "fireball"):
 *   geo/entity/fireball.geo.json, animations/entity/fireball.animation.json, textures/entity/fireball.png
 * The layer is translucent and ignores light, so the flames always glow.
 */
public class FireFxRenderer extends GeoEntityRenderer<FireFx> {
    public FireFxRenderer(EntityRendererFactory.Context ctx, String name) {
        super(ctx, new DefaultedEntityGeoModel<>(Identifier.of(Pyro.ID, name)));
        this.shadowRadius = 0f;
    }

    @Override
    public RenderLayer getRenderType(FireFx animatable, Identifier texture, VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityTranslucentEmissive(texture);
    }
}
