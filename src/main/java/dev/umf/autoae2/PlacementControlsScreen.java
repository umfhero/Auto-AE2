package dev.umf.autoae2;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public class PlacementControlsScreen extends Screen {
    private String feedback;
    private long feedbackUntil;
    public void showFeedback(String message) { feedback=message;feedbackUntil=net.minecraft.util.Util.getMillis()+4000; }
    public PlacementControlsScreen() { super(Component.literal("Placement controls")); }
    private void action(int action) { ClientPacketDistributor.sendToServer(new AdjustPlacement(action)); }
    private void control(String label,String hint,int x,int y,int w,int action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action(action)).bounds(x,y,w,18).tooltip(Tooltip.create(Component.literal(hint))).build());
    }
    @Override protected void init() {
        int x=width/2-79,y=height-113;
        control("<","Move left (Left arrow)",x+5,y+25,20,0);
        control(">","Move right (Right arrow)",x+27,y+25,20,1);
        control("+","Move up (Shift + Up)",x+51,y+25,20,2);
        control("-","Move down (Shift + Down)",x+73,y+25,20,3);
        control("^","Move further (Up arrow)",x+97,y+25,20,4);
        control("v","Move closer (Down arrow)",x+119,y+25,20,5);
        control("","Rotate 90 degrees (R)",x+5,y+47,20,6);
        control("Reset","Reset position and rotation",x+29,y+47,43,7);
        control("Place","Place blueprint",x+76,y+47,48,8);
        addRenderableWidget(Button.builder(Component.literal("X"),b -> onClose()).bounds(x+140,y+3,16,16).tooltip(Tooltip.create(Component.literal("Exit placement controls"))).build());
    }
    @Override public boolean keyPressed(KeyEvent event) {
        boolean shift=event.hasShiftDown();
        int action=switch(event.key()) {
            case GLFW.GLFW_KEY_LEFT -> 0; case GLFW.GLFW_KEY_RIGHT -> 1;
            case GLFW.GLFW_KEY_UP -> shift?2:4; case GLFW.GLFW_KEY_DOWN -> shift?3:5;
            case GLFW.GLFW_KEY_R -> 6; default -> -1;
        };
        if(action>=0){action(action);return true;}
        return super.keyPressed(event);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta) { }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        int x=width/2-79,y=height-113;
        g.fill(x,y,x+158,y+70,0xaa323b50); g.fill(x+2,y+2,x+156,y+68,0xabc6c9d2);
        g.text(font,"PLACEMENT",x+6,y+8,0xff343b55,false);
        super.extractRenderState(g,mx,my,delta);
        // Draw a compact rotation arrow using pixels so it works with every Minecraft font.
        int c=0xffeeeeee;
        g.fill(x+11,y+52,x+18,y+53,c); g.fill(x+10,y+53,x+11,y+58,c);
        g.fill(x+11,y+58,x+17,y+59,c); g.fill(x+17,y+56,x+18,y+58,c);
        g.fill(x+16,y+52,x+19,y+54,c); g.fill(x+18,y+53,x+20,y+54,c);
        if(feedback!=null && net.minecraft.util.Util.getMillis()<feedbackUntil) {
            var lines=font.split(Component.literal(feedback),Math.max(80,Math.min(width-32,250)));
            int boxWidth=0;for(var line:lines)boxWidth=Math.max(boxWidth,font.width(line));
            int top=Math.max(8,Math.min(height/2-lines.size()*5,y-lines.size()*10-12));
            g.fill(width/2-boxWidth/2-6,top-5,width/2+(boxWidth+1)/2+6,top+lines.size()*10+3,0xd9232939);
            for(int i=0;i<lines.size();i++)g.text(font,lines.get(i),width/2-font.width(lines.get(i))/2,top+i*10,0xffffc5c5,true);
        }
    }
}
