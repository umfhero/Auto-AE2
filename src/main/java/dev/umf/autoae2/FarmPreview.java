package dev.umf.autoae2;

import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import guideme.color.LightDarkMode;
import guideme.document.LytSize;
import guideme.internal.scene.ScenePictureInPictureRenderer;
import guideme.scene.CameraSettings;
import guideme.scene.GuidebookLevelRenderer;
import guideme.scene.GuidebookScene;
import guideme.scene.level.GuidebookLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix3x2f;

import java.util.Collections;

/** An isolated, interactive GuideME scene; it never touches the player's world. */
final class FarmPreview {
    private final GuidebookScene scene;
    FarmPreview(BlueprintCatalog.Blueprint blueprint) {
        var level=new GuidebookLevel() {
            @Override public net.neoforged.neoforge.model.data.ModelData getModelData(BlockPos pos) {
                var data=super.getModelData(pos);
                var state=data.get(appeng.block.networking.CableBusRenderState.PROPERTY);
                if(state==null || state.getCableType()==null)return data;
                if(!blueprint.id().equals("simple_certus_farm")) {
                    var cell=blueprint.cells().stream().filter(p->p.at(BlockPos.ZERO,0).equals(pos)).findFirst().orElse(null);
                    if(cell==null)return data;
                    for(var direction:Direction.values()) {
                        var neighbour=blueprint.cells().stream().filter(p->p.at(BlockPos.ZERO,0).equals(pos.relative(direction))).findFirst().orElse(null);
                        if(neighbour!=null && neighbour.cable() && CableChoices.connect(neighbour.block(),cell.block())) {
                            state.getConnectionTypes().put(direction,state.getCableType());state.getCableBusAdjacent().add(direction);
                        }
                    }
                    return data;
                }
                var current=FarmLayout.PARTS.stream().filter(p->p.at(BlockPos.ZERO,0).equals(pos)).findFirst().orElse(null);
                if(current==null)return data;
                for(var direction:Direction.values()) {
                    var next=pos.relative(direction);
                    var neighbour=FarmLayout.PARTS.stream().filter(p->p.at(BlockPos.ZERO,0).equals(next)).findFirst().orElse(null);
                    boolean joins=neighbour!=null && (neighbour.kind()==current.kind() || current.kind()==FarmLayout.Kind.BLUE_CABLE && neighbour.kind()==FarmLayout.Kind.ACCELERATOR);
                    boolean fiber=(pos.equals(new BlockPos(-1,1,-1)) && direction==Direction.DOWN) || (pos.equals(new BlockPos(-1,0,-1)) && direction==Direction.UP);
                    if(joins || fiber) {
                        state.getConnectionTypes().put(direction,state.getCableType());
                        if(neighbour!=null && neighbour.kind()!=FarmLayout.Kind.ACCELERATOR)state.getCableBusAdjacent().add(direction);
                    }
                }
                return data;
            }
        };
        var anchor=BlockPos.ZERO;
        if(blueprint.id().equals("simple_certus_farm")) {
        level.setBlockAndUpdate(anchor,block("ae2:flawless_budding_quartz").defaultBlockState());
        for(var part:FarmLayout.PARTS) {
            var pos=part.at(anchor,0);
            switch(part.kind()) {
                case BLUE_CABLE -> add(level,pos,null,blueprint.requirements()[0].id());
                case ORANGE_CABLE -> add(level,pos,null,blueprint.requirements()[1].id());
                case ACCELERATOR -> level.setBlockAndUpdate(pos,block("ae2:growth_accelerator").defaultBlockState().setValue(BlockStateProperties.FACING,part.facing()));
                case BARREL -> level.setBlockAndUpdate(pos,Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP));
            }
        }
        add(level,new BlockPos(-2,1,-1),Direction.SOUTH,"ae2:storage_bus");
        add(level,new BlockPos(-1,1,0),Direction.WEST,"ae2:storage_bus");
        add(level,new BlockPos(-1,1,0),Direction.EAST,"ae2:annihilation_plane");
        add(level,new BlockPos(-1,1,-1),Direction.DOWN,"ae2:quartz_fiber");
        } else {
            for(var cell:blueprint.cells()) {
                var pos=cell.at(anchor,0);
                if(cell.cable()) add(level,pos,null,cell.block());
                else level.setBlockAndUpdate(pos,block(cell.block()).defaultBlockState());
            }
            for(var cell:blueprint.cells())for(var part:cell.parts())add(level,cell.at(anchor,0),part.side(),part.id());
            for(var cell:blueprint.cells())if(cell.inserted()!=null && level.getBlockEntity(cell.at(anchor,0)) instanceof appeng.blockentity.misc.InscriberBlockEntity inscriber)
                inscriber.getInternalInventory().setItemDirect(0,BuiltInRegistries.ITEM.getValue(Identifier.parse(cell.inserted())).getDefaultInstance());
        }
        var camera=new CameraSettings();camera.setZoom(1f);
        scene=new GuidebookScene(level,camera);
        camera.setRotationCenter(scene.getWorldCenter());
        scene.centerScene();
        var bounds=scene.getScreenBounds();
        float width=Math.max(1,bounds.z-bounds.x),height=Math.max(1,bounds.w-bounds.y);
        camera.setZoom(Math.min(1.7f,Math.min(94f/width,(BlueprintCatalog.maxLevel(blueprint.id())>1?35f:52f)/height)));
        scene.centerScene();
    }
    private static net.minecraft.world.level.block.Block block(String id) { return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)); }
    private static void add(GuidebookLevel level,BlockPos pos,Direction side,String id) {
        var host=PartHelper.getOrPlacePartHost(level,pos,true,null);
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        if(host==null || !(item instanceof IPartItem<?> part) || host.addPart(part,side,null)==null)
            throw new IllegalStateException("Could not create preview part "+id);
        host.markForUpdate();
    }
    void drag(double dx,double dy) {
        var camera=scene.getCameraSettings();camera.setRotationY(camera.getRotationY()+(float)dx);
        camera.setRotationX(Math.clamp(camera.getRotationX()+(float)dy,-90,90));
    }
    void render(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean dark) {
        var camera=scene.getCameraSettings();camera.setViewportSize(new LytSize(w,h));
        var bounds=new ScreenRectangle(x,y,w,h).transformMaxBounds(g.pose());
        var scissor=g.peekScissorStack();if(scissor!=null)bounds=bounds.intersection(scissor);
        if(bounds==null)return;
        g.submitPictureInPictureRenderState(new ScenePictureInPictureRenderer.State(
            dark?LightDarkMode.DARK_MODE:LightDarkMode.LIGHT_MODE,new Matrix3x2f(g.pose()),x,y,x+w,y+h,
            null,bounds,scissor,(mode,pose,buffers)->GuidebookLevelRenderer.getInstance().render(scene.getLevel(),camera,buffers,Collections.emptyList(),mode)));
    }
}
