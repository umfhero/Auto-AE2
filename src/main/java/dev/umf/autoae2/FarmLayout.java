package dev.umf.autoae2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.List;

/** Relative to the budding quartz; derived from AE2's published simple_certus_farm.snbt. */
public final class FarmLayout {
    public enum Kind { BLUE_CABLE, ORANGE_CABLE, ACCELERATOR, BARREL }
    public record Part(int x,int y,int z,Kind kind,Direction facing) {
        public BlockPos at(BlockPos anchor,int turns) { BlockPos p=rotate(x,y,z,turns);return anchor.offset(p); }
    }
    public static final List<Part> PARTS=List.of(
        new Part(-3,0,-1,Kind.BLUE_CABLE,null),new Part(-2,0,-1,Kind.BLUE_CABLE,null),new Part(-1,0,-1,Kind.BLUE_CABLE,null),
        new Part(-1,0,1,Kind.BLUE_CABLE,null),new Part(1,0,-1,Kind.BLUE_CABLE,null),new Part(1,0,1,Kind.BLUE_CABLE,null),
        new Part(-2,1,-1,Kind.BLUE_CABLE,null),new Part(-1,1,-1,Kind.ORANGE_CABLE,null),new Part(-1,1,0,Kind.ORANGE_CABLE,null),
        new Part(-1,0,0,Kind.ACCELERATOR,Direction.NORTH),new Part(0,0,-1,Kind.ACCELERATOR,Direction.EAST),
        new Part(0,0,1,Kind.ACCELERATOR,Direction.WEST),new Part(1,0,0,Kind.ACCELERATOR,Direction.SOUTH),
        new Part(-2,1,0,Kind.BARREL,null)
    );
    public static BlockPos rotate(int x,int y,int z,int turns) {
        for(int i=0;i<Math.floorMod(turns,4);i++) { int tmp=x;x=-z;z=tmp; }
        return new BlockPos(x,y,z);
    }
    public static Direction rotate(Direction direction,int turns) {
        if(direction.getAxis().isVertical())return direction;
        for(int i=0;i<Math.floorMod(turns,4);i++) direction=direction.getClockWise();return direction;
    }
    private FarmLayout() {}
}
