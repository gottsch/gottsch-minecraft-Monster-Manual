/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * gottsch's Monster Manual is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gottsch's Monster Manual is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with gottsch's Monster Manual.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.forge.gmm.core.client.model.CanineModel;
import mod.gottsch.forge.gmm.core.client.renderer.entity.layer.GMMEyesLayer;
import mod.gottsch.forge.gmm.core.entity.monster.canine.GMMCanine;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Shared renderer for the canine family: the one {@link CanineModel} rig at a per-mob scale and
 * texture, with optional full-bright eyes ({@link GMMEyesLayer}). Each mob has a thin subclass with
 * its own defaults ({@link DireWolfRenderer}, {@link HellhoundRenderer}, {@link WinterWolfRenderer}),
 * so a consumer registers it as {@code HellhoundRenderer::new}.
 *
 * <p>The scale draws the rig; the hitbox is the consumer's {@code EntityType.Builder#sized}, for
 * which each mob publishes {@code WIDTH}/{@code HEIGHT} to match its default scale.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class CanineRenderer<T extends GMMCanine> extends MobRenderer<T, CanineModel<T>> {
    // vanilla wolf's shadow, scaled with the rig
    private static final float BASE_SHADOW = 0.5F;

    private final ResourceLocation texture;
    private final float scale;

    public CanineRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale, @Nullable ResourceLocation eyesTexture) {
        super(context, new CanineModel<>(context.bakeLayer(CanineModel.LAYER_LOCATION)), BASE_SHADOW * scale);
        this.texture = texture;
        this.scale = scale;
        if (eyesTexture != null) {
            this.addLayer(new GMMEyesLayer<>(this, eyesTexture));
        }
    }

    @Override
    protected void scale(T mob, PoseStack pose, float partialTick) {
        pose.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
