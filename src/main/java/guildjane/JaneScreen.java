package guildjane;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

class JaneScreen extends Screen {
    final GuildWire.Page page;
    static ResourceLocation art(String key){return new ResourceLocation(GuildJane.ID,"textures/gui/"+key+".png");}
    static final ResourceLocation FRAME=art("ledger"),SEAL=art("seal"),NORMAL=art("normal"),HOVER=art("hover");
    boolean sent;int panelWidth,left,top,boxHeight;
    JaneScreen(GuildWire.Page page){super(Component.literal(page.title()));this.page=page;}
    static net.minecraft.client.resources.sounds.SoundInstance speech;
    static void show(GuildWire.Page page){
        var mc=Minecraft.getInstance();
        if(!page.voice().isEmpty()){
            var sound=mc.getSoundManager();
            if(speech!=null)sound.stop(speech);
            speech=new net.minecraft.client.resources.sounds.SimpleSoundInstance(new ResourceLocation(GuildJane.ID,"voice."+page.voice()),net.minecraft.sounds.SoundSource.VOICE,1,1,net.minecraft.util.RandomSource.create(),false,0,net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,0,0,0,true);
            sound.play(speech);
        }
        mc.setScreen(page.layout().isEmpty()?new JaneScreen(page):new BountyScreen(page));
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){
        if(!sent&&minecraft!=null&&minecraft.getConnection()!=null)GuildWire.NET.sendToServer(new GuildWire.Pick(page.token(),-1));
        super.removed();
    }
    static void paint(GuiGraphics g,ResourceLocation tex,int x,int y,int w,int h,int tw,int th){
        g.blit(tex,x,y,w,h,0,0,tw,th,tw,th);
    }
    class Choice extends Button {
        Choice(int x,int y,int w,Component label,OnPress action){super(x,y,w,21,label,action,DEFAULT_NARRATION);}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
            boolean lit=isHoveredOrFocused();paint(g,lit?HOVER:NORMAL,getX(),getY(),width,height,520,69);
            g.drawCenteredString(font,getMessage(),getX()+width/2,getY()+(height-8)/2,lit?0xffffe0a2:0xfff1e7d5);
        }
    }
    @Override protected void init(){
        panelWidth=Math.min(448,width-14);left=(width-panelWidth)/2;
        int textHeight=font.wordWrapHeight(page.line(),panelWidth-42);
        int rows=(page.labels().size()+2)/2;
        boxHeight=56+textHeight+rows*24;top=Math.max(2,height-boxHeight-8);
        for(int i=0;i<=page.labels().size();i++){
            int index=i;boolean close=i==page.labels().size();
            addRenderableWidget(new Choice(left+16+(i%2)*(panelWidth-29)/2,top+44+textHeight+(i/2)*24,(panelWidth-35)/2,Component.literal(close?"告辞":page.labels().get(i)),b->{
                if(close){onClose();return;}
                if(!sent){sent=true;GuildWire.NET.sendToServer(new GuildWire.Pick(page.token(),index));}
            }));
        }
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        RenderSystem.enableBlend();
        paint(g,FRAME,left,top,panelWidth,boxHeight,1280,420);
        paint(g,SEAL,left+14,top+4,23,23,128,128);
        g.drawString(font,"公会接待员 · Jane Doe",left+42,top+10,0xffffdfa2,true);
        g.drawString(font,page.title(),left+panelWidth-17-font.width(page.title()),top+10,0xffedcea0,true);
        g.drawWordWrap(font,Component.literal(page.line()),left+20,top+31,panelWidth-42,0xfff8eedc);
        super.render(g,mx,my,dt);
    }
}
