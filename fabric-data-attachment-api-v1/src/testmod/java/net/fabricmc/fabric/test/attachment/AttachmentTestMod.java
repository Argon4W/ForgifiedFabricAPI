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

package net.fabricmc.fabric.test.attachment;

import java.io.File;
import java.io.IOException;

import com.mojang.serialization.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.LevelResource;

public class AttachmentTestMod implements ModInitializer {
	public static final String MOD_ID = "fabric-data-attachment-api-v1-testmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final AttachmentType<String> PERSISTENT = AttachmentRegistry.createPersistent(
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "persistent"),
			Codec.STRING
	);
	public static final AttachmentType<String> FEATURE_ATTACHMENT = AttachmentRegistry.create(
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "feature")
	);

	public static final ChunkPos FAR_CHUNK_POS = new ChunkPos(300, 0);

	private boolean serverStarted = false;
	public static boolean featurePlaced = false;

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "set_attachment"), new SetAttachmentFeature(NoneFeatureConfiguration.CODEC));

		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.VEGETAL_DECORATION,
				ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "set_attachment"))
		);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			File saveRoot = server.getWorldPath(LevelResource.ROOT).toFile();
			File markerFile = new File(saveRoot, MOD_ID + "_MARKER");
			boolean firstLaunch;

			try {
				firstLaunch = markerFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			ServerLevel overworld = server.overworld();
			LevelChunk chunk = overworld.getChunk(0, 0);

			if (firstLaunch) {
				LOGGER.info("First launch, testing attachment by feature");

				if (featurePlaced) {
					if (!"feature".equals(chunk.getAttached(FEATURE_ATTACHMENT))) {
						throw new AssertionError("Feature did not write attachment to ProtoChunk");
					}
				} else {
					LOGGER.warn("Feature not placed, could not test writing during worldgen");
				}

				LOGGER.info("setting up persistent attachments");

				overworld.setAttached(PERSISTENT, "world_data");

				chunk.setAttached(PERSISTENT, "chunk_data");

				ProtoChunk protoChunk = (ProtoChunk) overworld.getChunkSource().getChunk(FAR_CHUNK_POS.x, FAR_CHUNK_POS.z, ChunkStatus.STRUCTURE_STARTS, true);
				protoChunk.setAttached(PERSISTENT, "protochunk_data");
			} else {
				LOGGER.info("Second launch, testing persistent attachments");

				if (!"world_data".equals(overworld.getAttached(PERSISTENT))) throw new AssertionError("World attachment did not persist");
				if (!"chunk_data".equals(chunk.getAttached(PERSISTENT))) throw new AssertionError("WorldChunk attachment did not persist");

				ImposterProtoChunk wrapperProtoChunk = (ImposterProtoChunk) overworld.getChunkSource().getChunk(0, 0, ChunkStatus.EMPTY, true);
				if (!"chunk_data".equals(wrapperProtoChunk.getAttached(PERSISTENT))) throw new AssertionError("Attachment is not accessible through WrapperProtoChunk");

				ChunkAccess farChunk = overworld.getChunkSource().getChunk(FAR_CHUNK_POS.x, FAR_CHUNK_POS.z, ChunkStatus.EMPTY, true);

				if (farChunk instanceof ImposterProtoChunk) {
					LOGGER.warn("Far chunk already generated, can't test persistence in ProtoChunk.");
				} else {
					if (!"protochunk_data".equals(farChunk.getAttached(PERSISTENT))) throw new AssertionError("ProtoChunk attachment did not persist");
				}
			}

			serverStarted = true;
		});

		// Testing hint: load far chunk by running /tp @s 4800 ~ 0
		ServerChunkEvents.CHUNK_LOAD.register(((world, chunk) -> {
			if (!chunk.getPos().equals(FAR_CHUNK_POS)) return;

			if (!serverStarted) {
				LOGGER.warn("Chunk {} loaded before server started, can't test transfer of attachments to WorldChunk", FAR_CHUNK_POS);
				return;
			}

			LOGGER.info("Loaded chunk {}, testing transfer of attachments to WorldChunk", FAR_CHUNK_POS);

			if (!"protochunk_data".equals(chunk.getAttached(PERSISTENT))) throw new AssertionError("ProtoChunk attachment was not transfered to WorldChunk");
		}));
	}
}
