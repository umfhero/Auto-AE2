package dev.umf.autoae2;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Only designs with a complete, placeable implementation belong in this catalogue. */
public final class BlueprintCatalog {
    public record Attachment(String id,Direction side) {}
    public record Cell(int x,int y,int z,String block,String texture,boolean cable,List<Attachment> parts,String inserted) {
        public BlockPos at(BlockPos origin,int rotation) { return origin.offset(FarmLayout.rotate(x,y,z,rotation)); }
    }
    public record Blueprint(String id,String title,String icon,String hint,CertusFarm.Requirement[] requirements,List<Cell> cells) {
        public int total() { int n=0;for(var r:requirements)n+=r.count();return n; }
    }
    private static CertusFarm.Requirement r(String id,int count,String label) { return new CertusFarm.Requirement(id,count,label); }
    private static Cell c(int x,int y,int z,String block,String tex) { return new Cell(x,y,z,"ae2:"+block,"ae2:textures/block/"+tex+".png",false,List.of(),null); }
    private static Cell wire(int x,int y,int z,String color,Attachment...parts) { return new Cell(x,y,z,"ae2:"+color+"_smart_cable","ae2:textures/part/cable/smart/"+color+".png",true,List.of(parts),null); }
    private static Cell inscriber(int x,String press) { return new Cell(x,0,0,"ae2:inscriber","ae2:textures/block/inscriber.png",false,List.of(),press==null?null:"ae2:"+press); }
    private static Cell denseWire(int x) { return new Cell(x,2,0,"ae2:blue_smart_dense_cable","ae2:textures/part/cable/dense_smart/blue.png",true,List.of(),null); }
    private static Cell denseAt(int x,int y,int z) { return new Cell(x,y,z,"ae2:blue_smart_dense_cable","ae2:textures/part/cable/dense_smart/blue.png",true,List.of(),null); }
    private static Attachment part(String id,Direction side) { return new Attachment("ae2:"+id,side); }
    public static final List<Blueprint> ALL=List.of(
        new Blueprint("simple_certus_farm","Simple Certus Farm","ae2:certus_quartz_crystal","Aim at existing budding quartz",CertusFarm.REQUIREMENTS,List.of()),
        new Blueprint("controller","Controller Array","ae2:controller","Power and connect to one ME network",new CertusFarm.Requirement[]{r("ae2:controller",1,"ME Controller")},List.of(c(0,0,0,"controller","controller"))),
        new Blueprint("controller_cube","Controller Cube","ae2:controller","Power and connect to one ME network",new CertusFarm.Requirement[]{r("ae2:controller",1,"ME Controller")},List.of(c(0,0,0,"controller","controller"))),
        new Blueprint("assembler_module","Assembler Module","ae2:molecular_assembler","Add encoded crafting patterns and a CPU",new CertusFarm.Requirement[]{r("ae2:pattern_provider",1,"Pattern Provider"),r("ae2:molecular_assembler",1,"Molecular Assembler")},List.of(c(0,0,0,"pattern_provider","pattern_provider"),c(1,0,0,"molecular_assembler","molecular_assembler"))),
        new Blueprint("crafting_cpu","Crafting CPU","ae2:crafting_unit","Connect to ME network; keep CPUs separate",new CertusFarm.Requirement[]{r("ae2:1k_crafting_storage",1,"1k Crafting Storage"),r("ae2:crafting_unit",1,"Crafting Unit")},List.of(c(0,0,0,"1k_crafting_storage","crafting/1k_storage"),c(1,0,0,"crafting_unit","crafting/unit"))),
        new Blueprint("charger","Charger Automation","ae2:charged_certus_quartz_crystal","Connect blue cable; add processing pattern",new CertusFarm.Requirement[]{r("ae2:charger",1,"Charger"),r("ae2:pattern_provider",1,"Pattern Provider"),r("ae2:blue_smart_cable",3,"Blue Smart Cable"),r("ae2:green_smart_cable",2,"Green Smart Cable"),r("ae2:import_bus",1,"Import Bus"),r("ae2:storage_bus",1,"Storage Bus"),r("ae2:quartz_fiber",1,"Quartz Fiber")},List.of(
            c(0,0,0,"charger","charger"),c(1,0,0,"pattern_provider","pattern_provider"),wire(2,0,0,"blue"),wire(3,0,0,"blue"),
            wire(0,1,0,"green",part("import_bus",Direction.DOWN)),wire(1,1,0,"green",part("storage_bus",Direction.DOWN)),wire(2,1,0,"blue",part("quartz_fiber",Direction.WEST))
        )),
        new Blueprint("processor","Processor Production","ae2:logic_processor","Connect dense cable to the main ME network",new CertusFarm.Requirement[0],List.of()),
        new Blueprint("furnace","Furnace Automation","minecraft:furnace","Connect dense cable; add patterns and coal",new CertusFarm.Requirement[0],List.of())
    );
    public static Blueprint byId(String id) { return ALL.stream().filter(b->b.id().equals(id)).findFirst().orElse(null); }
    public static int maxLevel(String id) { return switch(id) { case "controller" -> 127; case "controller_cube" -> 8; case "assembler_module" -> 10; case "crafting_cpu" -> 5; case "processor" -> 5; case "furnace" -> 8; default -> 1; }; }
    public static int clampLevel(String id,int level) { return Math.clamp(level,1,maxLevel(id)); }
    private static final java.util.Map<String,Blueprint> VARIANTS=new java.util.concurrent.ConcurrentHashMap<>();
    public static Blueprint variant(String id,int level) {
        var base=byId(id);if(base==null)return null;
        int size=clampLevel(id,level);
        if(id.equals("processor"))return processorVariant(new int[]{size>=1?1:0,size>=2?1:0,size>=3?1:0,size>=4?1:0});
        if(maxLevel(id)==1)return base;
        return VARIANTS.computeIfAbsent(id+":"+size,key->makeVariant(base,size));
    }
    private static Blueprint makeVariant(Blueprint base,int level) {
        String id=base.id();
        var cells=new ArrayList<Cell>();
        switch(id) {
            case "controller", "controller_cube" -> {
                var shape=new HashSet<BlockPos>();
                if(id.equals("controller")) {
                    for(int x=0;x<=6;x+=2)for(int z=0;z<=6;z+=2)for(int y=0;y<=6;y++)shape.add(new BlockPos(x,y,z));
                    var path=new ArrayList<BlockPos>();
                    for(int z=0;z<=6;z+=2)for(int i=0;i<4;i++)path.add(new BlockPos((z/2%2==0?i:3-i)*2,0,z));
                    for(int i=1;i<path.size();i++) {var a=path.get(i-1);var b=path.get(i);shape.add(new BlockPos((a.getX()+b.getX())/2,0,(a.getZ()+b.getZ())/2));}
                } else {
                    int side=level-1;
                    if(level<=3) {
                        for(int y=0;y<level;y++)cells.add(c(0,y,0,"controller","controller"));
                    } else {
                        for(int x=0;x<side;x++)for(int y=0;y<side;y++)for(int z=0;z<side;z++) {
                            int edges=(x==0||x==side-1?1:0)+(y==0||y==side-1?1:0)+(z==0||z==side-1?1:0);
                            if(edges>=2)cells.add(c(x,y,z,"controller","controller"));
                        }
                    }
                }
                if(id.equals("controller")) {
                    var todo=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();todo.add(BlockPos.ZERO);seen.add(BlockPos.ZERO);
                    while(!todo.isEmpty() && cells.size()<level) {
                        var p=todo.remove();cells.add(c(p.getX(),p.getY(),p.getZ(),"controller","controller"));
                        for(var d:Direction.values()) {var next=p.relative(d);if(shape.contains(next)&&seen.add(next))todo.add(next);}
                    }
                }
            }
            case "assembler_module" -> {
                if(level==1) { cells.add(c(0,0,0,"pattern_provider","pattern_provider"));cells.add(c(1,0,0,"molecular_assembler","molecular_assembler")); }
                else for(int x=0;x<level-1;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++) {
                    boolean provider=(x+y+z)%2==0;cells.add(c(x,y,z,provider?"pattern_provider":"molecular_assembler",provider?"pattern_provider":"molecular_assembler"));
                }
            }
            case "crafting_cpu" -> {
                int[] dimensions={1,2,4,8,27};int count=dimensions[level-1];int width=level<=2?count:level==5?3:2;int depth=level<=2?1:level==3?2:width;
                String[] capacity={"1k","1k","4k","16k","256k"};String tier=capacity[level-1];
                cells.add(c(0,0,0,tier+"_crafting_storage","crafting/"+tier+"_storage"));
                for(int i=1;i<count;i++)cells.add(c(i%width,i/(width*depth),(i/width)%depth,"crafting_accelerator","crafting/accelerator"));
            }
            case "processor" -> {
                int[] presses={level>=1?1:0,level>=2?1:0,level>=3?1:0,level>=4?1:0};
                return processorVariant(presses);
            }
            case "furnace" -> {
                Direction[] faces={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
                int groups=(level+3)/4;
                for(int group=0;group<groups;group++) {
                    int centre=group*6,count=Math.min(4,level-group*4);
                    cells.add(c(centre,1,0,"pattern_provider","pattern_provider"));
                    var exports=new Attachment[count];
                    for(int j=0;j<count;j++)exports[j]=part("export_bus",faces[j]);
                    cells.add(wire(centre,0,0,"blue",exports));
                    cells.add(denseAt(centre,-1,0));
                    for(int j=0;j<count;j++) {
                        var dir=faces[j];int x=centre+dir.getStepX(),z=dir.getStepZ();
                        cells.add(new Cell(x,1,z,"minecraft:hopper","minecraft:textures/block/hopper_outside.png",false,List.of(),null));
                        cells.add(new Cell(x,0,z,"minecraft:furnace","minecraft:textures/block/furnace_side.png",false,List.of(),null));
                        cells.add(wire(x,-1,z,"blue",part("import_bus",Direction.UP)));
                    }
                }
                for(int x=0;x<=(groups-1)*6;x++)cells.add(denseAt(x,-2,0));
            }
            default -> { return base; }
        }
        var costs=new LinkedHashMap<String,Integer>();for(var cell:cells) {
            costs.merge(cell.block(),1,Integer::sum);for(var part:cell.parts())costs.merge(part.id(),1,Integer::sum);
            if(cell.inserted()!=null)costs.merge(cell.inserted(),1,Integer::sum);
        }
        var requirements=costs.entrySet().stream().map(e->r(e.getKey(),e.getValue(),switch(e.getKey()) {
            case "ae2:controller" -> "ME Controller";case "ae2:pattern_provider" -> "Pattern Provider";case "ae2:molecular_assembler" -> "Molecular Assembler";
            case "ae2:crafting_accelerator" -> "Co-processor";case "ae2:blue_smart_cable" -> "Blue Smart Cable";case "ae2:blue_smart_dense_cable" -> "Blue Dense Cable";case "ae2:import_bus" -> "Import Bus";case "ae2:export_bus" -> "Export Bus";case "minecraft:furnace" -> "Furnace";default -> e.getKey().substring(4).replace('_',' ');
        })).toArray(CertusFarm.Requirement[]::new);
        return new Blueprint(base.id(),base.title(),base.icon(),base.hint(),requirements,List.copyOf(cells));
    }
    public static final String[] PRESS_IDS={"ae2:logic_processor_press","ae2:silicon_press","ae2:calculation_processor_press","ae2:engineering_processor_press"};
    public static final String[] PRESS_LABELS={"Logic","Silicon","Calculation","Engineering"};
    public static Blueprint processorVariant(int[] requested) {
        if(requested.length!=4)throw new IllegalArgumentException("four press counts required");
        int[] counts=requested.clone();for(int i=0;i<4;i++)counts[i]=Math.clamp(counts[i],0,8);
        String key="processor_counts:"+java.util.Arrays.toString(counts);
        return VARIANTS.computeIfAbsent(key,ignored->makeProcessor(counts));
    }
    private static Blueprint makeProcessor(int[] counts) {
        var presses=new ArrayList<String>();
        for(int i=0;i<4;i++)for(int n=0;n<counts[i];n++)presses.add(PRESS_IDS[i]);
        var cells=new ArrayList<Cell>();Direction[] faces={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.UP};
        int groups=(presses.size()+4)/5;
        for(int group=0;group<groups;group++) {
            int x=group*5;cells.add(c(x,0,0,"pattern_provider","pattern_provider"));cells.add(wire(x,-1,0,"blue"));
            for(int j=0;j<5 && group*5+j<presses.size();j++) {
                var d=faces[j];cells.add(new Cell(x+d.getStepX(),d.getStepY(),d.getStepZ(),"ae2:inscriber","ae2:textures/block/inscriber.png",false,List.of(),presses.get(group*5+j)));
            }
        }
        int finalX=groups*5;cells.add(c(finalX,0,0,"pattern_provider","pattern_provider"));cells.add(wire(finalX,-1,0,"blue"));
        cells.add(new Cell(finalX+1,0,0,"ae2:inscriber","ae2:textures/block/inscriber.png",false,List.of(),null));
        for(int x=0;x<=finalX;x++)cells.add(denseAt(x,-2,0));
        var costs=new LinkedHashMap<String,Integer>();for(var cell:cells) {
            costs.merge(cell.block(),1,Integer::sum);for(var part:cell.parts())costs.merge(part.id(),1,Integer::sum);
            if(cell.inserted()!=null)costs.merge(cell.inserted(),1,Integer::sum);
        }
        costs.merge("ae2:blank_pattern",ProcessorPatterns.plan(cells).size(),Integer::sum);
        var req=costs.entrySet().stream().map(e->r(e.getKey(),e.getValue(),switch(e.getKey()) {
            case "ae2:blank_pattern" -> "Blank Pattern";case "ae2:pattern_provider" -> "Pattern Provider";case "ae2:inscriber" -> "Inscriber";case "ae2:blue_smart_cable" -> "Blue Smart Cable";case "ae2:blue_smart_dense_cable" -> "Blue Dense Cable";
            default -> e.getKey().substring(4).replace('_',' ');
        })).toArray(CertusFarm.Requirement[]::new);
        var base=byId("processor");return new Blueprint(base.id(),base.title(),base.icon(),base.hint(),req,List.copyOf(cells));
    }
    public static String levelLabel(String id,int level) {
        level=clampLevel(id,level);
        return switch(id) {
            case "controller" -> level+" controllers";
            case "controller_cube" -> level<=3?level+" controllers":(level-1)+"x"+(level-1)+"x"+(level-1)+" frame ("+(12*(level-1)-16)+")";
            case "assembler_module" -> level==1?"Starter pair":"2x2x"+(level-1);
            case "crafting_cpu" -> new String[]{"1k / 1 block","1k / 2 blocks","4k / 4 blocks","16k / 8 blocks","256k / 27 blocks"}[level-1];
            case "processor" -> level==5?"Full processor line":level+" printing station"+(level==1?"":"s");
            case "furnace" -> level+" furnace"+(level==1?"":"s");
            default -> "";
        };
    }
    private BlueprintCatalog() {}
}
