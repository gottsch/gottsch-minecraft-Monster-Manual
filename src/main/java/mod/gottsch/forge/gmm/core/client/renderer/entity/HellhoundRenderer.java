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
import mod.gottsch.forge.gmm.core.entity.monster.canine.Hellhound;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Hellhound: a black coat with ember markings at 1.1x, lit by world light like any flesh mob,
 * with only its eyes full-bright. (The Ember Hound, by contrast, is full-bright all over.)
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class HellhoundRenderer<T extends Hellhound> extends CanineRenderer<T> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(GMM.MOD_ID, "textures/entity/hellhound.png");
    public static final ResourceLocation EYES = new ResourceLocation(GMM.MOD_ID, "textures/entity/layer/hellhound_eyes.png");
    public static final float SCALE = 1.1F;

    public HellhoundRenderer(EntityRendererProvider.Context context) {
        this(context, TEXTURE, SCALE);
    }

    public HellhoundRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        super(context, texture, scale, EYES);
    }
}
