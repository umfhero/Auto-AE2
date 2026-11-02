package dev.umf.autoae2;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import net.minecraft.core.BlockPos;
import static org.junit.jupiter.api.Assertions.*;

class BlueprintCatalogTest {
    @Test void everyControllerSizeIsConnectedAndObeysControllerRules() {
        for(String id:java.util.List.of("controller","controller_cube"))for(int count=1;count<=BlueprintCatalog.maxLevel(id);count++) {
            var blueprint=BlueprintCatalog.variant(id,count);
            var positions=blueprint.cells().stream().map(c->c.at(BlockPos.ZERO,0)).collect(java.util.stream.Collectors.toSet());
            int expected=id.equals("controller_cube")?(count<=3?count:12*(count-1)-16):count;
            assertEquals(expected,positions.size());assertEquals(expected,blueprint.total());
            var seen=new HashSet<BlockPos>();var queue=new java.util.ArrayDeque<BlockPos>();queue.add(BlockPos.ZERO);seen.add(BlockPos.ZERO);
            while(!queue.isEmpty()) {var p=queue.remove();for(var d:net.minecraft.core.Direction.values()){var next=p.relative(d);if(positions.contains(next)&&seen.add(next))queue.add(next);}}
            assertEquals(positions.size(),seen.size());
            for(var p:positions) {
                int axes=0;for(var axis:net.minecraft.core.Direction.Axis.values()) {
                    var a=net.minecraft.core.Direction.get(net.minecraft.core.Direction.AxisDirection.POSITIVE,axis);
                    if(positions.contains(p.relative(a))&&positions.contains(p.relative(a.getOpposite())))axes++;
                }
                assertTrue(axes<=1,"controller crossing at "+p+" for size "+count);
                assertTrue(p.getX()<7 && p.getY()<7 && p.getZ()<7);
            }
        }
    }
    @Test void allCpuPresetsAreFilledRectanglesWithStorage() {
        for(int level=1;level<=5;level++) {
            var bp=BlueprintCatalog.variant("crafting_cpu",level);
            int maxX=bp.cells().stream().mapToInt(BlueprintCatalog.Cell::x).max().orElseThrow();
            int maxY=bp.cells().stream().mapToInt(BlueprintCatalog.Cell::y).max().orElseThrow();
            int maxZ=bp.cells().stream().mapToInt(BlueprintCatalog.Cell::z).max().orElseThrow();
            assertEquals((maxX+1)*(maxY+1)*(maxZ+1),bp.cells().size());
            assertEquals(1,bp.cells().stream().filter(c->c.block().contains("crafting_storage")).count());
        }
    }
    @Test void assemblerPresetsKeepProvidersAdjacentToAssemblers() {
        for(int level=1;level<=10;level++) {
            var bp=BlueprintCatalog.variant("assembler_module",level);
            assertEquals(level==1?2:4*(level-1),bp.cells().size());
            var cells=bp.cells();
            for(var cell:cells) {
                var p=cell.at(BlockPos.ZERO,0);
                assertTrue(cells.stream().anyMatch(other->!other.block().equals(cell.block()) && other.at(BlockPos.ZERO,0).distManhattan(p)==1));
            }
            assertEquals(bp.cells().size(),bp.total());
        }
    }
    @Test void processorPresetsIncludePressesAndSeparateReturnBuses() {
        for(int perType=0;perType<=8;perType++) {
            var counts=new int[]{perType,perType,perType,perType};var bp=BlueprintCatalog.processorVariant(counts);int total=perType*4;
            assertEquals(total+1,bp.cells().stream().filter(c->c.block().equals("ae2:inscriber")).count());
            assertEquals((total+4)/5+1,bp.cells().stream().filter(c->c.block().equals("ae2:pattern_provider")).count());
            assertEquals(total,bp.cells().stream().filter(c->c.inserted()!=null).count());
            for(var cell:bp.cells())if(cell.block().equals("ae2:inscriber"))
                assertEquals(1,bp.cells().stream().filter(p->p.block().equals("ae2:pattern_provider") && p.at(BlockPos.ZERO,0).distManhattan(cell.at(BlockPos.ZERO,0))==1).count());
        }
    }
    @Test void processorPatternsMatchEachProviderAndDeduplicatePresses() {
        var bp=BlueprintCatalog.processorVariant(new int[]{4,2,1,1});
        var patterns=ProcessorPatterns.plan(bp.cells());
        var byProvider=patterns.stream().collect(java.util.stream.Collectors.groupingBy(ProcessorPatterns.Spec::provider));
        assertEquals(3,byProvider.size());
        for(var group:byProvider.entrySet()) {
            var adjacent=bp.cells().stream().filter(c->c.block().equals("ae2:inscriber") && c.at(BlockPos.ZERO,0).distManhattan(group.getKey())==1).toList();
            assertTrue(group.getValue().size()<=9);
            if(adjacent.stream().anyMatch(c->c.inserted()==null)) {
                assertEquals(3,group.getValue().size());
                assertTrue(group.getValue().stream().noneMatch(ProcessorPatterns.Spec::printing));
            } else {
                assertEquals(adjacent.stream().map(BlueprintCatalog.Cell::inserted).distinct().count(),group.getValue().size());
                for(var pattern:group.getValue())assertTrue(adjacent.stream().anyMatch(c->c.inserted().equals(pattern.top())));
            }
        }
        assertEquals(patterns.size(),java.util.Arrays.stream(bp.requirements()).filter(r->r.id().equals("ae2:blank_pattern")).findFirst().orElseThrow().count());
    }
    @Test void cubeStagesAreHollowFrames() {
        int[] sizes={1,2,3,20,32,44,56,68};
        for(int stage=1;stage<=8;stage++) {
            var cells=BlueprintCatalog.variant("controller_cube",stage).cells();assertEquals(sizes[stage-1],cells.size());
            if(stage>=4) {int side=stage-1;for(var cell:cells) {
                int edges=(cell.x()==0||cell.x()==side-1?1:0)+(cell.y()==0||cell.y()==side-1?1:0)+(cell.z()==0||cell.z()==side-1?1:0);
                assertTrue(edges>=2);
            }}
        }
    }
    @Test void furnacePresetsFeedFromAboveAndReturnFromBelow() {
        for(int level=1;level<=8;level++) {
            var bp=BlueprintCatalog.variant("furnace",level);
            var cells=bp.cells().stream().collect(java.util.stream.Collectors.toMap(c->c.at(BlockPos.ZERO,0),java.util.function.Function.identity()));
            assertEquals(level,cells.values().stream().filter(c->c.block().equals("minecraft:furnace")).count());
            for(var entry:cells.entrySet())if(entry.getValue().block().equals("minecraft:furnace")) {
                assertEquals("minecraft:hopper",cells.get(entry.getKey().above()).block());
                assertTrue(cells.get(entry.getKey().below()).parts().stream().anyMatch(part->part.id().equals("ae2:import_bus")&&part.side()==net.minecraft.core.Direction.UP));
            }
            assertEquals(level,cells.values().stream().flatMap(c->c.parts().stream()).filter(p->p.id().equals("ae2:export_bus")).count());
        }
    }
    @Test void recoloringPreservesCostAndNetworkRoles() {
        for(String id:java.util.List.of("simple_certus_farm","charger","processor","furnace")) {
            var base=BlueprintCatalog.variant(id,BlueprintCatalog.maxLevel(id));
            var recolored=CableChoices.apply(base,7,14);
            assertEquals(base.total(),recolored.total());
            assertTrue(java.util.Arrays.stream(recolored.requirements()).anyMatch(r->r.id().equals("ae2:pink_smart_cable")));
            if(id.equals("charger")||id.equals("simple_certus_farm"))
                assertTrue(java.util.Arrays.stream(recolored.requirements()).anyMatch(r->r.id().equals("ae2:green_smart_cable")));
            if(id.equals("processor")||id.equals("furnace"))
                assertTrue(java.util.Arrays.stream(recolored.requirements()).anyMatch(r->r.id().equals("ae2:pink_smart_dense_cable")));
            assertTrue(recolored.cells().stream().filter(BlueprintCatalog.Cell::cable).allMatch(c->java.util.Arrays.stream(recolored.requirements()).anyMatch(r->r.id().equals(c.block()))));
        }
        assertTrue(CableChoices.connect("ae2:pink_smart_cable","ae2:pink_smart_dense_cable"));
        assertFalse(CableChoices.connect("ae2:pink_smart_cable","ae2:green_smart_cable"));
        assertTrue(CableChoices.connect("ae2:fluix_smart_cable","ae2:green_smart_cable"));
    }
    @Test void idsCostsAndRotatedFootprintsStayConsistent() {
        var ids=new HashSet<String>();
        for(var base:BlueprintCatalog.ALL) {
            assertTrue(ids.add(base.id()));
            assertEquals(base,BlueprintCatalog.byId(base.id()));
            var blueprint=BlueprintCatalog.variant(base.id(),BlueprintCatalog.maxLevel(base.id()));
            assertTrue(blueprint.requirements().length<=12);
            assertTrue(blueprint.total()>0);
            if(blueprint.id().equals("simple_certus_farm"))continue;
            assertEquals(blueprint.total(),blueprint.cells().size()+blueprint.cells().stream().mapToInt(cell->cell.parts().size()+(cell.inserted()==null?0:1)).sum()+(blueprint.id().equals("processor")?ProcessorPatterns.plan(blueprint.cells()).size():0));
            for(int turns=0;turns<4;turns++) {
                var cells=new HashSet<BlockPos>();
                for(var cell:blueprint.cells())assertTrue(cells.add(cell.at(BlockPos.ZERO,turns)));
                assertEquals(blueprint.cells().size(),cells.size());
            }
            for(var cell:blueprint.cells()) {
                assertTrue(java.util.Arrays.stream(blueprint.requirements()).anyMatch(req->req.id().equals(cell.block())));
                for(var part:cell.parts())assertTrue(java.util.Arrays.stream(blueprint.requirements()).anyMatch(req->req.id().equals(part.id())));
                if(cell.inserted()!=null)assertTrue(java.util.Arrays.stream(blueprint.requirements()).anyMatch(req->req.id().equals(cell.inserted())));
            }
            for(var req:blueprint.requirements()) {
                long count=blueprint.cells().stream().filter(c->c.block().equals(req.id())).count()
                    +blueprint.cells().stream().flatMap(c->c.parts().stream()).filter(p->p.id().equals(req.id())).count()
                    +blueprint.cells().stream().filter(c->req.id().equals(c.inserted())).count()
                    +(blueprint.id().equals("processor") && req.id().equals("ae2:blank_pattern")?ProcessorPatterns.plan(blueprint.cells()).size():0);
                assertEquals(req.count(),count,"Incorrect material cost for "+req.id());
            }
        }
    }
}
