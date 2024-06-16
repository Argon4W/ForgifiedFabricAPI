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

package net.fabricmc.fabric.mixin.screenhandler;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {

    @ModifyArg(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;"), index = 0)
    private MenuProvider fabric_replaceMenuProvider(@Nullable MenuProvider arg) {
        if (arg instanceof SimpleMenuProvider simpleFactory && simpleFactory.menuConstructor instanceof ExtendedScreenHandlerFactory<?> extendedFactory) {
            return extendedFactory;
        }
        return arg;
    }

    @ModifyVariable(method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;"), argsOnly = true)
    private Consumer<RegistryFriendlyByteBuf> fabric_replaceExtraDataWriter(@Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter, MenuProvider arg, @Local @Nullable AbstractContainerMenu menu) {
		if (menu != null && arg instanceof ExtendedScreenHandlerFactory<?> extendedFactory && menu.getType() instanceof ExtendedScreenHandlerType extendedType) {
			return buf -> {
				Object data = extendedFactory.getScreenOpeningData((ServerPlayer) (Object) this);
                extendedType.getPacketCodec().encode(buf, data);
			};
		}
        return extraDataWriter;
    }
}
