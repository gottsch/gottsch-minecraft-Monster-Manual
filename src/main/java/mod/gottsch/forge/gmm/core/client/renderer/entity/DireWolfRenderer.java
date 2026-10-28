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
import mod.gottsch.forge.gmm.core.entity.monster.canine.DireWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Dire Wolf: a dark timber wolf at 1.3x, with plain (not glowing) eyes.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class DireWolfRenderer<T extends DireWolf> extends CanineRenderer<T> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(GMM.MOD_ID, "textures/entity/dire_wolf.png");
    public static final float SCALE = 1.3F;

    public DireWolfRenderer(EntityRendererProvider.Context context) {
        this(context, TEXTURE, SCALE);
    }

    public DireWolfRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        super(context, texture, scale, null);
    }
}
