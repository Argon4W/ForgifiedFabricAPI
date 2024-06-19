package net.fabricmc.fabric.impl.registry.sync;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.mixin.registry.sync.BaseMappedRegistryAccessor;
import net.fabricmc.fabric.mixin.registry.sync.MappedRegistryAccessor;
import net.fabricmc.fabric.mixin.registry.sync.RegistryManagerAccessor;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.callback.AddCallback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unchecked", "rawtypes"})
public class FabricRegistryInit implements ModInitializer {
    private static final Map<Registry<?>, Event<RegistryEntryAddedCallback>> REGISTRY_ENTRY_ADDED_CALLBACKS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();
        bus.addListener(DataPackRegistryEvent.NewRegistry.class, DynamicRegistriesImpl::onNewDatapackRegistries);
    }

    public static <T> Event<RegistryEntryAddedCallback<T>> objectAddedEvent(Registry<T> registry) {
        return (Event<RegistryEntryAddedCallback<T>>) (Object) REGISTRY_ENTRY_ADDED_CALLBACKS.computeIfAbsent(registry, k -> {
            Event<RegistryEntryAddedCallback> event = EventFactory.createArrayBacked(RegistryEntryAddedCallback.class,
                callbacks -> (rawId, id, object) -> {
                    for (RegistryEntryAddedCallback callback : callbacks) {
                        callback.onEntryAdded(rawId, id, object);
                    }
                }
            );
            k.addCallback(AddCallback.class, (reg, id, key, val) -> event.invoker().onEntryAdded(id, key.location(), val));
            return event;
        });
    }

    public static void addRegistry(Registry<?> registry) {
        RegistryManagerAccessor.invokeTrackModdedRegistry(registry.key().location());

        boolean frozen = ((MappedRegistryAccessor) BuiltInRegistries.REGISTRY).getFrozen();
        if (frozen) {
            ((BaseMappedRegistryAccessor) BuiltInRegistries.REGISTRY).invokeUnfreeze();
        }

        ((WritableRegistry) BuiltInRegistries.REGISTRY).register(registry.key(), registry, RegistrationInfo.BUILT_IN);

        if (frozen) {
            ((WritableRegistry<?>) BuiltInRegistries.REGISTRY).freeze();
        }
    }
}
