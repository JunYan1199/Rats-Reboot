package guildjane;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import java.util.*;

public class PowerStation extends Block implements EntityBlock {
    static final IntegerProperty PART=IntegerProperty.create("part",0,7), LOAD=IntegerProperty.create("load",0,2);
    static final RegistryObject<Block> BLOCK=GuildHall.BLOCKS.register("power_station",PowerStation::new);
    static final RegistryObject<Item> ITEM=GuildHall.ITEMS.register("power_station",()->new BlockItem(BLOCK.get(),new Item.Properties()));
    static final RegistryObject<Item> LINK=GuildHall.ITEMS.register("wireless_connector",Connector::new);
    static final RegistryObject<Item> CELL=GuildHall.ITEMS.register("mana_cell",Cell::new);
    static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,GuildJane.ID);
    static final RegistryObject<BlockEntityType<Unit>> TYPE=TYPES.register("power_station",()->BlockEntityType.Builder.of(Unit::new,BLOCK.get()).build(null));
    public interface Output { boolean accepts(BlockEntity target); int supply(BlockEntity target,int budget); }
    static final Map<String,Output> modes=new LinkedHashMap<>();
    public static synchronized void registerOutput(String id,Output output){if(modes.putIfAbsent(id,Objects.requireNonNull(output))!=null)throw new IllegalArgumentException(id);}
    static void init(IEventBus bus){if(net.minecraftforge.fml.ModList.get().isLoaded("create"))StationDrive.register();TYPES.register(bus);bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(StationCompat::setup));registerOutput("RF",new Output(){
        public boolean accepts(BlockEntity t){return energy(t)!=null;}
        public int supply(BlockEntity t,int n){var e=energy(t);return e==null?0:e.receiveEnergy(n,false);}
        net.minecraftforge.energy.IEnergyStorage energy(BlockEntity t){
            for(Direction side:new Direction[]{null,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}){
                var e=t.getCapability(ForgeCapabilities.ENERGY,side).resolve().orElse(null);if(e!=null&&e.canReceive())return e;
            }return null;
        }
    });}
    PowerStation(){super(Properties.copy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(s->s.getValue(LOAD)>0&&s.getValue(PART)>=4?12:0));registerDefaultState(defaultBlockState().setValue(PART,0).setValue(LOAD,0));}
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(PART,LOAD);}
    static BlockPos offset(BlockPos p,int i){return p.offset(i%2,i/4,(i/2)%2);}
    static BlockPos root(BlockPos p,BlockState s){int i=s.getValue(PART);return p.offset(-(i%2),-(i/4),-((i/2)%2));}
    public BlockState getStateForPlacement(BlockPlaceContext c){for(int i=0;i<8;i++){var p=offset(c.getClickedPos(),i);if(c.getLevel().isOutsideBuildHeight(p)||!c.getLevel().getWorldBorder().isWithinBounds(p)||!c.getLevel().getBlockState(p).canBeReplaced(c)||c.getPlayer()!=null&&!c.getLevel().mayInteract(c.getPlayer(),p))return null;}return defaultBlockState();}
    public void setPlacedBy(Level w,BlockPos p,BlockState s,LivingEntity e,ItemStack st){for(int i=1;i<8;i++)w.setBlock(offset(p,i),s.setValue(PART,i),3);}
    public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(PART)==0?new Unit(p,s):null;}
    public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState s){return net.minecraft.world.level.material.PushReaction.BLOCK;}
    public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    public void onRemove(BlockState s,Level w,BlockPos p,BlockState next,boolean moving){
        if(!next.is(this)&&!w.isClientSide){var base=root(p,s);if(w.getBlockEntity(base) instanceof Unit u)u.release();
            for(int i=0;i<8;i++){var q=offset(base,i);if(!q.equals(p)&&w.getBlockState(q).is(this)&&w.getBlockState(q).getValue(PART)==i)w.removeBlock(q,false);}}
        super.onRemove(s,w,p,next,moving);
    }
    public InteractionResult use(BlockState s,Level w,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){
        if(!player.getItemInHand(h).isEmpty())return InteractionResult.PASS;
        if(player instanceof ServerPlayer sp && w.getBlockEntity(root(p,s)) instanceof Unit u)panel(sp,u,null);
        return InteractionResult.sidedSuccess(w.isClientSide);
    }
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level w,BlockState s,BlockEntityType<T> t){return !w.isClientSide&&t==TYPE.get()?(l,p,b,e)->((Unit)e).work():null;}
    static class Unit extends BlockEntity {
        CompoundTag animal=new CompoundTag();String mode="RF";Set<Long> links=new LinkedHashSet<>();int power;
        Unit(BlockPos p,BlockState s){super(TYPE.get(),p,s);}
        protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.put("animal",animal.copy());t.putString("mode",mode);t.putInt("power",power);t.putLongArray("links",links.stream().mapToLong(Long::longValue).toArray());}
        public void load(CompoundTag t){super.load(t);animal=t.getCompound("animal");mode=t.getString("mode");power=t.getInt("power");links.clear();for(long n:t.getLongArray("links"))links.add(n);}
        void display(int kind){for(int i=0;i<8;i++){var p=offset(worldPosition,i);var s=level.getBlockState(p);if(s.is(BLOCK.get()))level.setBlock(p,s.setValue(LOAD,kind),3);}setChanged();}
        void insert(ServerPlayer p){
            if(!animal.isEmpty())return;
            var list=level.getEntitiesOfClass(LivingEntity.class,new AABB(worldPosition).inflate(3),e->!e.isPassenger()&&!e.isVehicle()&&(e instanceof Pig||e instanceof TamedRat r&&r.isOwnedBy(p)&&Set.of("spark","volt").contains(r.getEvolution().id)));
            list.sort(Comparator.comparingDouble(e->e.distanceToSqr(p)));if(list.isEmpty()){msg(p,"请把猪或自己的电气鼠带到机器旁。");return;}
            var e=list.get(0);var tag=new CompoundTag();if(!e.save(tag))return;animal=tag;e.discard();display(e instanceof Pig?1:2);
        }
        void release(){if(animal.isEmpty()||!(level instanceof ServerLevel w))return;var e=EntityType.loadEntityRecursive(animal,w,a->{a.moveTo(worldPosition.getX()+.5,worldPosition.getY()+2.1,worldPosition.getZ()+.5,0,0);return a;});if(e!=null&&w.tryAddFreshEntityWithPassengers(e)){animal=new CompoundTag();display(0);}}
        void work(){
            if(animal.isEmpty()||mode.equals("机械动力应力")||mode.equals("法力"))return;
            boolean rf=mode.equals("RF");int tick=(int)(level.getGameTime()%20);
            if(!rf&&tick!=0)return;
            var out=modes.get(mode);if(out==null)return;
            int capacity=rf?StationCompat.rfCapacity.getAsInt():0;
            int budget=rf?capacity/20+(tick<capacity%20?1:0):1000;
            for(long n:links){var pos=BlockPos.of(n);if(!level.hasChunkAt(pos))continue;var t=level.getBlockEntity(pos);if(t!=null&&out.accepts(t))out.supply(t,budget);}
        }
    }
    static void msg(Player p,String s){p.displayClientMessage(Component.literal(s),true);}
    static class Connector extends Item{
        Connector(){super(new Properties().stacksTo(1));}
        public InteractionResult onItemUseFirst(ItemStack st,UseOnContext c){var w=c.getLevel();var p=c.getPlayer();if(p==null)return InteractionResult.PASS;if(w.isClientSide)return InteractionResult.SUCCESS;var pos=c.getClickedPos();var s=w.getBlockState(pos);var tag=st.getOrCreateTag();
            if(!w.mayInteract(p,pos))return InteractionResult.FAIL;
            if(s.is(BLOCK.get())){tag.putLong("station",root(pos,s).asLong());tag.putString("dimension",w.dimension().location().toString());msg(p,"已选取发电站");}
            else if(tag.contains("station")&&tag.getString("dimension").equals(w.dimension().location().toString())&&w.hasChunkAt(BlockPos.of(tag.getLong("station")))&&w.getBlockEntity(BlockPos.of(tag.getLong("station"))) instanceof Unit u&&w.mayInteract(p,u.getBlockPos())){
                var target=w.getBlockEntity(pos);var out=modes.get(u.mode);if(u.links.remove(pos.asLong()))msg(p,"已断开连接");else if(target!=null&&out!=null&&out.accepts(target)&&(!u.mode.equals("机械动力应力")||StationDrive.attach((ServerPlayer)p,u,pos,c.getClickedFace()))){u.links.add(pos.asLong());msg(p,"已连接 · 共 "+u.links.size()+" 个接收端");}else msg(p,"目标没有当前模式的接收接口");u.setChanged();
            }else msg(p,"请先右键同维度的发电站");return InteractionResult.CONSUME;}
    }
    static class Cell extends Item{
        public InteractionResult useOn(UseOnContext c){var w=c.getLevel();var s=w.getBlockState(c.getClickedPos());if(!s.is(BLOCK.get()))return InteractionResult.PASS;
            if(!w.isClientSide && c.getPlayer()!=null && w.getBlockEntity(root(c.getClickedPos(),s)) instanceof Unit u){
                if(!u.mode.equals("法力"))msg(c.getPlayer(),"请先把发电站切换为法力模式");else{var tag=c.getItemInHand().getOrCreateTag();tag.putLong("source",u.getBlockPos().asLong());tag.putString("world",w.dimension().location().toString());msg(c.getPlayer(),"法力存储装置已绑定");}}
            return InteractionResult.sidedSuccess(w.isClientSide);}
        public void inventoryTick(ItemStack stack,Level w,Entity holder,int slot,boolean selected){
            if(!(holder instanceof ServerPlayer p)||w.getGameTime()%20!=0||StationCompat.manaGet==null)return;
            var tag=stack.getOrCreateTag();int charge=tag.getInt("charge");
            if(tag.contains("source")&&tag.getString("world").equals(w.dimension().location().toString())){var pos=BlockPos.of(tag.getLong("source"));if(w.hasChunkAt(pos)&&w.getBlockEntity(pos) instanceof Unit u&&u.mode.equals("法力")&&!u.animal.isEmpty()){charge+=Math.min(10000-charge,1000);}}
            charge-=StationCompat.refill(p,Math.min(charge,tag.contains("amount")?tag.getInt("amount"):1000));tag.putInt("charge",charge);}
        Cell(){super(new Properties().stacksTo(1));}
        public InteractionResultHolder<ItemStack> use(Level w,Player p,InteractionHand h){var st=p.getItemInHand(h);if(p instanceof ServerPlayer sp)panel(sp,null,st);return InteractionResultHolder.sidedSuccess(st,w.isClientSide);}
    }
    static void panel(ServerPlayer p,Unit u,ItemStack cell){
        var actions=new ArrayList<Runnable>();var buttons=new SimpleContainer(27);
        java.util.function.BiConsumer<String,Runnable> add=(name,run)->{buttons.setItem(actions.size(),new ItemStack(Items.PAPER).setHoverName(Component.literal(name)));actions.add(run);};
        if(u!=null){for(String mode:modes.keySet())add.accept((mode.equals(u.mode)?"✓ ":"")+mode,()->{u.mode=mode;u.setChanged();});
            if(net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks"))add.accept("法力 · 持装置绑定",()->{u.mode="法力";u.setChanged();});
            add.accept("装入附近的猪或电气鼠",()->u.insert(p));add.accept("放出舱内生物",u::release);
        }else for(int n:new int[]{0,10,50,100,250,500,1000})add.accept("每次补给 "+n+" 法力",()->cell.getOrCreateTag().putInt("amount",n));
        p.openMenu(new SimpleMenuProvider((id,inv,who)->new ChestMenu(MenuType.GENERIC_9x3,id,inv,buttons,3){
            public boolean stillValid(Player pl){return u!=null?!u.isRemoved()&&u.getLevel()==pl.level()&&pl.distanceToSqr(Vec3.atCenterOf(u.getBlockPos()))<64:pl.getMainHandItem()==cell||pl.getOffhandItem()==cell;}
            public void clicked(int slot,int b,ClickType type,Player pl){if(stillValid(pl)&&type==ClickType.PICKUP&&slot>=0&&slot<actions.size()){actions.get(slot).run();p.closeContainer();}}
            public ItemStack quickMoveStack(Player pl,int i){return ItemStack.EMPTY;}
        },Component.literal(u==null?"法力存储装置 · "+cell.getOrCreateTag().getInt("charge")+"/10000 · 每秒 "+(cell.getOrCreateTag().contains("amount")?cell.getOrCreateTag().getInt("amount"):1000):"发电站 · "+u.mode+" · "+u.links.size()+" 路")));
    }
}
