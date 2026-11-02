package dev.umf.autoae2;

import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.resources.Identifier;

/** Optional recipe-viewer integration for the planner's requirements. */
@JeiPlugin
public final class PlannerJeiPlugin implements IModPlugin {
    @Override public Identifier getPluginUid() { return Identifier.fromNamespaceAndPath(AutoAE2.MOD_ID,"planner"); }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(PlannerScreen.class,new IGuiContainerHandler<PlannerScreen>() {
            @Override public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory,PlannerScreen screen,double x,double y) {
                var stack=screen.requirementAt(x,y);
                return stack.isEmpty()?Optional.empty():factory.createBuilder(stack).buildWithArea(screen.requirementArea(x,y));
            }
        });
    }
}
