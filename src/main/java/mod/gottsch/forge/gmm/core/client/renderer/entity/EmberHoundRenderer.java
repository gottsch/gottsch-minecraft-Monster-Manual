/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
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
import mod.gottsch.forge.gmm.core.GMM;
import mod.gottsch.forge.gmm.core.client.model.EmberHoundModel;
import mod.gottsch.forge.gmm.core.entity.monster.EmberHound;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Ember Hound full-bright (it is made of fire, like a Blaze), with Mage Flame's texture.
 * A doused hound loses the full-bright and its model draws the coat darker (see {@link EmberHoundModel}).
 *
 * @author Mark Gottschling on 10/16/2026
 */
public class EmberHoundRenderer<T extends EmberHound> extends MobRenderer<T, EmberHoundModel<T>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GMM.MOD_ID, "textures/entity/ember_hound.png");
    private ResourceLocation texture = TEXTURE;
    private float scale = 1.0F;

    public EmberHoundRenderer(EntityRendererProvider.Context context) {
        super(context, new EmberHoundModel<>(context.bakeLayer(EmberHoundModel.LAYER_LOCATION)), 0.4F);
    }

    public EmberHoundRenderer(EntityRendererProvider.Context context, float scale) {
        this(context);
        this.scale = scale;
    }

    public EmberHoundRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        this(context);
        this.texture = texture;
    }

    public EmberHoundRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        this(context, texture);
        this.scale = scale;
    }

    @Override
    protected void scale(T mob, PoseStack pose, float partialTick) {
        pose.scale(this.scale, this.scale, this.scale);
    }

    /** Full-bright while it burns; a doused hound takes the world's light like any other mob. */
    @Override
    protected int getBlockLightLevel(T mob, BlockPos pos) {
        return mob.isDoused() ? super.getBlockLightLevel(mob, pos) : 15;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
