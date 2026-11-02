package dev.umf.autoae2;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Counts taken from AE2's published simple_certus_farm.snbt; the bud and sample barrel contents are not construction costs. */
public final class CertusFarm {
    public record Requirement(String id, int count, String label) {
        public Item item() { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); }
    }
    public static final Requirement[] REQUIREMENTS = {
        new Requirement("ae2:blue_smart_cable",7,"Blue Smart Cable"),
        new Requirement("ae2:orange_smart_cable",2,"Orange Smart Cable"),
        new Requirement("ae2:growth_accelerator",4,"Growth Accelerator"),
        new Requirement("ae2:storage_bus",2,"ME Storage Bus"),
        new Requirement("ae2:annihilation_plane",1,"ME Annihilation Plane"),
        new Requirement("ae2:quartz_fiber",1,"Quartz Fiber"),
        new Requirement("minecraft:barrel",1,"Barrel")

    };
    public static final int TOTAL=18;
    private CertusFarm() {}
}
