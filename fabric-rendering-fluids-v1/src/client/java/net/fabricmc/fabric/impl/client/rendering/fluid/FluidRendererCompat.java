package net.fabricmc.fabric.impl.client.rendering.fluid;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class FluidRendererCompat {

	public static void onClientSetup(FMLClientSetupEvent event) {
		// Register forge handlers only to the "handlers" map and not "modHandlers"
		// This allows fabric mods to access render handlers for forge mods' fluids without them being
		// used for rendering fluids, as that should remain handled by forge
		Map<FluidType, FluidRenderHandler> forgeHandlers = new HashMap<>();
		for (Map.Entry<ResourceKey<Fluid>, Fluid> entry : BuiltInRegistries.FLUID.entrySet()) {
			Fluid fluid = entry.getValue();
			if (fluid != Fluids.EMPTY && fluid.getFluidType() != NeoForgeMod.EMPTY_TYPE.value() && FluidRenderHandlerRegistry.INSTANCE.get(fluid) == null) {
				FluidRenderHandler handler = forgeHandlers.computeIfAbsent(fluid.getFluidType(), ForgeFluidRenderHandler::new);
				((FluidRenderHandlerRegistryImpl) FluidRenderHandlerRegistry.INSTANCE).registerHandlerOnly(fluid, handler);
			}
		}
	}

	private record ForgeFluidRenderHandler(FluidType fluidType) implements FluidRenderHandler {
		@Override
		public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
            TextureAtlasSprite[] forgeSprites = FluidSpriteCache.getFluidSprites(view, pos, state);
			return forgeSprites[2] == null ? Arrays.copyOfRange(forgeSprites, 0, 2) : forgeSprites;
		}

		@Override
		public int getFluidColor(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
			int color = IClientFluidTypeExtensions.of(this.fluidType).getTintColor(state, view, pos);
			return 0x00FFFFFF & color;
		}
	}

	private FluidRendererCompat() {}
}
