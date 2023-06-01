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

package net.fabricmc.fabric.impl.event.lifecycle;

import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Set;

/**
 * A simple marker interface which holds references to chunks which block entities may be loaded or unloaded from.
 */
public interface LoadedChunksCache {
	Set<LevelChunk> fabric_getLoadedChunks();

	/**
	 * Marks a chunk as loaded in a world.
	 */
	void fabric_markLoaded(LevelChunk chunk);

	/**
	 * Marks a chunk as unloaded in a world.
	 */
	void fabric_markUnloaded(LevelChunk chunk);
}
