package dev.umf.autoae2;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@EventBusSubscriber(modid = AutoAE2.MOD_ID, value = Dist.CLIENT)
public final class AutoAE2Client {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(AutoAE2.PLANNER_MENU.get(), PlannerScreen::new);
    }
    @SubscribeEvent public static void feedback(RegisterClientPayloadHandlersEvent event) {
        event.register(PlacementFeedback.TYPE,(message,context)->{
            var client=Minecraft.getInstance();
            if(client.screen instanceof PlacementControlsScreen controls) controls.showFeedback(message.message());
            else client.gui.setOverlayMessage(Component.literal(message.message()),false);
        });
    }
}
