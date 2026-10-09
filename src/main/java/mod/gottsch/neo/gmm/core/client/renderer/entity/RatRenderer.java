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
package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.RatModel;
import mod.gottsch.neo.gmm.core.entity.monster.Rat;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Apr 1, 2022
 *
 */
public class RatRenderer<T extends Rat> extends MobRenderer<T, RatModel<T>> {

    // TODO change this to pull from some sort of registry or config or this is just the DEFAULT texture;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/rat.png");
    private ResourceLocation texture = TEXTURE;
    private float scale = 1.0F;

	/**
	 *
	 * @param context
	 */
	public RatRenderer(EntityRendererProvider.Context context) {
        super(context, new RatModel<>(context.bakeLayer(RatModel.LAYER_LOCATION)), 0.3F);
    }

    public RatRenderer(EntityRendererProvider.Context context, float scale) {
        this(context);
        this.scale = scale;
    }

    public RatRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        this(context);
        this.texture = texture;
    }

    public RatRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        this(context, texture);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    protected void scale(Rat mob, PoseStack pose, float scale) {
        pose.scale(this.scale, this.scale, this.scale);
    }
    
    @Override
    public ResourceLocation getTextureLocation(Rat entity) {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public float getScale() {
        return scale;
    }
}
