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

import mod.gottsch.forge.gmm.core.GMM;
import mod.gottsch.forge.gmm.core.entity.monster.canine.WinterWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Winter Wolf: a white and pale-blue coat at 1.4x, with full-bright ice-blue eyes.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class WinterWolfRenderer<T extends WinterWolf> extends CanineRenderer<T> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(GMM.MOD_ID, "textures/entity/winter_wolf.png");
    public static final ResourceLocation EYES = new ResourceLocation(GMM.MOD_ID, "textures/entity/layer/winter_wolf_eyes.png");
    public static final float SCALE = 1.4F;

    public WinterWolfRenderer(EntityRendererProvider.Context context) {
        this(context, TEXTURE, SCALE);
    }

    public WinterWolfRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        super(context, texture, scale, EYES);
    }
}
