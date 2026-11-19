package guildjane;

import com.google.gson.Gson;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

record Bounty(String id,String title,String kind,int level,String target,int count,String objective,List<String> stories,List<String> baseDrops,List<String> rareDrops) {
    static final List<Bounty> ALL;
    static {
        try(var in=new InputStreamReader(Objects.requireNonNull(Bounty.class.getResourceAsStream("/guildjane_bounties.json")),StandardCharsets.UTF_8)){
            ALL=List.of(new Gson().fromJson(in,Bounty[].class));
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    static List<Bounty> daily(ServerPlayer player, boolean bosses){
        long day=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Taipei")).toEpochDay();
        long seed=player.server.overworld().getSeed();
        var result=new ArrayList<Bounty>();
        for(String kind:bosses?List.of("首领"):List.of("送餐","采集","寻物","讨伐")){
            var pool=new ArrayList<>(ALL.stream().filter(q->q.kind.equals(kind)&&q.exists()).sorted(Comparator.comparingInt(Bounty::level)).toList());
            var rng=new Random(seed ^ (day*0x9E3779B97F4A7C15L) ^ kind.hashCode());
            int slots=Math.min(bosses?2:3,pool.size());
            for(int i=0;i<slots;i++){
                int start=i*pool.size()/slots,end=(i+1)*pool.size()/slots;
                result.add(pool.get(start+rng.nextInt(end-start)));
            }
        }
        result.sort(Comparator.comparingInt(Bounty::level));
        return result;
    }
    static Bounty named(String id){return ALL.stream().filter(q->q.id.equals(id)).findFirst().orElseThrow();}
    static Bounty fromJob(CompoundTag job){return named(job.contains("quest")?job.getString("quest"):job.getBoolean("hard")?"mine":"mail");}
    boolean food(){return kind.equals("送餐");}
    boolean hunt(){return kind.equals("讨伐")||boss();}
    boolean boss(){return kind.equals("首领");}
    boolean hasDrops(){return baseDrops!=null&&!baseDrops.isEmpty();}
    List<String> drops(){var list=new ArrayList<>(baseDrops);list.addAll(rareDrops);return list;}
    int trophyChance(double mean){return rareDrops.isEmpty()?0:Mth.clamp((int)Math.round(5+mean*.35),5,40);}
    String dropNames(List<String> ids){return String.join("、",ids.stream().map(s->itemById(s).getDescription().getString()).toList());}
    boolean exists(){var key=new ResourceLocation(target);return hunt()?ForgeRegistries.ENTITY_TYPES.containsKey(key):ForgeRegistries.ITEMS.containsKey(key);}
    Item item(){return ForgeRegistries.ITEMS.getValue(new ResourceLocation(target));}
    Item icon(){
        if(!hunt())return item();
        var egg=SpawnEggItem.byId(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(target)));
        return egg!=null?egg:hasDrops()?itemById(baseDrops.get(0)):Items.IRON_SWORD;
    }
    int held(ServerPlayer p){return p.getInventory().items.stream().filter(s->s.is(item())).mapToInt(ItemStack::getCount).sum();}
    // Equal-level teams win 75%; the width of the level band increases with difficulty.
    int chance(double mean){return Mth.clamp((int)Math.round(75+24*(mean-level)/Math.max(4,level*.35)),10,98);}
    int duration(){return 20*(20+level);}
    int pay(){return 2+(level+3)/8;}
    int rareChance(double mean){return Mth.clamp((int)Math.round(5+Math.min(level,mean)*.55),5,45);}
    String payment(){return hasDrops()?"首领掉落 ×2 · 公会物资":"公会物资 · 随机报酬";}
    String art(){return kind.equals("采集")?"mine":"mail";}
    private static void add(ListTag bag,ItemStack stack){bag.add(stack.save(new CompoundTag()));}
    private static Item itemById(String id){return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));}
    private static final String[] COMMON={"minecraft:iron_ingot","minecraft:gold_ingot","rats:cheese","rats:string_cheese"};
    private static final String[] FIELD={"minecraft:lapis_lazuli","rats:rat_flute","rats:rat_sack","farmersdelight:dumplings"};
    private static final String[] RARE={"minecraft:diamond","rats:token_fragment","rats:gilded_rat_flute","twilightforest:ironwood_ingot"};
    String relicId(){return "ageofmythology:"+target.replace(':','_')+"_item";}
    private static void supplies(ListTag bag,RandomSource rng,String[] pool,int count){
        var candidates=Arrays.stream(pool).filter(id->ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id))).toList();
        if(candidates.isEmpty())return;
        Item item=itemById(candidates.get(rng.nextInt(candidates.size())));
        add(bag,new ItemStack(item,Math.min(count,item.getMaxStackSize())));
    }
    void bonusLoot(ServerPlayer p,ListTag bag){
        var world=p.serverLevel();
        var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(target));
        var mob=type.create(world);if(mob==null)return;
        mob.moveTo(p.getX(),p.getY(),p.getZ(),0,0);
        var ctx=new net.minecraft.world.level.storage.loot.LootParams.Builder(world)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,mob)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,p.position())
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,world.damageSources().playerAttack(p))
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.KILLER_ENTITY,p)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_KILLER_ENTITY,p)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER,p)
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
        var loot=p.server.getLootData().getLootTable(type.getDefaultLootTable()).getRandomItems(ctx);
        var choices=loot.stream().filter(st->!st.isEmpty()&&!ForgeRegistries.ITEMS.getKey(st.getItem()).toString().equals(relicId())).toList();
        if(!choices.isEmpty())add(bag,choices.get(p.getRandom().nextInt(choices.size())).copy());
        else supplies(bag,p.getRandom(),baseDrops.toArray(String[]::new),1);
        if(ForgeRegistries.ITEMS.containsKey(new ResourceLocation(relicId()))&&p.getRandom().nextFloat()<.02f)add(bag,new ItemStack(itemById(relicId())));
    }
    void book(ServerPlayer p,CompoundTag job,double mean){
        var rng=p.getRandom();int odds=chance(mean);boolean win=rng.nextInt(100)<odds;
        job.putString("quest",id);job.putString("title",title);job.putInt("chance",odds);job.putDouble("levelMean",mean);
        job.putBoolean("success",win);job.putString("story",job.getString("lead")+stories.get((win?0:2)+rng.nextInt(2)));
        var payout=new ListTag();
        if(food()){
            int left=count;
            for(var stack:p.getInventory().items){
                if(left==0)break;
                if(!stack.is(item()))continue;
                var portion=stack.split(Math.min(left,stack.getCount()));left-=portion.getCount();
                if(!win)add(payout,portion);
            }
            p.getInventory().setChanged();
        }
        if(win){
            int bonus=rng.nextInt(1+Math.min(4,(int)mean/12));
            supplies(payout,rng,new String[]{"minecraft:emerald","minecraft:iron_ingot","minecraft:gold_ingot","irons_spellbooks:arcane_essence","goety:ectoplasm","rats:cheese"},pay()+bonus);
            if(hasDrops())supplies(payout,rng,new String[]{"irons_spellbooks:magic_cloth","irons_spellbooks:arcane_salvage","goety:dark_ingot","rats:token_fragment","minecraft:diamond"},2+rng.nextInt(3));
            if(kind.equals("采集"))add(payout,new ItemStack(item(),count));
            if(hasDrops()){
                List<String> pool=rng.nextInt(100)<trophyChance(mean)?rareDrops:baseDrops;
                add(payout,new ItemStack(itemById(pool.get(rng.nextInt(pool.size())))));
                bonusLoot(p,payout);
            }else{
                boolean rare=rng.nextInt(100)<rareChance(mean);
                String[] pool=rare?(level>=22&&mean>=16?RARE:FIELD):(level>=12&&rng.nextBoolean()?FIELD:COMMON);
                var available=Arrays.stream(pool).filter(s->ForgeRegistries.ITEMS.containsKey(new ResourceLocation(s))).toList();
                var extra=available.isEmpty()?Items.IRON_INGOT:itemById(available.get(rng.nextInt(available.size())));
                add(payout,new ItemStack(extra,pool==COMMON?1+rng.nextInt(2+Math.min(3,(int)mean/15)):1));
            }
        }
        job.put("payout",payout);
    }
    static List<ItemStack> payout(CompoundTag job){
        if(!job.contains("payout"))return job.getBoolean("success")?List.of(new ItemStack(Items.EMERALD,job.getBoolean("hard")?4:2)):List.of();
        return job.getList("payout",10).stream().map(t->ItemStack.of((CompoundTag)t)).toList();
    }
}
