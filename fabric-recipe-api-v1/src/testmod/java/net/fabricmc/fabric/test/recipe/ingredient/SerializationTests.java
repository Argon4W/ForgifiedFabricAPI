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

package net.fabricmc.fabric.test.recipe.ingredient;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class SerializationTests {
	/**
	 * Check that trying to use a custom ingredient inside an array ingredient fails.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testArrayDeserialization(GameTestHelper context) {
		String ingredientJson = """
[
	{
		"fabric:type": "fabric:all",
		"ingredients": [
			{
				"item": "minecraft:stone"
			}
		]
	}, {
		"item": "minecraft:dirt"
	}
]
				""";
		JsonElement json = JsonParser.parseString(ingredientJson);

		try {
			Ingredient.CODEC_NONEMPTY.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
			context.succeed();
		} catch (JsonParseException e) {
			throw new GameTestAssertException("Using a custom ingredient inside an array ingredient should not have failed.");
		}
	}

	/**
	 * Check that we can serialise and deserialize a custom ingredient.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testCustomIngredientSerialization(GameTestHelper context) {
		for (boolean allowEmpty : List.of(false, true)) {
			String ingredientJson = """
					{"ingredients":[{"item":"minecraft:stone"}],"fabric:type":"fabric:all"}
					""".trim();

			Ingredient ingredient = DefaultCustomIngredients.all(
					Ingredient.of(Items.STONE)
			);
			Codec<Ingredient> ingredientCodec = allowEmpty ? Ingredient.CODEC : Ingredient.CODEC_NONEMPTY;
			JsonObject json = ingredientCodec.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow(IllegalStateException::new).getAsJsonObject();
			context.assertTrue(json.toString().equals(ingredientJson), "Unexpected json: " + json);
			// Make sure that we can deserialize it
			FabricIngredient deserialized = (FabricIngredient) ingredientCodec.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
			context.assertTrue(deserialized.getCustomIngredient() != null, "Custom ingredient was not deserialized");
			context.assertTrue(deserialized.getCustomIngredient().getSerializer() == ((FabricIngredient) ingredient).getCustomIngredient().getSerializer(), "Serializer did not match");
		}

		context.succeed();
	}
}
