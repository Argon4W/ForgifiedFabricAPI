package net.fabricmc.fabric.mixin.attachment;

import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BaseMappedRegistry.class)
public interface BaseMappedRegistryAccessor {
    @Invoker
    void invokeUnfreeze();
}
