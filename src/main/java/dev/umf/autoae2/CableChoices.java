package dev.umf.autoae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Select whole, electrically compatible cable groups; isolated groups must remain distinct. */
public final class CableChoices {
    public static final String[] COLORS={"fluix","white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
    public static String color(int index) { return COLORS[Math.clamp(index,0,COLORS.length-1)]; }
    private static String originalRole(String id,String blueprint) {
        if(!id.startsWith("ae2:") || !(id.endsWith("_smart_cable")||id.endsWith("_smart_dense_cable")))return null;
        if(id.startsWith("ae2:blue_"))return "main";
        if(blueprint.equals("charger") && id.startsWith("ae2:green_") || blueprint.equals("simple_certus_farm") && id.startsWith("ae2:orange_"))return "isolated";
        return null;
    }
    private static String replace(String id,String color) {
        return "ae2:"+color+(id.endsWith("_smart_dense_cable")?"_smart_dense_cable":"_smart_cable");
    }
    private static long score(BlueprintCatalog.Blueprint blueprint,String role,int index,IGrid grid,Player player,PlannerBlockEntity planner) {
        long score=0; boolean any=false,possible=true;
        for(var req:blueprint.requirements()) if(role.equals(originalRole(req.id(),blueprint.id()))) {
            any=true;var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(replace(req.id(),color(index))));var key=AEItemKey.of(item);
            long available=grid==null||key==null?0:grid.getStorageService().getInventory().extract(key,req.count(),Actionable.SIMULATE,IActionSource.ofPlayer(player,planner));
            boolean craftable=grid!=null && key!=null && grid.getCraftingService().isCraftable(key);
            if(available>=req.count())score+=100000;else if(craftable)score+=10000;else possible=false;
            score+=Math.min(available,req.count());
        }
        return any?(score+(possible?1000000000L:0)):0;
    }
    public static int[] select(BlueprintCatalog.Blueprint blueprint,IGrid grid,Player player,PlannerBlockEntity planner) {
        boolean main=Arrays.stream(blueprint.requirements()).anyMatch(r->"main".equals(originalRole(r.id(),blueprint.id())));
        boolean isolated=Arrays.stream(blueprint.requirements()).anyMatch(r->"isolated".equals(originalRole(r.id(),blueprint.id())));
        if(!main)return new int[]{0,0};
        int bestMain=isolated?12:0,bestOther=isolated?(blueprint.id().equals("charger")?14:2):0;
        long[] mainScore=new long[COLORS.length],otherScore=new long[COLORS.length];
        for(int i=0;i<COLORS.length;i++){mainScore[i]=score(blueprint,"main",i,grid,player,planner);otherScore[i]=isolated?score(blueprint,"isolated",i,grid,player,planner):0;}
        long best=mainScore[bestMain]+otherScore[bestOther];
        for(int a=isolated?1:0;a<COLORS.length;a++)for(int b=isolated?1:0;b<(isolated?COLORS.length:1);b++) {
            if(isolated && a==b)continue;
            long value=mainScore[a]+otherScore[b];
            if(value>best){best=value;bestMain=a;bestOther=b;}
        }
        return new int[]{bestMain,bestOther};
    }
    public static BlueprintCatalog.Blueprint apply(BlueprintCatalog.Blueprint bp,int main,int isolated) {
        var requirements=Arrays.stream(bp.requirements()).map(r->{var role=originalRole(r.id(),bp.id());if(role==null)return r;
            String name=color(role.equals("main")?main:isolated),id=replace(r.id(),name);
            return new CertusFarm.Requirement(id,r.count(),(name.equals("fluix")?"Fluix":Arrays.stream(name.split("_")).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).reduce((a,b)->a+" "+b).orElse(name))+(id.endsWith("_smart_dense_cable")?" Dense Smart Cable":" Smart Cable"));
        }).toArray(CertusFarm.Requirement[]::new);
        var cells=bp.cells().stream().map(c->{var role=originalRole(c.block(),bp.id());if(role==null)return c;String name=color(role.equals("main")?main:isolated),id=replace(c.block(),name);
            String texture="ae2:textures/part/cable/"+(id.endsWith("_smart_dense_cable")?"dense_smart/":"smart/")+(name.equals("fluix")?"transparent":name)+".png";
            return new BlueprintCatalog.Cell(c.x(),c.y(),c.z(),id,texture,c.cable(),c.parts(),c.inserted());
        }).toList();
        return new BlueprintCatalog.Blueprint(bp.id(),bp.title(),bp.icon(),bp.hint().replace("blue cable","main cable"),requirements,cells);
    }
    public static boolean connect(String a,String b) {
        if(!a.startsWith("ae2:")||!b.startsWith("ae2:"))return false;
        String ca=a.substring(4).replace("_smart_dense_cable","").replace("_smart_cable","");
        String cb=b.substring(4).replace("_smart_dense_cable","").replace("_smart_cable","");
        return ca.equals(cb)||ca.equals("fluix")||cb.equals("fluix");
    }
    private CableChoices() {}
}
