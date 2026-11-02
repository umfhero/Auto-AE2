package dev.umf.autoae2;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import static org.junit.jupiter.api.Assertions.*;

class FarmLayoutTest {
    @Test void everyRotationKeepsBudClearAndAllCellsDistinct() {
        for(int turn=0;turn<4;turn++) {
            var positions=new HashSet<BlockPos>();
            for(var part:FarmLayout.PARTS) {
                var p=part.at(BlockPos.ZERO,turn);
                assertNotEquals(BlockPos.ZERO,p,"must preserve existing budding quartz");
                assertTrue(positions.add(p),"duplicate footprint cell: "+p);
            }
            assertEquals(14,positions.size());
        }
    }
    @Test void countsMatchGuideConstruction() {
        assertEquals(7,FarmLayout.PARTS.stream().filter(p->p.kind()==FarmLayout.Kind.BLUE_CABLE).count());
        assertEquals(2,FarmLayout.PARTS.stream().filter(p->p.kind()==FarmLayout.Kind.ORANGE_CABLE).count());
        assertEquals(4,FarmLayout.PARTS.stream().filter(p->p.kind()==FarmLayout.Kind.ACCELERATOR).count());
        assertEquals(1,FarmLayout.PARTS.stream().filter(p->p.kind()==FarmLayout.Kind.BARREL).count());
        assertEquals(18,CertusFarm.TOTAL);
        assertEquals(CertusFarm.TOTAL,java.util.Arrays.stream(CertusFarm.REQUIREMENTS).mapToInt(CertusFarm.Requirement::count).sum());
    }
    @Test void rotationsMoveDirectionsAndOffsetsTogether() {
        for(var d:Direction.Plane.HORIZONTAL) {
            var point=new BlockPos(d.getStepX(),0,d.getStepZ());
            for(int turns=0;turns<4;turns++) {
                var rotated=FarmLayout.rotate(point.getX(),point.getY(),point.getZ(),turns);
                var face=FarmLayout.rotate(d,turns);
                assertEquals(new BlockPos(face.getStepX(),0,face.getStepZ()),rotated);
            }
        }
    }
}
