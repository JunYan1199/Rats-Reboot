package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.server.brick.CounterNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.List;
import java.util.UUID;

public final class BrickQueueScreen extends Screen {
    private final BrickOrderScreen menu;
    private CounterNetwork.QueueState state;
    private UUID selected;
    private Button cancel;
    private int ticks, offset, visible, x, y, w, h;

    BrickQueueScreen(BrickOrderScreen menu, CounterNetwork.QueueState initial) {
        super(label("order_list"));
        this.menu=menu;
        this.state=initial;
    }
    private static Component label(String key){return Component.translatable("brick.rats."+key);}
    private List<CounterNetwork.Entry> orders(){return state.orders();}
    void update(CounterNetwork.QueueState next){
        if(!state.counter().equals(next.counter()))return;
        menu.accept(next);
        state=next;
        if(orders().stream().noneMatch(o->o.ticket().equals(selected)))selected=null;
        scroll(0);
        enableCancel();
    }
    private void enableCancel(){
        if(cancel!=null)cancel.active=orders().stream().anyMatch(o->o.mine()&&o.ticket().equals(selected));
    }
    private void scroll(int delta){offset=Mth.clamp(offset+delta,0,Math.max(0,orders().size()-visible));}
    private void refresh(){CounterNetwork.send(state.counter(),3,BrickOrderScreen.EMPTY);}
    @Override protected void init(){
        w=Math.min(326,width-16);h=Math.min(242,height-12);x=(width-w)/2;y=(height-h)/2;
        visible=Math.max(1,(h-85)/23);
        addRenderableWidget(Button.builder(label("back_to_menu"),b->onClose()).bounds(x+8,y+h-24,85,18).build());
        cancel=addRenderableWidget(Button.builder(label("cancel_selected"),b->{
            if(selected!=null){CounterNetwork.cancel(state.counter(),selected);selected=null;enableCancel();}
        }).bounds(x+w-96,y+h-24,88,18).build());
        addRenderableWidget(Button.builder(Component.literal("▲"),b->scroll(-visible)).bounds(x+w-25,y+32,17,18).build());
        addRenderableWidget(Button.builder(Component.literal("▼"),b->scroll(visible)).bounds(x+w-25,y+28+visible*23-18,17,18).build());
        scroll(0);enableCancel();refresh();
    }
    @Override public void tick(){if(++ticks%30==0)refresh();}
    @Override public void onClose(){minecraft.setScreen(menu);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean mouseScrolled(double mx,double my,double delta){scroll(delta<0?1:-1);return true;}
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(super.mouseClicked(mx,my,button))return true;
        if(button==0&&mx>=x+8&&mx<x+w-29&&my>=y+28&&my<y+28+23*visible){
            int index=offset+(int)((my-y-28)/23);
            if(index<orders().size()){selected=orders().get(index).ticket();enableCancel();}
            return true;
        }
        return false;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(x,y,x+w,y+h,0xff25282b);
        g.drawString(font,title,x+8,y+10,0xffefce92);
        for(int i=0;i<visible && offset+i<orders().size();i++){
            int n=offset+i,yy=y+28+i*23;
            var o=orders().get(n);
            g.fill(x+8,yy,x+w-30,yy+22,o.ticket().equals(selected)?0xff536c61:0xff33393b);
            if(!o.food().isEmpty())g.renderItem(o.food(),x+11,yy+3);
            String name=o.food().isEmpty()?o.recipe().toString():o.food().getHoverName().getString();
            g.drawString(font,font.plainSubstrByWidth((n+1)+". "+name,w-120),x+33,yy+7,0xfffaf4df);
            var owner=label(o.mine()?"mine":"other");g.drawString(font,owner,x+w-36-font.width(owner),yy+7,0xffbecfc3);
        }
        if(orders().isEmpty())g.drawString(font,label("no_orders"),x+10,y+36,0xffe9d6ad);
        else {
            var status=state.status().getString().isEmpty()?label("first"):state.status();
            var first=orders().get(0);
            String dish=first.food().isEmpty()?first.recipe().toString():first.food().getHoverName().getString();
            g.drawString(font,font.plainSubstrByWidth(status.getString()+" · "+dish,w-16),x+8,y+h-50,0xffbad6c5);
        }
        g.drawString(font,label("select_order"),x+8,y+h-38,0xffdfd3b8);
        super.render(g,mx,my,partial);
    }
}