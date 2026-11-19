package guildjane;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=GuildJane.ID)
public class Conversation {
    static final Map<UUID,Conversation> sessions=new HashMap<>();
    final ServerPlayer p; final GuildJane.Receptionist jane; final GuildLedger guild; final CompoundTag me;
    final List<String> labels=new ArrayList<>(); final List<Runnable> actions=new ArrayList<>();
    final List<UUID> selected=new ArrayList<>(); UUID token; Bounty quest; int sheet; boolean bossSection;
    String cue="",layout="";
    final net.minecraft.core.BlockPos wall;
    final net.minecraft.world.level.Level world;
    net.minecraft.world.phys.AABB station(){return wall==null?jane.getBoundingBox():new net.minecraft.world.phys.AABB(wall);}
    boolean inReach(){
        if(world!=p.level())return false;
        if(wall!=null)return p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(wall))<=49 && world.getBlockState(wall).is(GuildHall.BOARD.get());
        return jane.isAlive()&&p.distanceToSqr(jane)<=36;
    }
    static void openBoard(ServerPlayer p,net.minecraft.core.BlockPos pos){
        var c=new Conversation(p,null,pos.immutable());sessions.put(p.getUUID(),c);c.board();
    }
    Conversation(ServerPlayer p,GuildJane.Receptionist jane,net.minecraft.core.BlockPos wall){this.wall=wall;world=p.level();this.p=p;this.jane=jane;guild=GuildLedger.get(p.server);me=guild.player(p);}
    static void open(ServerPlayer p,GuildJane.Receptionist jane){
        var talk=new Conversation(p,jane,null);sessions.put(p.getUUID(),talk);talk.home();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)sessions.remove(p.getUUID());}
    static void choose(ServerPlayer p,GuildWire.Pick pick){
        if(p==null)return;
        var c=sessions.get(p.getUUID());
        if(c==null||!pick.token().equals(c.token))return;
        int index=pick.index();if(index==-1){sessions.remove(p.getUUID());return;}
        if(!c.inReach()){sessions.remove(p.getUUID());return;}
        if(index<0||index>=c.actions.size())return;
        Runnable action=c.actions.get(index);c.token=null;c.actions.clear();c.labels.clear();action.run();
    }
    void option(String label,Runnable action){labels.add(label);actions.add(action);}
    void show(String title,String line){
        if(jane!=null)jane.duty.speak(p,Math.min(88,Math.max(35,line.length()+12)));
        if(wall!=null)cue="";
        if(!cue.isEmpty()){
            long now=p.server.overworld().getGameTime();
            String key="heard_"+cue;
            if(me.contains(key)&&now-me.getLong(key)<900)cue="";
            else{me.putLong(key,now);guild.setDirty();}
        }
        token=UUID.randomUUID();GuildWire.send(p,new GuildWire.Page(token,title,line,cue,layout,List.copyOf(labels)));cue="";
    }
    static boolean engaged(ServerPlayer p,GuildJane.Receptionist jane){
        var c=sessions.get(p.getUUID());return c!=null&&c.jane==jane&&p.isAlive();
    }
    static ServerPlayer otherGuest(GuildJane.Receptionist jane){
        for(var c:sessions.values())if(c.jane==jane&&c.p.isAlive()&&c.p.level()==jane.level()&&c.p.distanceToSqr(jane)<36)return c.p;
        return null;
    }
    void back(){if(wall==null)option("回到柜台",this::home);else option("回到悬赏板",this::board);}
    void say(String title,String line){back();show(title,line);}
    void comment(String voice,String title,String line){cue=voice;say(title,line);}
    void remember(String key){me.putBoolean(key,true);guild.setDirty();}
    String jobSlot="job";int trackPage;
    CompoundTag job(){return me.getCompound(jobSlot);}
    void tracks(){
        var slots=GuildLedger.jobs(me);int pages=Math.max(1,(slots.size()+1)/2);trackPage=Math.min(trackPage,pages-1);
        var times=new ArrayList<String>();var names=new ArrayList<String>();
        for(String slot:slots.subList(trackPage*2,Math.min(slots.size(),trackPage*2+2))){
            var j=me.getCompound(slot);var q=Bounty.fromJob(j);
            times.add((GuildLedger.ready(j)?0:j.getLong("due"))+","+j.getLong("duration")+","+q.duration());
            names.add(j.getString("lead")+" · "+q.title());
        }
        layout="tracks/"+String.join(";",times);
        if(trackPage>0)option("上一页",()->{trackPage--;tracks();});
        if(trackPage+1<pages)option("下一页",()->{trackPage++;tracks();});
        option("继续接委托",this::board);option("刷新归队状态",this::tracks);
        show("派遣进度　"+(trackPage+1)+" / "+pages,String.join("\n",names));
    }
    void home(){
        boolean first=!me.getBoolean("met");remember("met");
        String greeting=first?"小心手肘，墨还没干。你来找活儿，还是来找刚才从这儿跑过去的那位？":"又见面了。登记簿在我这儿。今天打算带谁出去走走？";
        var jobs=GuildLedger.jobs(me);if(!jobs.isEmpty()){
            jobSlot=jobs.stream().filter(k->GuildLedger.ready(me.getCompound(k))).findFirst().orElse(jobs.get(0));
            if(GuildLedger.ready(job())){option("它们回来了？",this::returned);greeting="听，门边那阵小爪子声。你来得正好。";}
            else option("看看我的鼠队",this::progress);
        }option("找活儿，我想派一支鼠队",this::board);
        option("聊两句",this::chat);
        if(guild.unlocked())option("聊聊你的衣服",this::wardrobe);
        if(guild.unlocked()&&!me.getBoolean("victorySeen"))option("今天有什么好事？",()->{remember("victorySeen");say("公会头一单","公会第一支出征的队伍平安回来了。喏，记录在这儿。"+guild.data.getString("firstStory"));});
        option("随便看看",()->say("大厅","请便。墙边的委托随便翻。要是哪张纸自己动起来了，轻轻敲两下，底下那位就醒了。"));
        if(first)cue="greet";
        show("委托柜台",greeting);
    }
    void chat(){
        if(!me.getBoolean("ink"))option("刚才谁跑过去了？",()->{remember("ink");say("三枚爪印","一只前脚踩了墨、还坚持自己没上过柜台的鼠。证据嘛……她抬起登记簿的一角。暂时替它保密。");});
        else option("今天柜台倒挺干净",()->say("袖口的爪印","嗯。我把印台挪高了。……她瞥见袖口上半枚爪印。……看来还不够高。"));
        if(me.getBoolean("ink")&&me.getBoolean("won"))option("那枚爪印，后来擦了吗？",()->say("最早的登记纸","留着了。公会第一天开门，总得有人先签个名。"));
        option("你也会离开柜台？",()->say("委托板","当然。站在柜台后面，永远看不见委托板最下面那排字。你蹲下来看看——从它们的高度看，贴得还是高了点。"));
        option("你在看公会的徽章？",()->comment("emblem","公会徽章","你看盾牌上那条尾巴。刻章的人非说它是只兔子。我请他在公会住了一晚，第二天，他自己把尾巴刻长了。"));
        back();show("柜台闲聊","委托簿可以合上一会儿。你想问什么？");
    }
    void board(){
        layout="";
        var pool=Bounty.daily(p,bossSection);
        int pages=(pool.size()+1)/2;
        sheet=Math.max(0,Math.min(sheet,pages-1));
        var visible=pool.subList(2*sheet,Math.min(pool.size(),2*sheet+2));
        if(wall!=null)layout="catalog/"+String.join(",",visible.stream().map(Bounty::id).toList());
        cue="choose";
        for(var q:visible)option(q.title()+" · Lv."+q.level(),()->prepare(q));
        if(sheet>0)option("上一页",()->{sheet--;board();});
        if(sheet+1<pages)option("下一页",()->{sheet++;board();});
        option(bossSection?"常规委托":"满级首领",()->{bossSection=!bossSection;sheet=0;board();});
        if(wall!=null&&!GuildLedger.jobs(me).isEmpty())option("派遣进度",this::tracks);
        if(wall==null)back();
        show((bossSection?"今日首领　":"今日委托　")+(sheet+1)+" / "+pages,"今天的悬赏按推荐等级排列。每日零点换榜，已出发的队伍照常归来。");
    }
    void boardStatus(){
        var q=Bounty.fromJob(job());layout="detail/"+q.id();
        boolean ready=GuildLedger.ready(job());
        show(ready?"鼠队已归来":"鼠队出征中",ready?"回执已经送到柜台。找简听旅途见闻，办理结算。":
            "带队的是"+job().getString("lead")+"。委托已登记，鼠队正在路上。");
    }
    void prepare(Bounty selectedQuest){
        quest=selectedQuest;selected.clear();for(var rat:GuildLedger.available(p,station()))if(selected.size()<3)selected.add(rat.getUUID());brief();
    }
    void brief(){
        layout=wall==null?"":"detail/"+quest.id();
        var team=GuildLedger.available(p,station()).stream().filter(r->selected.contains(r.getUUID())).toList();
        double mean=team.stream().mapToInt(r->r.getRatLevel()).average().orElse(0);
        int chance=quest.chance(mean);
        String line=quest.objective()+"\n";
        boolean supplied=!quest.food()||quest.held(p)>=quest.count();
        if(quest.food())line+="备货 "+quest.held(p)+"/"+quest.count()+" · 出发交货，失败原物退回。\n";
        if(team.isEmpty())line+="带驯服的鼠到十二格内，再来编队。";
        else{
            line+=String.format(Locale.ROOT,"队伍均级 %.1f · 成功率 %d%% · 预计 %d 秒",mean,chance,quest.duration()/20);
            if(chance<50)cue="careful";
            if(supplied)option(chance<50?"我知道，照常派遣":"就它们，出发",this::dispatch);
        }
        if(quest.hasDrops())option("奖励列表",this::rewardList);
        option("更换队员",this::roster);back();
        if(wall==null)line+="\n成功报酬　"+quest.payment();
        show(quest.title(),line);
    }
    void rewardList(){
        if(wall!=null)layout="loot/"+quest.id();
        var team=GuildLedger.available(p,station()).stream().filter(r->selected.contains(r.getUUID())).toList();
        double mean=team.stream().mapToInt(r->r.getRatLevel()).average().orElse(0);
        String rule=quest.rareDrops().isEmpty()?"首领掉落两份，另发公会物资。":team.isEmpty()?"保底一份，另抽战利品表，珍稀档概率由队伍均级决定。":
            "保底一份，另抽战利品表 · 当前珍稀档 "+quest.trophyChance(mean)+"%";
        if(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new net.minecraft.resources.ResourceLocation(quest.relicId())))rule+=" · 遗物 2%";
        option("回到委托",this::brief);
        if(wall==null)rule+="\n常规　"+quest.dropNames(quest.baseDrops())+(quest.rareDrops().isEmpty()?"":"\n珍稀　"+quest.dropNames(quest.rareDrops()));
        show(quest.title()+" · 奖励",rule);
    }
    int rosterPage;
    record RatCard(UUID id,int entity,String name,String level,String state){}
    void roster(){
        var cards=new ArrayList<RatCard>();
        var available=GuildLedger.available(p,station());
        var eligible=new HashSet<UUID>();
        for(var r:available){eligible.add(r.getUUID());cards.add(new RatCard(r.getUUID(),r.getId(),r.getName().getString(),""+r.getRatLevel(),"附近"));}
        selected.retainAll(eligible);
        for(var w:p.server.getAllLevels())for(var e:w.getAllEntities()){
            if(e instanceof com.github.alexthe666.rats.server.entity.rat.TamedRat r&&p.getUUID().equals(r.getOwnerUUID())&&!eligible.contains(r.getUUID())){
                cards.add(new RatCard(r.getUUID(),w==p.level()?r.getId():-1,r.getName().getString(),""+r.getRatLevel(),w!=p.level()?"其他维度":station().inflate(12).intersects(r.getBoundingBox())?"暂不可派":"距离过远"));
            }
        }
        for(String slot:GuildLedger.jobs(me))for(var raw:me.getCompound(slot).getList("away",10)){
            var n=(CompoundTag)raw;var label=net.minecraft.network.chat.Component.Serializer.fromJson(n.contains("CustomName")?n.getString("CustomName"):"\"驯服鼠\"");
            cards.add(new RatCard(n.getUUID("UUID"),-1,label==null?"驯服鼠":label.getString(),n.contains("GuildShownLevel")?""+n.getInt("GuildShownLevel"):"?","出征中"));
        }
        int pages=Math.max(1,(cards.size()+3)/4);rosterPage=Math.min(rosterPage,pages-1);var ids=new ArrayList<String>();
        for(var card:cards.subList(rosterPage*4,Math.min(cards.size(),rosterPage*4+4))){
            boolean ok=eligible.contains(card.id());ids.add(card.entity()+","+(ok?1:0)+","+(selected.contains(card.id())?1:0));
            option(card.name()+"\nLv."+card.level()+" · "+card.state(),()->{if(ok&&!selected.remove(card.id())&&selected.size()<3)selected.add(card.id());roster();});
        }
        if(rosterPage>0)option("上一页",()->{rosterPage--;roster();});
        if(rosterPage+1<pages)option("下一页",()->{rosterPage++;roster();});
        option("清空选择",()->{selected.clear();roster();});
        option("核对名单",this::brief);
        var team=available.stream().filter(r->selected.contains(r.getUUID())).toList();
        int odds=quest.chance(team.stream().mapToInt(r->r.getRatLevel()).average().orElse(0));
        if(!team.isEmpty()&&(!quest.food()||quest.held(p)>=quest.count()))option("确认出征",this::dispatch);
        layout="rats/"+String.join(";",ids)+"/"+quest.id()+"/"+(team.isEmpty()?0:odds);
        show("队伍管理　"+(rosterPage+1)+"/"+pages,"已选 "+selected.size()+" / 3　·　共 "+cards.size()+" 只");
    }
    void dispatch(){
        if(wall==null)layout="";
        if(!guild.dispatch(p,station(),selected,quest)){say("重核名单","再数数队员和货物。出发前，它们都得在这里。");return;}
        jobSlot=me.getString("latestJob");if(wall!=null){trackPage=0;tracks();return;}
        cue="depart";option("看派遣进度",this::progress);back();show("鼠队已出发","签好了。回来时走正门，别又把邮袋从窗缝里硬塞进来。……后半句是说给它们听的。");
    }
    void progress(){
        if(GuildLedger.ready(job())){returned();return;}
        option("我只是担心它们",()->comment("trust","等候归队","嗯，我知道。你进门先看的是门边那排小脚印。回执一到，我替你压在最上面。"));back();
        show("出征途中","还在路上。你要看的是这一栏，旁边那栏是报酬。刚才你的眼睛先去了哪边，我可看见了。");
    }
    void returned(){
        if(!GuildLedger.ready(job())){home();return;}
        option("一路上发生了什么？",this::report);option("好，给它们结算吧",this::settle);back();
        if(job().getBoolean("success"))cue="returned";
        show("鼠队归来","回来了。你先数，我刚才已经数过两遍。");
    }
    void report(){
        if(!GuildLedger.ready(job())){home();return;}
        option("好，给它们结算吧",this::settle);back();show("旅途记录",GuildLedger.story(job()));
    }
    void settle(){
        var job=job();if(!GuildLedger.ready(job)){home();return;}
        boolean success=job.getBoolean("success");me.remove(jobSlot);guild.setDirty();
        var rewards=Bounty.payout(job);
        String receipt=String.join("、",rewards.stream().map(r->r.getHoverName().getString()+" ×"+r.getCount()).toList());
        for(var stack:rewards)if(!p.getInventory().add(stack))p.drop(stack,false);
        if(!success){comment("rethink","委托未完成","都回来了就好。来，把地图摊开，我们看看哪一段该换条路。"+(receipt.isEmpty()?"":"\n退回货物　"+receipt));return;}
        remember("won");
        if(guild.unlocked()){comment("thanks","委托已结算","这单我盖过章了。报酬收好，记得给跑腿的也分一份。\n"+receipt);return;}
        guild.data.putString("firstStory",GuildLedger.story(job));guild.data.putBoolean("unlocked",true);remember("victorySeen");
        change(true);p.server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("老鼠公会 · 首次成功出征已结算，全服解锁「夜曲」"),false);
        option("聊聊你的衣服",this::wardrobe);back();
        cue="nocturne";show("夜曲 · 全服已解锁","我跟它们打了个赌。第一单要是办得漂亮，今晚庆功，我就穿件像样的来。怎么，队长？它们都回来了，你还打算站着陪我值班？\n本单报酬　"+receipt);
    }
    void change(boolean night){
        guild.outfit(night);jane.getEntityData().set(GuildJane.Receptionist.NIGHT,night);
        jane.level().broadcastEntityEvent(jane,(byte)61);
        p.serverLevel().sendParticles(ParticleTypes.END_ROD,jane.getX(),jane.getY()+1,jane.getZ(),16,.28,.5,.22,.018);
        p.serverLevel().playSound(null,jane.blockPosition(),SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.NEUTRAL,.55f,1.4f);
    }
    void wardrobe(){
        if(!guild.unlocked()){home();return;}
        boolean target=!guild.night();
        option(target?"今晚穿夜曲怎么样？":"今天穿原来那件吧",()->{
            if(!guild.unlocked()){home();return;}
            change(target);comment("decisive",target?"已换上夜曲":"已换回原版",target?"好啊。劳驾替我看一眼柜台，尤其盯着那盒印泥。":"惦记我那件外套？也是，口袋够大，能装下两本账和一个逃班的家伙。");
        });back();show("公会衣装",guild.night()?"这件？队员们赢来的。你倒是挺留意我今天穿什么。":"外套在身上，印泥也在。怎么，今天想换点花样？");
    }
}
