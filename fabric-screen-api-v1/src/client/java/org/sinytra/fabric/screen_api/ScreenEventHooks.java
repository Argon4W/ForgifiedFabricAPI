package org.sinytra.fabric.screen_api;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ScreenEventHooks {
    @SubscribeEvent
    public static void beforeScreenDraw(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        ScreenEvents.beforeRender(screen).invoker().beforeRender(screen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void afterScreenDraw(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        ScreenEvents.afterRender(screen).invoker().afterRender(screen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void beforeKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!ScreenKeyboardEvents.allowKeyPress(screen).invoker().allowKeyPress(screen, event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        } else {
            ScreenKeyboardEvents.beforeKeyPress(screen).invoker().beforeKeyPress(screen, event.getKeyCode(), event.getScanCode(), event.getModifiers());
        }
    }

    @SubscribeEvent
    public static void afterKeyPressed(ScreenEvent.KeyPressed.Post event) {
        Screen screen = event.getScreen();
        ScreenKeyboardEvents.afterKeyPress(screen).invoker().afterKeyPress(screen, event.getKeyCode(), event.getScanCode(), event.getModifiers());
    }

    @SubscribeEvent
    public static void beforeKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        Screen screen = event.getScreen();
        if (!ScreenKeyboardEvents.allowKeyRelease(screen).invoker().allowKeyRelease(screen, event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        } else {
            ScreenKeyboardEvents.beforeKeyRelease(screen).invoker().beforeKeyRelease(screen, event.getKeyCode(), event.getScanCode(), event.getModifiers());
        }
    }

    @SubscribeEvent
    public static void afterKeyReleased(ScreenEvent.KeyReleased.Post event) {
        Screen screen = event.getScreen();
        ScreenKeyboardEvents.afterKeyRelease(screen).invoker().afterKeyRelease(screen, event.getKeyCode(), event.getScanCode(), event.getModifiers());
    }

    @SubscribeEvent
    public static void beforeMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!ScreenMouseEvents.allowMouseClick(screen).invoker().allowMouseClick(screen, event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        } else {
            ScreenMouseEvents.beforeMouseClick(screen).invoker().beforeMouseClick(screen, event.getMouseX(), event.getMouseY(), event.getButton());
        }
    }

    @SubscribeEvent
    public static void afterMouseClicked(ScreenEvent.MouseButtonPressed.Post event) {
        Screen screen = event.getScreen();
        ScreenMouseEvents.afterMouseClick(screen).invoker().afterMouseClick(screen, event.getMouseX(), event.getMouseY(), event.getButton());
    }

    @SubscribeEvent
    public static void beforeMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        Screen screen = event.getScreen();
        if (!ScreenMouseEvents.allowMouseRelease(screen).invoker().allowMouseRelease(screen, event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        } else {
            ScreenMouseEvents.beforeMouseRelease(screen).invoker().beforeMouseRelease(screen, event.getMouseX(), event.getMouseY(), event.getButton());
        }
    }

    @SubscribeEvent
    public static void afterMouseReleased(ScreenEvent.MouseButtonReleased.Post event) {
        Screen screen = event.getScreen();
        ScreenMouseEvents.afterMouseRelease(screen).invoker().afterMouseRelease(screen, event.getMouseX(), event.getMouseY(), event.getButton());
    }

    @SubscribeEvent
    public static void beforeMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        Screen screen = event.getScreen();
        if (!ScreenMouseEvents.allowMouseScroll(screen).invoker().allowMouseScroll(screen, event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        } else {
            ScreenMouseEvents.beforeMouseScroll(screen).invoker().beforeMouseScroll(screen, event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY());
        }
    }

    @SubscribeEvent
    public static void afterMouseScroll(ScreenEvent.MouseScrolled.Post event) {
        Screen screen = event.getScreen();
        ScreenMouseEvents.afterMouseScroll(screen).invoker().afterMouseScroll(screen, event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY());
    }
}
