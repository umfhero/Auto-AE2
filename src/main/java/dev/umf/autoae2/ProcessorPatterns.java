package dev.umf.autoae2;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.misc.InscriberRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Encodes the standard AE2 processor recipes, one blank per provider and recipe. */
public final class ProcessorPatterns {
    public record Spec(BlockPos provider, String middle, String top, String bottom, String output, boolean printing) {}
    private static final String[][] PRINT={
        {"minecraft:gold_ingot","ae2:logic_processor_press","ae2:printed_logic_processor"},
        {"ae2:silicon","ae2:silicon_press","ae2:printed_silicon"},
        {"ae2:certus_quartz_crystal","ae2:calculation_processor_press","ae2:printed_calculation_processor"},
        {"minecraft:diamond","ae2:engineering_processor_press","ae2:printed_engineering_processor"}
    };
    public static List<Spec> plan(List<BlueprintCatalog.Cell> cells) {
        var result=new ArrayList<Spec>();
        for(var provider:cells)if(provider.block().equals("ae2:pattern_provider")) {
            var pos=provider.at(BlockPos.ZERO,0);var presses=new LinkedHashSet<String>();boolean assembly=false;
            for(var machine:cells)if(machine.block().equals("ae2:inscriber") && machine.at(BlockPos.ZERO,0).distManhattan(pos)==1) {
                if(machine.inserted()==null)assembly=true;else presses.add(machine.inserted());
            }
            for(var press:presses)for(var recipe:PRINT)if(recipe[1].equals(press))result.add(new Spec(pos,recipe[0],recipe[1],null,recipe[2],true));
            if(assembly)for(var type:List.of("logic","calculation","engineering"))result.add(new Spec(pos,"minecraft:redstone","ae2:printed_"+type+"_processor","ae2:printed_silicon","ae2:"+type+"_processor",false));
        }
        return List.copyOf(result);
    }
    private static ItemStack item(String id) { return id==null?ItemStack.EMPTY:BuiltInRegistries.ITEM.getValue(Identifier.parse(id)).getDefaultInstance(); }
    public static boolean matchesRecipe(ServerLevel level,Spec spec) {
        var recipe=InscriberRecipes.findRecipe(level,item(spec.middle()),item(spec.top()),item(spec.bottom()),false);
        return recipe!=null && ItemStack.isSameItemSameComponents(recipe.result().create(),item(spec.output())) && recipe.result().create().getCount()==1;
    }
    public static ItemStack encode(Spec spec) {
        var inputs=new ArrayList<GenericStack>();inputs.add(new GenericStack(AEItemKey.of(item(spec.middle())),1));
        if(!spec.printing()) {inputs.add(new GenericStack(AEItemKey.of(item(spec.top())),1));inputs.add(new GenericStack(AEItemKey.of(item(spec.bottom())),1));}
        return PatternDetailsHelper.encodeProcessingPattern(inputs,List.of(new GenericStack(AEItemKey.of(item(spec.output())),1)));
    }
    private ProcessorPatterns() {}
}
