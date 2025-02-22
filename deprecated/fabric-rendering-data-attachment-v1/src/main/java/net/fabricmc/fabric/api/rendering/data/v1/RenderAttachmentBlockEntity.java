/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.api.rendering.data.v1;

import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * This interface is guaranteed to be implemented on all {@link BlockEntity} instances.
 *
 * @deprecated Use {@link RenderDataBlockEntity} instead.
 */
@Deprecated
@FunctionalInterface
public interface RenderAttachmentBlockEntity {
	/**
	 * This method will be automatically called if {@link RenderDataBlockEntity#getRenderData()} is not overridden.
	 *
	 * @deprecated Use {@link RenderDataBlockEntity#getRenderData()} instead.
	 */
	@Deprecated
	@Nullable
	Object getRenderAttachmentData();
}
