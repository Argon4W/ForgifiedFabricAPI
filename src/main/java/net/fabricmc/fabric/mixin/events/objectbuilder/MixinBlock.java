/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
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

package net.fabricmc.fabric.mixin.events.objectbuilder;

import net.fabricmc.fabric.events.ObjectBuilderEvent;
import net.fabricmc.fabric.impl.util.HandlerArray;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(Block.class)
public class MixinBlock {
	@Inject(method = "<init>(Lnet/minecraft/block/Block$Settings;)V", at = @At("RETURN"))
	public void init(Block.Settings builder, CallbackInfo info) {
		for (BiConsumer<Block.Settings, Block> consumer : ((HandlerArray<BiConsumer<Block.Settings, Block>>) ObjectBuilderEvent.BLOCK).getBackingArray()) {
			consumer.accept(builder, (Block) (Object) this);
		}
	}
}
