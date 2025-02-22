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

package net.fabricmc.fabric.impl.item;

import java.util.List;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentSource;
import net.fabricmc.fabric.impl.resource.loader.BuiltinModResourcePackSource;
import net.fabricmc.fabric.impl.resource.loader.FabricResource;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackCreator;
import net.fabricmc.fabric.mixin.item.EnchantmentBuilderAccessor;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentUtil {
	@SuppressWarnings("unchecked")
	public static Enchantment modify(ResourceKey<Enchantment> key, Enchantment originalEnchantment, EnchantmentSource source) {
		Enchantment.Builder builder = Enchantment.enchantment(originalEnchantment.definition());
		EnchantmentBuilderAccessor accessor = (EnchantmentBuilderAccessor) builder;

		builder.exclusiveWith(originalEnchantment.exclusiveSet());
		accessor.getEffectMap().addAll(originalEnchantment.effects());

		originalEnchantment.effects().stream()
				.forEach(component -> {
					if (component.value() instanceof List<?> valueList) {
						// component type cast is checked by the value
						accessor.invokeGetEffectsList((DataComponentType<List<Object>>) component.type())
								.addAll(valueList);
					}
				});

		EnchantmentEvents.MODIFY.invoker().modify(key, builder, source);

		return new Enchantment(
				originalEnchantment.description(),
				accessor.getDefinition(),
				accessor.getExclusiveSet(),
				accessor.getEffectMap().build()
		);
	}

	public static EnchantmentSource determineSource(Resource resource) {
		if (resource != null) {
			PackSource packSource = ((FabricResource) resource).getFabricPackSource();

			if (packSource == PackSource.BUILT_IN) {
				return EnchantmentSource.VANILLA;
			} else if (packSource == ModResourcePackCreator.RESOURCE_PACK_SOURCE || packSource instanceof BuiltinModResourcePackSource) {
				return EnchantmentSource.MOD;
			}
		}

		// If not builtin or mod, assume external data pack.
		// It might also be a virtual enchantment injected via mixin instead of being loaded
		// from a resource, but we can't determine that here.
		return EnchantmentSource.DATA_PACK;
	}

	private EnchantmentUtil() { }
}
