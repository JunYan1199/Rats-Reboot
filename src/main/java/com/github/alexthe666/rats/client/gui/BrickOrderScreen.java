package com.github.alexthe666.rats.client.gui;
import com.github.alexthe666.rats.server.brick.CounterNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.*;

public final class BrickOrderScreen extends Screen {
    private CounterNetwork.Catalog data;
    private CounterNetwork.QueueState queue;
    private EditBox search;
    private List<CounterNetwork.Row> rows=List.of();
    private int x,y,w,h,offset,visible;
    private boolean fresh;
    static final ResourceLocation EMPTY=new ResourceLocation("rats","order_counter");
    public BrickOrderScreen(CounterNetwork.Catalog data){super(Component.translatable("block.rats.order_counter"));this.data=data;this.queue=new CounterNetwork.QueueState(data.counter(),List.of(),data.status());}
    public static void show(CounterNetwork.Catalog next){
        var mc=Minecraft.getInstance();
        if(mc.screen instanceof BrickOrderScreen s&&s.data.counter().equals(next.counter())){s.data=next;s.filter();}
        else mc.setScreen(new BrickOrderScreen(next));
    }
    public static void update(CounterNetwork.QueueState next){
        var screen=Minecraft.getInstance().screen;
        if(screen instanceof BrickQueueScreen q)q.update(next);
        if(screen instanceof BrickOrderScreen m)m.accept(next);
    }
    void accept(CounterNetwork.QueueState next){
        if(!data.counter().equals(next.counter()))return;
        queue=next;
        var head=next.orders().isEmpty()?Component.empty():next.orders().get(0).food().getHoverName();
        data=new CounterNetwork.Catalog(data.counter(),data.rows(),data.gourmet(),next.orders().size(),head,next.status());
    }
    private Component text(String key,Object... args){return Component.translatable("brick.rats."+key,args);}
    @Override protected void init(){
        w=Math.min(326,width-16);h=Math.min(242,height-12);x=(width-w)/2;y=(height-h)/2;visible=Math.max(1,(h-112)/20);
        search=new EditBox(font,x+8,y+25,w-(data.gourmet()?117:16),18,text("search"));
        search.setHint(text("search"));search.setResponder(s->{offset=0;filter();});addRenderableWidget(search);
        if(data.gourmet())addRenderableWidget(Button.builder(text(fresh?"uneaten":"all"),b->{fresh=!fresh;offset=0;filter();b.setMessage(text(fresh?"uneaten":"all"));}).bounds(x+w-102,y+24,94,20).build());
        addRenderableWidget(Button.builder(Component.literal("▲"),b->scroll(-visible)).bounds(x+w-25,y+49,17,18).build());
        addRenderableWidget(Button.builder(Component.literal("▼"),b->scroll(visible)).bounds(x+w-25,y+48+20*visible-18,17,18).build());
        addRenderableWidget(Button.builder(text("refresh"),b->CounterNetwork.send(data.counter(),0,EMPTY)).bounds(x+w-121,y+h-24,48,18).build());
        addRenderableWidget(Button.builder(text("orders"),b->minecraft.setScreen(new BrickQueueScreen(this,queue))).bounds(x+w-71,y+h-24,63,18).build());
        filter();setInitialFocus(search);
    }
    private void filter(){
        String q=search==null?"":search.getValue().strip().toLowerCase(Locale.ROOT);
        rows=data.rows().stream().filter(r->(!fresh||!r.eaten())&&(r.output().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)||r.recipe().toString().contains(q)))
            .sorted(Comparator.comparing((CounterNetwork.Row r)->r.output().getHoverName().getString()).thenComparing(r->r.recipe().toString())).toList();
        scroll(0);
    }
    private void scroll(int n){offset=Mth.clamp(offset+n,0,Math.max(0,rows.size()-visible));}
    @Override public boolean mouseScrolled(double mx,double my,double delta){scroll(delta<0?1:-1);return true;}
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(super.mouseClicked(mx,my,button))return true;
        if(button==0&&mx>=x+8&&mx<x+w-30&&my>=y+48&&my<y+48+20*visible){
            int i=offset+(int)((my-y-48)/20);if(i<rows.size())CounterNetwork.send(data.counter(),1,rows.get(i).recipe());
            return true;
        }
        return false;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(x,y,x+w,y+h,0xff25282b);g.drawString(font,title,x+8,y+8,0xffefce92);
        for(int i=0;i<visible&&offset+i<rows.size();i++){
            var r=rows.get(offset+i);int yy=y+48+20*i;boolean hover=mx>=x+8&&mx<x+w-31&&my>=yy&&my<yy+19;
            g.fill(x+7,yy,x+w-30,yy+19,hover?0xff505e61:0xff33393b);
            g.renderItem(r.output(),x+10,yy+1);g.renderItemDecorations(font,r.output(),x+10,yy+1);
            g.drawString(font,font.plainSubstrByWidth(r.output().getHoverName().getString(),w-75),x+32,yy+5,0xfffaf4df);
        }
        if(rows.isEmpty())g.drawString(font,text(data.rows().isEmpty()?"empty_stations":"empty_search"),x+9,y+53,0xffd9cdb6);
        g.drawString(font,font.plainSubstrByWidth(text("queue",data.count(),data.next()).getString(),w-15),x+8,y+h-48,0xffe9d6ad);
        g.drawString(font,font.plainSubstrByWidth(data.status().getString(),w-15),x+8,y+h-36,0xffbad6c5);
        g.drawString(font,(rows.isEmpty()?0:offset+1)+" / "+rows.size(),x+8,y+h-19,0xffbec3c4);
        super.render(g,mx,my,partial);
        if(mx>=x+8&&mx<x+w-30&&my>=y+48&&my<y+48+20*visible){
            int i=offset+(my-y-48)/20;if(i<rows.size())g.renderTooltip(font,rows.get(i).output(),mx,my);
        }
    }
    @Override public boolean isPauseScreen(){return false;}
}
