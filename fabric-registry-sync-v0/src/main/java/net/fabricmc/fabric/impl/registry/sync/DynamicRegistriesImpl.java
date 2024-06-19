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

package net.fabricmc.fabric.impl.registry.sync;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.*;

public final class DynamicRegistriesImpl {
	private static final List<RegistryDataLoader.RegistryData<?>> DYNAMIC_REGISTRIES = new ArrayList<>();
	public static final Set<ResourceKey<? extends Registry<?>>> DYNAMIC_REGISTRY_KEYS = new HashSet<>();
	public static final Map<ResourceKey<? extends Registry<?>>, Codec<?>> NETWORK_CODECS = new HashMap<>();

	static {
		for (RegistryDataLoader.RegistryData<?> vanillaEntry : RegistryDataLoader.WORLDGEN_REGISTRIES) {
			DYNAMIC_REGISTRY_KEYS.add(vanillaEntry.key());
		}
	}

	private DynamicRegistriesImpl() {
	}

	public static <T> RegistryDataLoader.RegistryData<T> register(ResourceKey<? extends Registry<T>> key, Codec<T> codec) {
		Objects.requireNonNull(key, "Registry key cannot be null");
		Objects.requireNonNull(codec, "Codec cannot be null");

		if (!DYNAMIC_REGISTRY_KEYS.add(key)) {
			throw new IllegalArgumentException("Dynamic registry " + key + " has already been registered!");
		}

		var entry = new RegistryDataLoader.RegistryData<>(key, codec, false);
		DYNAMIC_REGISTRIES.add(entry);
		return entry;
	}

	public static <T> void addSyncedRegistry(ResourceKey<? extends Registry<T>> key, Codec<T> networkCodec, DynamicRegistries.SyncOption... options) {
		Objects.requireNonNull(key, "Registry key cannot be null");
		Objects.requireNonNull(networkCodec, "Network codec cannot be null");
		Objects.requireNonNull(options, "Options cannot be null");

        NETWORK_CODECS.put(key, networkCodec);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
    static void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        for (RegistryDataLoader.RegistryData dynamicRegistry : DYNAMIC_REGISTRIES) {
            Codec networkCodec = NETWORK_CODECS.get(dynamicRegistry.key());
            event.dataPackRegistry(dynamicRegistry.key(), dynamicRegistry.elementCodec(), networkCodec);
        }
    }
}
