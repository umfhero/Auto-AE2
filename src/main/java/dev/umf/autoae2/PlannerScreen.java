package dev.umf.autoae2;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

/** Catalogue and synced materials view. */
public class PlannerScreen extends AbstractContainerScreen<PlannerMenu> {
    private static final Identifier GHOST=Identifier.fromNamespaceAndPath("autoae2","textures/gui/wrench_ghost.png");
    private static boolean darkTheme;
    private boolean showRequirements,showPressConfig;
    private FarmPreview preview;
    private int previewIndex=-1, previewLevel=-1,previewPressHash,previewCableHash;
    private boolean draggingSlider;
    private int listOffset,requirementsOffset;
    private static final java.util.List<Integer> VISIBLE_BLUEPRINTS=java.util.stream.IntStream.range(0,BlueprintCatalog.ALL.size()).filter(i->!BlueprintCatalog.ALL.get(i).id().equals("controller_cube")).boxed().toList();
    private boolean draggingPreview;
    public PlannerScreen(PlannerMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,290,244);inventoryLabelX=57;inventoryLabelY=148;
    }
    private boolean inside(double mx,double my,int x,int y,int w,int h) { return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h; }
    private static void border(GuiGraphicsExtractor g,int x,int y,int w,int h,int light,int shade,int fill) {
        g.fill(x,y,x+w,y+h,light);g.fill(x+1,y+1,x+w-1,y+h-1,shade);g.fill(x+2,y+2,x+w-2,y+h-2,fill);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my) {
        int ink=darkTheme?0xffe4e8f5:0xff3f425b;
        g.text(font,"Automation Planner",9,7,ink,false);g.text(font,"Inventory",57,148,ink,false);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta) {
        super.extractBackground(g,mx,my,delta);
        int x=leftPos,y=topPos,edge=darkTheme?0xff777f98:0xff575d79,shell=darkTheme?0xff303545:0xffc6c9d2;
        int inner=darkTheme?0xff222736:0xff9299ae,cell=darkTheme?0xff40495d:0xffaab0c2,ink=darkTheme?0xffe4e8f5:0xff3b4058;
        border(g,x,y,imageWidth,imageHeight,0xffe4e5ed,edge,shell);
        g.fill(x+7,y+20,x+165,y+141,edge);g.fill(x+9,y+22,x+163,y+139,inner);g.text(font,"BLUEPRINTS",x+12,y+25,ink,false);
        var selected=menu.blueprint();
        for(int row=0;row<5 && row+listOffset<VISIBLE_BLUEPRINTS.size();row++) {
            int i=VISIBLE_BLUEPRINTS.get(row+listOffset);var entry=BlueprintCatalog.ALL.get(i);int yy=y+37+row*20;
            boolean active=i==menu.selected() || entry.id().equals("controller") && menu.blueprint().id().equals("controller_cube");
            border(g,x+12,yy,140,18,0xffe1e3ec,edge,active?(darkTheme?0xff60768a:0xffd3d9e5):cell);
            var icon=BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.icon()));
            g.item(icon.getDefaultInstance(),x+14,yy+1);
            g.text(font,font.plainSubstrByWidth(entry.id().equals("controller")?"ME Controller":entry.title(),116),x+33,yy+5,ink,false);
        }
        if(VISIBLE_BLUEPRINTS.size()>5) {
            g.fill(x+154,y+37,x+159,y+135,edge);
            int travel=78, max=VISIBLE_BLUEPRINTS.size()-5;
            int thumb=y+38+Math.round(listOffset*(float)travel/max);g.fill(x+155,thumb,x+158,thumb+18,0xffd5dbe8);
        }
        border(g,x+170,y+20,113,68,0xffe7e8ef,edge,inner);
        if(!showRequirements && !showPressConfig) {
            int pressHash=java.util.Arrays.hashCode(menu.pressCounts());
            if(preview==null || previewIndex!=menu.selected() || previewLevel!=menu.level() || previewCableHash!=menu.cableHash() || selected.id().equals("processor") && previewPressHash!=pressHash) { preview=new FarmPreview(selected); previewIndex=menu.selected();previewLevel=menu.level();previewPressHash=pressHash;previewCableHash=menu.cableHash(); }
            preview.render(g,x+174,y+23,105,BlueprintCatalog.maxLevel(selected.id())>1?39:62,darkTheme);
        }
        if(selected.id().equals("processor") || selected.id().equals("furnace"))g.text(font,"i",x+274,y+24,ink,false);
        if(selected.id().equals("processor")) { border(g,x+248,y+22,22,13,0xffe4e7ef,edge,cell);g.text(font,"P",x+256,y+25,ink,false); }
        if(selected.id().equals("controller") || selected.id().equals("controller_cube")) {
            border(g,x+244,y+22,35,13,0xffe4e7ef,edge,cell);
            g.text(font,selected.id().equals("controller_cube")?"Cube":"Array",x+247,y+25,ink,false);
        }
        int max=BlueprintCatalog.maxLevel(selected.id());
        if(selected.id().equals("processor")) {
            g.text(font,menu.pressTotal()+" presses + final",x+175,y+64,ink,false);
            g.text(font,"P: configure presses",x+175,y+76,ink,false);
        } else if(max>1) {
            int sx=x+176,sy=y+76;g.fill(sx,sy,sx+102,sy+3,edge);
            int knob=sx+Math.round((menu.level()-1)*98f/(max-1));g.fill(knob,sy-2,knob+5,sy+5,0xffd8ebf2);
            String label=BlueprintCatalog.levelLabel(selected.id(),menu.level());g.text(font,label,x+175,y+64,ink,false);
        }
        border(g,x+228,y+92,54,19,0xffe8eaf0,edge,menu.isLinked()||menu.hasWrench()?(darkTheme?0xff527989:0xffc6dae0):(darkTheme?0xff443e4b:0xffb9aeb8));
        g.text(font,menu.isLinked()?"Linked":"Link",x+(menu.isLinked()?239:244),y+97,ink,false);
        border(g,x+257,y+115,22,22,0xffe8eaf0,edge,cell);
        if(!menu.hasWrench())g.blit(RenderPipelines.GUI_TEXTURED,GHOST,x+260,y+118,0,0,16,16,16,16);
        g.text(font,menu.totalAvailable()+"/"+menu.blueprint().total(),x+172,y+97,ink,false);
        int bookBg=menu.available()?0xff548e6d:0xffaa5961;
        border(g,x+173,y+115,22,22,0xffe8eaf0,edge,bookBg);g.item(Items.BOOK.getDefaultInstance(),x+176,y+118);
        border(g,x+199,y+115,54,22,0xffe8eaf0,edge,cell);g.text(font,"Craft",x+211,y+122,ink,false);
        String status=switch(menu.craftStatus()) {case 1 -> "ME offline";case 2 -> "All items available";case 3 -> "No craftable items";default -> "";};
        g.text(font,status,x+172,y+139,ink,false);
        for(int row=0;row<4;row++)for(int col=0;col<9;col++) {int sy=row==3?218:159+row*18,sx=56+col*18;border(g,x+sx,y+sy,18,18,0xffe4e7ef,edge,cell);}
        border(g,x+260,y+3,23,15,0xffececf4,edge,cell);g.text(font,darkTheme?"L":"D",x+268,y+7,ink,false);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        super.extractRenderState(g,mx,my,delta);
        int x=leftPos,y=topPos;
        if(showPressConfig) {
            int edge=darkTheme?0xff777f98:0xff575d79,shell=darkTheme?0xff303545:0xffe0e2e9,ink=darkTheme?0xffe4e8f5:0xff3b4058,cell=darkTheme?0xff40495d:0xffaab0c2;
            border(g,x+38,y+20,214,124,0xfff0f1f5,edge,shell);
            g.text(font,"CONFIGURE PRESSES",x+45,y+25,ink,false);g.text(font,"X",x+237,y+25,ink,false);
            for(int i=0;i<4;i++) {
                int yy=y+40+i*19;var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(BlueprintCatalog.PRESS_IDS[i]));
                g.item(item.getDefaultInstance(),x+45,yy-3);g.text(font,BlueprintCatalog.PRESS_LABELS[i],x+63,yy+1,ink,false);
                border(g,x+161,yy-2,16,14,0xffe8eaf0,edge,cell);g.text(font,"-",x+166,yy+1,ink,false);
                g.text(font,Integer.toString(menu.pressCount(i)),x+183,yy+1,ink,false);
                border(g,x+198,yy-2,16,14,0xffe8eaf0,edge,cell);g.text(font,"+",x+203,yy+1,ink,false);
                g.text(font,"/"+menu.pressAvailable(i),x+218,yy+1,ink,false);
            }
            g.text(font,"Selected / in ME storage",x+45,y+126,ink,false);
        } else if(showRequirements) {
            int edge=darkTheme?0xff777f98:0xff575d79,shell=darkTheme?0xff303545:0xffe0e2e9,ink=darkTheme?0xffe4e8f5:0xff3b4058;
            border(g,x+38,y+20,214,124,0xfff0f1f5,edge,shell);
            g.text(font,"REQUIREMENTS",x+45,y+25,ink,false);g.text(font,"X",x+237,y+25,ink,false);
            for(int row=0;row<7 && row+requirementsOffset<menu.blueprint().requirements().length;row++) {
                int i=row+requirementsOffset;var req=menu.blueprint().requirements()[i];int yy=y+37+row*12;
                g.pose().pushMatrix();g.pose().translate(x+45,yy-1);g.pose().scale(.625f,.625f);g.item(req.item().getDefaultInstance(),0,0);g.pose().popMatrix();
                String count=menu.quantity(i)+"/"+req.count();int countX=x+245-font.width(count);
                int markerX=countX-8;g.text(font,font.plainSubstrByWidth(req.label(),Math.max(0,markerX-(x+60))),x+58,yy,ink,false);
                if(menu.isCraftable(i))g.text(font,"+",markerX,yy,0xff388452,false);
                g.text(font,count,countX,yy,menu.quantity(i)>=req.count()?0xff388452:0xffb2454c,false);
            }
            if(menu.blueprint().requirements().length>7)g.text(font,"Scroll",x+197,y+25,0xff69748a,false);
            var hintLines=font.split(Component.literal(menu.blueprint().hint()),196);
            for(int line=0;line<Math.min(2,hintLines.size());line++)g.text(font,hintLines.get(line),x+45,y+123+line*9,0xffa86440,false);
            var ingredient=requirementAt(mx,my);
            if(!ingredient.isEmpty()) {
                int index=(my-(y+37))/12+requirementsOffset;
                g.setComponentTooltipForNextFrame(font,java.util.List.of(ingredient.getHoverName(),Component.literal(menu.isCraftable(index)?"ME pattern available":"No ME pattern available"),Component.literal("JEI: R recipe, U uses, A bookmark")),mx,my);
            }
            else if(inside(mx,my,43,122,205,20))g.setTooltipForNextFrame(font,Component.literal(menu.blueprint().hint()),mx,my);
        } else if(inside(mx,my,12,37,140,100)) {
            int row=(my-(y+37))/20+listOffset; if(row>=0 && row<VISIBLE_BLUEPRINTS.size())g.setTooltipForNextFrame(font,Component.literal(BlueprintCatalog.ALL.get(VISIBLE_BLUEPRINTS.get(row)).hint()),mx,my);
        } else if((menu.blueprint().id().equals("controller") || menu.blueprint().id().equals("controller_cube")) && inside(mx,my,244,22,35,13)) {
            g.setTooltipForNextFrame(font,Component.literal("Toggle controller array / cube"),mx,my);
        } else if(menu.blueprint().id().equals("processor") && inside(mx,my,268,21,14,14)) {
            g.setComponentTooltipForNextFrame(font,java.util.List.of(
                Component.literal("Processor line: connect the dense cable"),
                Component.literal("Presses are installed during placement."),
                Component.literal("Patterns and presses are installed during placement."),
                Component.literal("Gold -> printed logic; silicon -> printed silicon"),
                Component.literal("Certus -> printed calculation; diamond -> printed engineering"),
                Component.literal("Final station: printed circuit + printed silicon"),
                Component.literal("+ redstone -> corresponding processor"),
                Component.literal("Connect the dense cable to your ME network.")),mx,my);
        } else if(menu.blueprint().id().equals("furnace") && inside(mx,my,268,21,14,14)) {
            g.setComponentTooltipForNextFrame(font,java.util.List.of(
                Component.literal("Furnaces: connect the dense cable"),
                Component.literal("Put coal in ME storage for the fuel bus."),
                Component.literal("Add smelting processing patterns to each provider."),
                Component.literal("Input goes in the top; output returns to ME storage.")),mx,my);
        } else if(menu.blueprint().id().equals("processor") && inside(mx,my,248,22,22,13)) {
            g.setTooltipForNextFrame(font,Component.literal("Configure how many of each press to install"),mx,my);
        } else if(!draggingPreview && inside(mx,my,174,23,105,40)) {
            g.setTooltipForNextFrame(font,Component.literal("Drag to rotate the preview"),mx,my);
        } else if(inside(mx,my,228,92,54,19)) {
            g.setTooltipForNextFrame(font,Component.literal(menu.isLinked()?"Linked wrench follows this planner selection":"Insert a Placement Wrench to link"),mx,my);
        } else if(inside(mx,my,173,115,22,22)) {
            g.setTooltipForNextFrame(font,Component.literal(menu.totalAvailable()+" of "+menu.blueprint().total()+" items available - click for details"),mx,my);
        } else if(inside(mx,my,199,115,54,22)) {
            g.setComponentTooltipForNextFrame(font,java.util.List.of(Component.literal("Review craftable missing items in AE2"),Component.literal("Items without an ME pattern are skipped.")),mx,my);
        } else if(inside(mx,my,257,115,22,22)&&!menu.hasWrench()) {
            g.setTooltipForNextFrame(font,Component.literal("Insert a Placement Wrench"),mx,my);
        } else if(inside(mx,my,260,3,23,15)) {
            g.setTooltipForNextFrame(font,Component.literal("Toggle light / dark theme"),mx,my);
        }
    }
    /** The visible item icon and row are exposed to recipe viewers such as JEI. */
    public net.minecraft.world.item.ItemStack requirementAt(double mx,double my) {
        if(!showRequirements || !inside(mx,my,44,36,174,84))return net.minecraft.world.item.ItemStack.EMPTY;
        int row=(int)(my-(topPos+37))/12,index=row+requirementsOffset;
        if(row<0 || row>=7 || index>=menu.blueprint().requirements().length)return net.minecraft.world.item.ItemStack.EMPTY;
        return menu.blueprint().requirements()[index].item().getDefaultInstance();
    }
    public net.minecraft.client.renderer.Rect2i requirementArea(double mx,double my) {
        int row=(int)(my-(topPos+37))/12;
        return new net.minecraft.client.renderer.Rect2i(leftPos+44,topPos+36+row*12,174,11);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(showPressConfig) {
            if(event.button()==0) {
                if(inside(event.x(),event.y(),229,20,23,18)){showPressConfig=false;return true;}
                for(int i=0;i<4;i++) {
                    int yy=40+i*19;
                    int action=inside(event.x(),event.y(),161,yy-2,16,14)?500+i*2:inside(event.x(),event.y(),198,yy-2,16,14)?501+i*2:-1;
                    if(action>=0 && minecraft!=null && minecraft.gameMode!=null){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);return true;}
                }
            }
            return true;
        }
        if(showRequirements) { if(event.button()==0 && inside(event.x(),event.y(),229,20,23,18)) showRequirements=false;return true; }
        if(event.button()==0) {
            double mx=event.x(),my=event.y();
            if(menu.blueprint().id().equals("processor") && inside(mx,my,248,22,22,13)){showPressConfig=true;return true;}
            if((menu.blueprint().id().equals("controller") || menu.blueprint().id().equals("controller_cube")) && inside(mx,my,244,22,35,13)) {
                String next=menu.blueprint().id().equals("controller")?"controller_cube":"controller";
                int i=java.util.stream.IntStream.range(0,BlueprintCatalog.ALL.size()).filter(n->BlueprintCatalog.ALL.get(n).id().equals(next)).findFirst().orElse(1);
                if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+i);return true;
            }
            if(!menu.blueprint().id().equals("processor") && BlueprintCatalog.maxLevel(menu.blueprint().id())>1 && inside(mx,my,174,72,106,14)){draggingSlider=true;updateSlider(mx);return true;}
            if(inside(mx,my,174,23,105,39)){draggingPreview=true;return true;}
            if(inside(mx,my,260,3,23,15)){darkTheme=!darkTheme;return true;}
            if(inside(mx,my,173,115,22,22)){showRequirements=true;requirementsOffset=0;return true;}
            if(inside(mx,my,199,115,54,22)){if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true;}
            if(inside(mx,my,228,92,54,19)) { if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);return true; }
            if(inside(mx,my,153,37,8,98) && VISIBLE_BLUEPRINTS.size()>5) {
                listOffset=(int)Math.round(Math.clamp((my-(topPos+37))/80d,0d,1d)*(VISIBLE_BLUEPRINTS.size()-5));return true;
            }
            if(inside(mx,my,12,37,140,100)) {
                int row=(int)(my-(topPos+37))/20+listOffset;
                if(row>=0 && row<VISIBLE_BLUEPRINTS.size() && minecraft!=null && minecraft.gameMode!=null) {
                    int i=VISIBLE_BLUEPRINTS.get(row);
                    if(BlueprintCatalog.ALL.get(i).id().equals("controller") && menu.blueprint().id().equals("controller_cube"))i=menu.selected();
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+i);
                }
                return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double scrollX,double scrollY) {
        if(showRequirements && inside(mx,my,38,20,214,124)) {
            requirementsOffset=Math.clamp(requirementsOffset-(int)Math.signum(scrollY),0,Math.max(0,menu.blueprint().requirements().length-7));return true;
        }
        if(!showRequirements && inside(mx,my,7,20,158,121)) {
            listOffset=Math.clamp(listOffset-(int)Math.signum(scrollY),0,Math.max(0,VISIBLE_BLUEPRINTS.size()-5));return true;
        }
        return super.mouseScrolled(mx,my,scrollX,scrollY);
    }
    private void updateSlider(double mouseX) {
        int max=BlueprintCatalog.maxLevel(menu.blueprint().id());
        int value=1+(int)Math.round(Math.clamp((mouseX-(leftPos+176))/98d,0d,1d)*(max-1));
        if(minecraft!=null && minecraft.gameMode!=null && value!=menu.level())minecraft.gameMode.handleInventoryButtonClick(menu.containerId,199+value);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(draggingSlider){updateSlider(event.x());return true;}
        if(draggingPreview && preview!=null){preview.drag(dx,dy);return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(draggingSlider){draggingSlider=false;return true;}
        if(draggingPreview){draggingPreview=false;return true;}
        return super.mouseReleased(event);
    }
}
