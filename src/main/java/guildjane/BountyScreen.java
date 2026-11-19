package guildjane;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class BountyScreen extends Screen {
    private static final ResourceLocation MAIL=JaneScreen.art("quest_mail"),MINE=JaneScreen.art("quest_mine"),PAPER=JaneScreen.art("parchment");
    private final GuildWire.Page page;
    private final boolean catalog;
    private final java.util.List<Bounty> cards;
    private final Bounty detail;
    private final Bounty loot;
    private static final String[] ILLUSTRATIONS={"ignis","leviathan","remnant","guardian","monstrosity","harbinger","maledictus","scylla","nightwarden","zaevorath"};
    private static final String[] NAMES={"焰魔","利维坦","远古遗魂","末影守卫","下界合金巨兽","先驱者","咒翼灵骸","斯库拉","夜巡者","Zaevorath"};
    private boolean gallery;
    private int portrait=-1;
    private boolean sent;
    private int x,y,w,h;
    BountyScreen(GuildWire.Page page){super(Component.literal("公会悬赏板"));this.page=page;catalog=page.layout().startsWith("catalog/");
        cards=catalog?java.util.Arrays.stream(page.layout().substring(8).split(",")).filter(k->!k.isEmpty()).map(Bounty::named).toList():java.util.List.of();
        detail=page.layout().startsWith("detail/")?Bounty.named(page.layout().substring(7)):null;
        loot=page.layout().startsWith("loot/")?Bounty.named(page.layout().substring(5)):null;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){
        if(!sent&&minecraft!=null&&minecraft.getConnection()!=null)GuildWire.NET.sendToServer(new GuildWire.Pick(page.token(),-1));
        super.removed();
    }
    @Override protected void init(){
        w=Math.min(450,width-12);h=Math.min(234,height-6);x=(width-w)/2;y=(height-h)/2;
        if(gallery){
            if(portrait<0){
                int cw=(w-24)/5,rh=(h-58)/2;
                for(int i=0;i<NAMES.length;i++){
                    final int index=i;
                    addRenderableWidget(new FoilButton(x+12+i%5*cw,y+30+i/5*rh+rh-18,cw-2,17,NAMES[i],b->{portrait=index;rebuildWidgets();}));
                }
            }
            addRenderableWidget(new FoilButton(x+w-67,y+h-23,52,16,"返回",b->{if(portrait>=0)portrait=-1;else gallery=false;rebuildWidgets();}));
            return;
        }
        if(catalog)addRenderableWidget(new FoilButton(x+w-115,y+7,70,16,"首领图鉴",b->{gallery=true;rebuildWidgets();}));
        int n=page.labels().size();
        for(int i=0;i<n;i++){
            final int ix=i;
            Button.OnPress act=b->{if(!sent){sent=true;GuildWire.NET.sendToServer(new GuildWire.Pick(page.token(),ix));}};
            if(page.layout().startsWith("rats/")){
                String info=page.layout().split("/",-1)[1];String[] rows=info.isEmpty()?new String[0]:info.split(";");
                if(i<rows.length){
                    String[] val=rows[i].split(",");int cw=w-w/3-27,ch=(h-92)/4;
                    var card=new RatButton(x+w/3+12,y+47+i*(ch+2),cw,ch,page.labels().get(i),act,Integer.parseInt(val[0]),val[2].equals("1"));
                    card.active=val[1].equals("1");addRenderableWidget(card);
                }else addRenderableWidget(new FoilButton(x+10+(i-rows.length)*((w-82)/5),y+h-24,(w-82)/5-2,17,page.labels().get(i),act));
            }else if(catalog&&i>=cards.size()){
                addRenderableWidget(new FoilButton(x+15+(i-cards.size())*54,y+h-23,50,16,page.labels().get(i),act));
            }else if(catalog){
                int cw=(w-35)/2;
                addRenderableWidget(new Notice(x+14+i*(cw+7),y+34,cw,h-64,i,page.labels().get(i),act));
            }else{
                int rows=(n+1)/2,bw=(w-37)/2;
                addRenderableWidget(new FoilButton(x+15+(i%2)*(bw+7),y+h-30-rows*22+(i/2)*22,bw,19,page.labels().get(i),act));
            }
        }
        addRenderableWidget(new FoilButton(x+w-65,y+h-23,50,16,"合上",b->onClose()));
    }
    private static void illustration(GuiGraphics g,Bounty quest,int x,int y,int size){
        var art=JaneScreen.art("quest_"+quest.id());
        if(net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(art).isPresent()){
            texture(g,art,x,y,size,size,256);
        }else{
            texture(g,quest.art().equals("mine")?MINE:MAIL,x,y,size,size,512);
        }
    }
    private static void texture(GuiGraphics g,ResourceLocation id,int x,int y,int w,int h,int source){
        g.blit(id,x,y,w,h,0,0,source,source,source,source);
    }
    private void centered(GuiGraphics g,String s,int cx,int yy,int color){g.drawString(font,s,cx-font.width(s)/2,yy,color,false);}
    private static void line(GuiGraphics g,int x,int y,int w,int color){g.fill(x,y,x+w,y+1,color);}
    private class FoilButton extends Button {
        FoilButton(int x,int y,int w,int h,String text,OnPress act){super(x,y,w,h,Component.literal(text),act,DEFAULT_NARRATION);}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
            boolean lit=isHoveredOrFocused();
            JaneScreen.paint(g,lit?JaneScreen.HOVER:JaneScreen.NORMAL,getX(),getY(),width,height,520,69);
            centered(g,font.plainSubstrByWidth(getMessage().getString(),width-8),getX()+width/2,getY()+(height-8)/2,lit?0xffffe1aa:0xfff6e6c6);
        }
    }
    private class RatButton extends Button {
        final int entity;final boolean checked;
        RatButton(int x,int y,int w,int h,String label,OnPress act,int id,boolean selected){super(x,y,w,h,Component.literal(label),act,DEFAULT_NARRATION);entity=id;checked=selected;}
        public void renderWidget(GuiGraphics g,int mx,int my,float dt){
            int l=getX(),t=getY();texture(g,PAPER,l,t,width,height,256);g.renderOutline(l,t,width,height,checked?0xff277966:0xff896f44);
            var e=minecraft.level.getEntity(entity);
            if(e instanceof net.minecraft.world.entity.LivingEntity r)net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g,l+26,t+height-4,25,-7,1,r);
            else g.renderItem(new net.minecraft.world.item.ItemStack(com.github.alexthe666.rats.registry.RatsItemRegistry.RAT_SACK.get()),l+9,t+height/2-8);
            String[] labels=getMessage().getString().split("\n");
            g.drawString(font,font.plainSubstrByWidth(labels[0],width-53),l+48,t+height/2-12,0xff36291f,false);
            g.drawString(font,labels[labels.length-1],l+48,t+height/2+2,0xff51432c,false);
            if(checked)g.drawString(font,"✓",l+width-12,t+3,0xff16604b,false);
        }
    }
    private class Notice extends Button {
        final int which;
        Notice(int x,int y,int w,int h,int which,String label,OnPress act){super(x,y,w,h,Component.literal(label),act,DEFAULT_NARRATION);this.which=which;}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
            int l=getX(),t=getY();var quest=cards.get(which);boolean lit=isHoveredOrFocused(),mine=quest.art().equals("mine");
            g.fill(l+2,t+2,l+width+2,t+height+2,0x990e0908);
            texture(g,PAPER,l,t,width,height,256);
            int color=mine?0xff6c2733:0xff244e4c;
            g.renderOutline(l,t,width,height,lit?0xffffda79:0xffb38c51);
            g.renderOutline(l+3,t+3,width-6,height-6,0xffa28254);
            int imageSize=Math.max(36,Math.min(width-15,height-63));
            illustration(g,quest,l+(width-imageSize)/2,t+6,imageSize);
            var item=quest.icon();if(item!=null){g.fill(l+6,t+6,l+26,t+26,0xffedd5a4);g.renderItem(new net.minecraft.world.item.ItemStack(item),l+8,t+8);}
            int title=t+imageSize+10;
            centered(g,quest.title(),l+width/2,title,0xff3b2219);
            centered(g,quest.kind()+" · 推荐 Lv."+quest.level(),l+width/2,title+12,0xff66503e);
            int bar=t+height-25;
            g.fill(l+5,bar,l+width-5,t+height-5,lit?0xff604028:color);
            centered(g,quest.payment(),l+width/2,bar+6,0xffffedc7);
        }
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        renderBackground(g);RenderSystem.enableBlend();
        g.fill(x-2,y-2,x+w+2,y+h+2,0xaf15100e);
        JaneScreen.paint(g,JaneScreen.FRAME,x,y,w,h,1280,420);
        JaneScreen.paint(g,JaneScreen.SEAL,x+12,y+5,22,22,128,128);
        g.drawString(font,"公会悬赏板",x+40,y+12,0xffffe4a3,true);
        if(gallery){
            if(portrait>=0){
                int side=h-59;texture(g,JaneScreen.art("quest_"+ILLUSTRATIONS[portrait]),x+(w-side)/2,y+30,side,side,128);
                centered(g,NAMES[portrait],x+w/2,y+h-18,0xffffdea4);
            }else{
                int cw=(w-24)/5,rh=(h-58)/2,side=Math.min(cw-4,rh-20);
                for(int i=0;i<NAMES.length;i++)texture(g,JaneScreen.art("quest_"+ILLUSTRATIONS[i]),x+12+i%5*cw+(cw-side)/2,y+30+i/5*rh,side,side,128);
            }
            super.render(g,mx,my,dt);return;
        }
        String pageLabel=catalog?page.title().substring(page.title().lastIndexOf('　')+1):page.title();
        g.drawString(font,pageLabel,x+w-15-font.width(pageLabel),y+12,0xffebd1a3,false);
        line(g,x+15,y+29,w-30,0xff927348);
        net.minecraft.world.item.ItemStack hover=null;
        if(page.layout().startsWith("rats/")){
            String[] params=page.layout().split("/",-1);var quest=Bounty.named(params[2]);int l=x+15,pw=w/3-7;
            texture(g,PAPER,l,y+35,pw,h-71,256);
            int side=Math.min(pw-10,h-129);illustration(g,quest,l+(pw-side)/2,y+40,side);
            int t=y+44+side;g.drawString(font,quest.title(),l+4,t,0xff4c3021,false);
            g.drawString(font,"耗时 "+quest.duration()/20+" 秒",l+4,t+12,0xff543929,false);
            g.drawString(font,"成功率 "+(params[3].equals("0")?"—":params[3]+"%"),l+4,t+24,0xff315849,false);
            g.drawString(font,page.line(),x+w/3+12,y+33,0xffefe0c1,false);
        }
        else if(page.layout().startsWith("tracks/")){
            texture(g,PAPER,x+14,y+34,w-28,h-112,256);
            String encoded=page.layout().substring(7);
            String[] rows=encoded.split(";"),names=page.line().split("\n");
            for(int i=0;!encoded.isEmpty()&&i<rows.length;i++){
                String[] bits=rows[i].split(",");long due=Long.parseLong(bits[0]),total=Long.parseLong(bits[1]);if(total<=0)total=Long.parseLong(bits[2]);
                long left=Math.max(0,due-minecraft.level.getGameTime()),sec=(left+19)/20;
                int l=x+22,t=y+42+i*40,bar=w-44;
                String status=due==0?"已归队 · 找简结算":left==0?"正在归队 · 可刷新":String.format(java.util.Locale.ROOT,"%d:%02d 后归来",sec/60,sec%60);
                g.drawString(font,font.plainSubstrByWidth(names[i],bar-font.width(status)-6),l,t,0xff422d22,false);
                g.drawString(font,status,l+bar-font.width(status),t,0xff513921,false);
                g.fill(l,t+14,l+bar,t+21,0xff79674f);
                g.fill(l,t+14,l+(int)(bar*net.minecraft.util.Mth.clamp(1f-(float)left/Math.max(1,total),0,1)),t+21,0xff428274);
            }
        }else if(loot!=null){
            texture(g,PAPER,x+14,y+34,w-28,h-88,256);
            g.drawString(font,page.line(),x+21,y+42,0xff503329,false);
            int cell=(w-45)/2,row=Math.min(22,(h-113)/((loot.drops().size()+1)/2));
            var drops=loot.drops();
            for(int i=0;i<drops.size();i++){
                int l=x+20+(i%2)*(cell+4),t=y+59+(i/2)*row;
                var id=drops.get(i);
                var stack=new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
                boolean rare=i>=loot.baseDrops().size();
                g.fill(l,t,l+cell,t+row-1,rare?0xffd1b288:0xffddc79e);
                g.fill(l,t,l+2,t+row-1,rare?0xff884461:0xff71684c);
                g.renderItem(stack,l+4,t+1);
                String text=(rare?"珍稀 · ":"")+stack.getHoverName().getString();
                g.drawString(font,font.plainSubstrByWidth(text,cell-24),l+23,t+5,rare?0xff64283f:0xff3b3023,false);
                if(mx>=l&&mx<l+cell&&my>=t&&my<t+row)hover=stack;
            }
        }else if(!catalog){
            int rows=(page.labels().size()+1)/2;
            int boxH=h-68-rows*22;
            texture(g,PAPER,x+14,y+34,w-28,boxH,256);
            boolean roster=detail==null;
            int size=roster?0:Math.max(0,Math.min(boxH-10,100));
            int left=x+22;
            if(size>0){illustration(g,detail,left,y+39,size);left+=size+9;}
            int tw=x+w-22-left;
            if(size>0){
                
                g.drawString(font,detail.title(),left,y+42,0xff462c23,false);
                g.drawString(font,"Lv."+detail.level()+" · "+detail.payment(),left,y+55,0xff705039,false);
                line(g,left,y+67,tw,0xffa88a5a);
            }
            int ty=size>0?y+73:y+43;
            g.enableScissor(left,ty,x+w-18,y+34+boxH-2);
            for(var line:font.split(Component.literal(page.line()),tw)){g.drawString(font,line,left,ty,0xff392820,false);ty+=10;}
            g.disableScissor();
        }
        super.render(g,mx,my,dt);
        if(hover!=null)g.renderTooltip(font,hover,mx,my);
    }
}
