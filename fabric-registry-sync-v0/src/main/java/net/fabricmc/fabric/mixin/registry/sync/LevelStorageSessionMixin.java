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

package net.fabricmc.fabric.mixin.registry.sync;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.RegistryMapSerializer;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.sync.RemapException;
import net.fabricmc.fabric.impl.registry.sync.RemappableRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class LevelStorageSessionMixin {
	@Unique
	private static final int FABRIC_ID_REGISTRY_BACKUPS = 3;
	@Unique
	private static final Logger FABRIC_LOGGER = LoggerFactory.getLogger("FabricRegistrySync");
	@Unique
	private Map<ResourceLocation, Object2IntMap<ResourceLocation>> fabric_lastSavedRegistryMap = null;
	@Unique
	private Map<ResourceLocation, Object2IntMap<ResourceLocation>> fabric_activeRegistryMap = null;

	@Shadow
	@Final
	private LevelStorageSource.LevelDirectory levelDirectory;

	@Unique
	private boolean fabric_readIdMapFile(File file) throws IOException, RemapException {
		FABRIC_LOGGER.debug("Reading registry data from " + file.toString());

		if (file.exists()) {
			FileInputStream fileInputStream = new FileInputStream(file);
			CompoundTag tag = NbtIo.readCompressed(fileInputStream);
			fileInputStream.close();

			if (tag != null) {
				fabric_activeRegistryMap = RegistryMapSerializer.fromNbt(tag);
				RegistrySyncManager.apply(fabric_activeRegistryMap, RemappableRegistry.RemapMode.AUTHORITATIVE);
				return true;
			}
		}

		return false;
	}

	@Unique
	private File fabric_getWorldIdMapFile(int i) {
		return new File(new File(levelDirectory.path().toFile(), "data"), "fabricRegistry" + ".dat" + (i == 0 ? "" : ("." + i)));
	}

	@Unique
	private void fabric_saveRegistryData() {
		FABRIC_LOGGER.debug("Starting registry save");
		Map<ResourceLocation, Object2IntMap<ResourceLocation>> newMap = RegistrySyncManager.createAndPopulateRegistryMap(false, fabric_activeRegistryMap);

		if (newMap == null) {
			FABRIC_LOGGER.debug("Not saving empty registry data");
			return;
		}

		if (!newMap.equals(fabric_lastSavedRegistryMap)) {
			for (int i = FABRIC_ID_REGISTRY_BACKUPS - 1; i >= 0; i--) {
				File file = fabric_getWorldIdMapFile(i);

				if (file.exists()) {
					if (i == FABRIC_ID_REGISTRY_BACKUPS - 1) {
						file.delete();
					} else {
						File target = fabric_getWorldIdMapFile(i + 1);
						file.renameTo(target);
					}
				}
			}

			try {
				File file = fabric_getWorldIdMapFile(0);
				File parentFile = file.getParentFile();

				if (!parentFile.exists()) {
					if (!parentFile.mkdirs()) {
						FABRIC_LOGGER.warn("[fabric-registry-sync] Could not create directory " + parentFile + "!");
					}
				}

				FABRIC_LOGGER.debug("Saving registry data to " + file);
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				NbtIo.writeCompressed(RegistryMapSerializer.toNbt(newMap), fileOutputStream);
				fileOutputStream.close();
			} catch (IOException e) {
				FABRIC_LOGGER.warn("[fabric-registry-sync] Failed to save registry file!", e);
			}

			fabric_lastSavedRegistryMap = newMap;
		}
	}

	@Inject(method = "saveDataTag(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/storage/WorldData;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"))
	public void saveWorld(RegistryAccess registryTracker, WorldData saveProperties, CompoundTag compoundTag, CallbackInfo info) {
		if (!Files.exists(levelDirectory.path())) {
			return;
		}

		fabric_saveRegistryData();
	}

	// TODO: stop double save on client?
	@Inject(method = "readAdditionalLevelSaveData", at = @At("HEAD"), remap = false)
	public void readWorldProperties(CallbackInfo callbackInfo) {
		// Load
		for (int i = 0; i < FABRIC_ID_REGISTRY_BACKUPS; i++) {
			FABRIC_LOGGER.trace("[fabric-registry-sync] Loading Fabric registry [file " + (i + 1) + "/" + (FABRIC_ID_REGISTRY_BACKUPS + 1) + "]");

			try {
				if (fabric_readIdMapFile(fabric_getWorldIdMapFile(i))) {
					FABRIC_LOGGER.info("[fabric-registry-sync] Loaded registry data [file " + (i + 1) + "/" + (FABRIC_ID_REGISTRY_BACKUPS + 1) + "]");
					return;
				}
			} catch (FileNotFoundException e) {
				// pass
			} catch (IOException e) {
				if (i >= FABRIC_ID_REGISTRY_BACKUPS - 1) {
					throw new RuntimeException(e);
				} else {
					FABRIC_LOGGER.warn("Reading registry file failed!", e);
				}
			} catch (RemapException e) {
				throw new RuntimeException("Remapping world failed!", e);
			}
		}

		// If not returned (not present), try saving the registry data
		fabric_saveRegistryData();
	}
}
