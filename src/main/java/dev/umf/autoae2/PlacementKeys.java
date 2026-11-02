package dev.umf.autoae2;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=AutoAE2.MOD_ID,value=Dist.CLIENT)
public final class PlacementKeys {
    private static final KeyMapping.Category CATEGORY=new KeyMapping.Category(Identifier.fromNamespaceAndPath(AutoAE2.MOD_ID,"controls"));
    private static final KeyMapping OPEN=new KeyMapping("key.autoae2.placement_controls_v2",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,CATEGORY);
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.registerCategory(CATEGORY); event.register(OPEN); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        while(OPEN.consumeClick()) if(mc.player!=null && mc.screen==null && mc.player.getMainHandItem().is(AutoAE2.WRENCH.get()) && PlacementWrench.linked(mc.player.getMainHandItem())) mc.setScreen(new PlacementControlsScreen());
    }
}
