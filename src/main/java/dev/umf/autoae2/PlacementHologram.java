package dev.umf.autoae2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** A translucent, client-only preview; placement is always validated on the server. */
@EventBusSubscriber(modid=AutoAE2.MOD_ID,value=Dist.CLIENT)
public final class PlacementHologram {
    private static final ContextKey<Ghost> KEY=new ContextKey<>(Identifier.fromNamespaceAndPath(AutoAE2.MOD_ID,"placement_ghost"));
    private static final Identifier SIDE=Identifier.withDefaultNamespace("textures/block/oak_log.png");
    private static final Identifier END=Identifier.withDefaultNamespace("textures/block/oak_log_top.png");
    private static final Identifier ACCEL=Identifier.fromNamespaceAndPath("ae2","textures/block/growth_accelerator_side.png");
    private static final Identifier BARREL=Identifier.withDefaultNamespace("textures/block/barrel_side.png");
    private static final Identifier BLUE=Identifier.fromNamespaceAndPath("ae2","textures/part/cable/smart/blue.png");
    private static final Identifier ORANGE=Identifier.fromNamespaceAndPath("ae2","textures/part/cable/smart/orange.png");
    private static final Identifier BUS=Identifier.fromNamespaceAndPath("ae2","textures/part/storage_bus.png");
    private static final Identifier PLANE=Identifier.fromNamespaceAndPath("ae2","textures/part/annihilation_plane.png");
    private static final Identifier FIBER=Identifier.fromNamespaceAndPath("ae2","textures/part/quartz_fiber.png");
    private record Ghost(BlockPos pos, boolean valid, int rotation, boolean farm, BlueprintCatalog.Blueprint blueprint) {}
    @SubscribeEvent public static void extract(ExtractLevelRenderStateEvent event) {
        var mc=Minecraft.getInstance(); var player=mc.player;
        if(player==null || (mc.screen!=null && !(mc.screen instanceof PlacementControlsScreen))) return;
        ItemStack held=player.getMainHandItem();
        if(!held.is(AutoAE2.WRENCH.get()) || !PlacementWrench.loaded(held)) return;
        BlockPos pos=PlacementWrench.target(player,held);
        if(pos==null || !event.getLevel().hasChunkAt(pos)) return;
        boolean farm=PlacementWrench.isFarm(held);int rotation=PlacementWrench.rotation(held);
        boolean valid=farm ? FarmBuilder.isBudding(event.getLevel().getBlockState(pos)) : event.getLevel().getBlockState(pos).canBeReplaced();
        var blueprint=PlacementWrench.blueprint(held);
        if(blueprint!=null && !farm) for(var cell:blueprint.cells()) {
            var location=cell.at(pos,rotation);
            if(!event.getLevel().hasChunkAt(location)||!event.getLevel().getBlockState(location).isAir())valid=false;
        }
        if(farm) for(var part:FarmLayout.PARTS) {
            BlockPos location=part.at(pos,rotation);
            if(!event.getLevel().hasChunkAt(location)||!event.getLevel().getBlockState(location).isAir()) valid=false;
        }
        event.getRenderState().setRenderData(KEY,new Ghost(pos,valid,rotation,farm,blueprint));
    }
    @SubscribeEvent public static void submit(SubmitCustomGeometryEvent event) {
        Ghost ghost=event.getLevelRenderState().getRenderData(KEY);
        if(ghost==null) return;
        Vec3 camera=event.getLevelRenderState().cameraRenderState.pos;
        int tint=ghost.valid?0x997feaf3:0x99ff6464;
        if(ghost.farm) {
            for(var part:FarmLayout.PARTS) {
                BlockPos pos=part.at(ghost.pos,ghost.rotation);
                Identifier tex=switch(part.kind()) { case BLUE_CABLE -> cableTexture(ghost.blueprint.requirements()[0].id()); case ORANGE_CABLE -> cableTexture(ghost.blueprint.requirements()[1].id()); case ACCELERATOR -> ACCEL; case BARREL -> BARREL; };
                boolean cable=part.kind()==FarmLayout.Kind.BLUE_CABLE||part.kind()==FarmLayout.Kind.ORANGE_CABLE;
                if(cable) {
                    box(event,camera,pos,tex,tint,ghost.rotation,.3125f,.3125f,.3125f,.6875f,.6875f,.6875f);
                    for(var direction:net.minecraft.core.Direction.values()) {
                        boolean joins=FarmLayout.PARTS.stream().anyMatch(other -> other.x()==part.x()+direction.getStepX() && other.y()==part.y()+direction.getStepY() && other.z()==part.z()+direction.getStepZ() && (other.kind()==part.kind() || part.kind()==FarmLayout.Kind.BLUE_CABLE && other.kind()==FarmLayout.Kind.ACCELERATOR));
                        if(joins) arm(event,camera,pos,tex,tint,ghost.rotation,direction);
                    }
                    if(part.x()==-2 && part.y()==1 && part.z()==-1) box(event,camera,pos,BUS,tint,ghost.rotation,.2f,.2f,.68f,.8f,.8f,1f);
                    if(part.x()==-1 && part.y()==1 && part.z()==0) {
                        box(event,camera,pos,BUS,tint,ghost.rotation,0f,.2f,.2f,.32f,.8f,.8f);
                        box(event,camera,pos,PLANE,tint,ghost.rotation,.68f,.08f,.08f,1f,.92f,.92f);
                    }
                    if(part.x()==-1 && part.y()==1 && part.z()==-1) box(event,camera,pos,FIBER,tint,ghost.rotation,.28f,0f,.28f,.72f,.32f,.72f);
                } else renderCube(event,camera,pos,tex,tex,tint,false,ghost.rotation);
            }
        } else {
            var blueprint=ghost.blueprint;
            if(blueprint==null)renderCube(event,camera,ghost.pos,SIDE,END,tint,false,ghost.rotation);
            else for(var cell:blueprint.cells()) {
                var texture=Identifier.parse(cell.texture());var pos=cell.at(ghost.pos,ghost.rotation);
                if(!cell.cable())renderCube(event,camera,pos,texture,texture,tint,false,ghost.rotation);
                else {
                    box(event,camera,pos,texture,tint,ghost.rotation,.3125f,.3125f,.3125f,.6875f,.6875f,.6875f);
                    for(var direction:net.minecraft.core.Direction.values()) {
                        boolean joins=blueprint.cells().stream().anyMatch(other->other.cable() && CableChoices.connect(other.block(),cell.block()) && other.x()==cell.x()+direction.getStepX() && other.y()==cell.y()+direction.getStepY() && other.z()==cell.z()+direction.getStepZ());
                        if(joins)arm(event,camera,pos,texture,tint,ghost.rotation,direction);
                    }
                    for(var part:cell.parts()) {
                        var id=Identifier.parse(part.id());var tex=Identifier.fromNamespaceAndPath("ae2","textures/part/"+id.getPath()+".png");
                        arm(event,camera,pos,tex,tint,ghost.rotation,part.side());
                    }
                }
            }
        }
    }
    private static Identifier cableTexture(String id) { String color=id.substring(4).replace("_smart_cable","");return Identifier.fromNamespaceAndPath("ae2","textures/part/cable/smart/"+(color.equals("fluix")?"transparent":color)+".png"); }
    private static void arm(SubmitCustomGeometryEvent event,Vec3 camera,BlockPos pos,Identifier tex,int tint,int rotation,net.minecraft.core.Direction dir) {
        float lo=.3125f,hi=.6875f;
        switch(dir) {
            case WEST -> box(event,camera,pos,tex,tint,rotation,0,lo,lo,hi,hi,hi);
            case EAST -> box(event,camera,pos,tex,tint,rotation,lo,lo,lo,1,hi,hi);
            case DOWN -> box(event,camera,pos,tex,tint,rotation,lo,0,lo,hi,hi,hi);
            case UP -> box(event,camera,pos,tex,tint,rotation,lo,lo,lo,hi,1,hi);
            case NORTH -> box(event,camera,pos,tex,tint,rotation,lo,lo,0,hi,hi,hi);
            case SOUTH -> box(event,camera,pos,tex,tint,rotation,lo,lo,lo,hi,hi,1);
        }
    }
    private static void box(SubmitCustomGeometryEvent event,Vec3 camera,BlockPos pos,Identifier tex,int tint,int rotation,float x1,float y1,float z1,float x2,float y2,float z2) {
        PoseStack pose=event.getPoseStack();pose.pushPose();
        pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
        pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(-rotation*90f));pose.translate(-.5,0,-.5);
        pose.translate(x1,y1,z1);pose.scale(x2-x1,y2-y1,z2-z1);
        event.getSubmitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucent(tex,false),(p,v)->{sides(p,v,tint);ends(p,v,tint);});
        pose.popPose();
    }
    private static void renderCube(SubmitCustomGeometryEvent event,Vec3 camera,BlockPos pos,Identifier sidesTexture,Identifier topTexture,int tint,boolean small,int rotation) {
        PoseStack pose=event.getPoseStack();pose.pushPose();
        pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
        pose.translate(0.5,0,0.5);pose.mulPose(Axis.YP.rotationDegrees(rotation*90f));pose.translate(-0.5,0,-0.5);
        if(small) {pose.translate(0.3125,0.3125,0.3125);pose.scale(0.375f,0.375f,0.375f);}
        event.getSubmitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucent(sidesTexture,false),(p,v)->sides(p,v,tint));
        event.getSubmitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucent(topTexture,false),(p,v)->ends(p,v,tint));
        pose.popPose();
    }
    private static void vertex(PoseStack.Pose p,VertexConsumer v,float x,float y,float z,float u,float w,int color,float nx,float ny,float nz) {
        v.addVertex(p,x,y,z).setColor(color).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p,nx,ny,nz);
    }
    private static void quad(PoseStack.Pose p,VertexConsumer v,int color,float nx,float ny,float nz,float... a) {
        for(int i=0;i<4;i++) vertex(p,v,a[i*3],a[i*3+1],a[i*3+2],(i==0||i==3)?0:1,(i<2)?1:0,color,nx,ny,nz);
    }
    private static void sides(PoseStack.Pose p,VertexConsumer v,int c) {
        quad(p,v,c,0,0,-1, 1,0,0, 0,0,0, 0,1,0, 1,1,0);
        quad(p,v,c,0,0,1, 0,0,1, 1,0,1, 1,1,1, 0,1,1);
        quad(p,v,c,-1,0,0, 0,0,0, 0,0,1, 0,1,1, 0,1,0);
        quad(p,v,c,1,0,0, 1,0,1, 1,0,0, 1,1,0, 1,1,1);
    }
    private static void ends(PoseStack.Pose p,VertexConsumer v,int c) {
        quad(p,v,c,0,1,0, 0,1,1, 1,1,1, 1,1,0, 0,1,0);
        quad(p,v,c,0,-1,0, 0,0,0, 1,0,0, 1,0,1, 0,0,1);
    }
}
