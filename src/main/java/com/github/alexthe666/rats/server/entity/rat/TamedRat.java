package com.github.alexthe666.rats.server.entity.rat;

import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import com.github.alexthe666.rats.server.legion.RatLegion;
import com.github.alexthe666.rats.server.legion.LegionBaton;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.misc.RatTubeTransit;
import com.github.alexthe666.rats.client.events.ForgeClientEvents;
import com.github.alexthe666.rats.data.tags.RatsItemTags;
import com.github.alexthe666.rats.registry.*;
import com.github.alexthe666.rats.server.block.RatCageBlock;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.entity.RatMount;
import com.github.alexthe666.rats.server.entity.ai.goal.*;
import com.github.alexthe666.rats.server.entity.ai.goal.harvest.RatHarvestCropsGoal;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.*;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.EtherealRatNavigation;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.RatFlightNavigation;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.RatNavigation;
import com.github.alexthe666.rats.server.entity.mount.RatBiplaneMount;
import com.github.alexthe666.rats.server.entity.mount.RatMountBase;
import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import com.github.alexthe666.rats.server.items.RatSackItem;
import com.github.alexthe666.rats.server.items.RatStaffItem;
import com.github.alexthe666.rats.server.items.upgrades.*;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.*;
import com.github.alexthe666.rats.server.message.ManageRatStaffPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.SetDancingRatPacket;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import com.github.alexthe666.rats.server.misc.RatVariant;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import com.github.alexthe666.rats.server.items.RatTrainingItem;
import com.github.alexthe666.rats.server.brick.BrickKitchen;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.block.entity.UpgradeCombinerBlockEntity;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TamedRat extends InventoryRat {
    private static final EntityDataAccessor<Boolean> BRICK = SynchedEntityData.defineId(TamedRat.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BRICK_COOKING = SynchedEntityData.defineId(TamedRat.class,EntityDataSerializers.BOOLEAN);
    /** 布瑞克职业动作（客户端据此选动画）：见 BrickJob.Action，0 = 无 */
    private static final EntityDataAccessor<Byte> BRICK_ACTION = SynchedEntityData.defineId(TamedRat.class,EntityDataSerializers.BYTE);
    public byte getBrickAction(){return entityData.get(BRICK_ACTION);}
    public void setBrickAction(int action){entityData.set(BRICK_ACTION,(byte)action);}
    public final BrickKitchen kitchen = new BrickKitchen(this);
    /** 布瑞克职业工作：子任务开关、圈地、驻守点、参数、寻宝计划（国王权杖设置） */
    public final com.github.alexthe666.rats.server.brick.BrickWork work = new com.github.alexthe666.rats.server.brick.BrickWork(this);
    private static final EntityDataAccessor<Integer> EVOLUTION = SynchedEntityData.defineId(TamedRat.class,EntityDataSerializers.INT);
    public RatEvolution getEvolution(){return RatEvolution.byIndex(entityData.get(EVOLUTION));}
    private InteractionResult useEvolutionStone(Player player,ItemStack stack,RatEvolution form){
        if(!level().isClientSide()){
            Component feedback;
            if(!isOwnedBy(player))feedback=Component.translatable("evolution.rats.owner");
            else if(getRespawnCountdown()>0||isBaby()||isBrick())feedback=Component.translatable("evolution.rats.base");
            else if(getEvolution()!=RatEvolution.NONE)feedback=Component.translatable("evolution.rats.already",getEvolution().title());
            else if(getRatLevel()<RatEvolution.MIN_LEVEL)feedback=Component.translatable("evolution.rats.level",RatEvolution.MIN_LEVEL,getRatLevel());
            else{
                entityData.set(EVOLUTION,form.ordinal());
                form.updateSpeed(this);
                if(!player.getAbilities().instabuild)stack.shrink(1);
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME,.9F,1.4F);
                ((net.minecraft.server.level.ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY()+getBbHeight()*.65,getZ(),18,.27,.2,.27,.035);
                feedback=Component.translatable("evolution.rats.complete",form.title());
            }
            player.displayClientMessage(feedback,true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }
    public boolean isBrick(){return entityData.get(BRICK);}
    public boolean isBrickCooking(){return entityData.get(BRICK_COOKING);}
    public void setBrickCooking(boolean value){entityData.set(BRICK_COOKING,value);}
    @Override protected Component getTypeName(){return isBrick()?Component.translatable("entity.rats.brick"):getEvolution()==RatEvolution.NONE?super.getTypeName():getEvolution().title();}
    private InteractionResult toggleBrick(Player player){
        if(!isOwnedBy(player)||getRespawnCountdown()>0)return InteractionResult.FAIL;
        if(!level().isClientSide()){
            if(isBrick()){
                kitchen.stopWork();
                if(getCommand()==RatCommand.COOK)setCommand(RatCommand.WANDER);
                work.stop();
                // 变回老鼠：副手和 4 格盔甲栏里的东西放回背包，放不下就掉在脚边
                for(int slot=RatContainer.BRICK_OFFHAND;slot<RatContainer.TOTAL;slot++){
                    ItemStack off=getInventory().removeItemNoUpdate(slot);
                    if(off.isEmpty())continue;
                    int moved=((RatContainer)getInventory()).collect(off,off.getCount(),false);off.shrink(moved);
                    if(!off.isEmpty())spawnAtLocation(off);
                }
                com.github.alexthe666.rats.server.brick.BrickWork.clearArmor(this);
            }
            entityData.set(BRICK,!isBrick());
            containerChanged(getInventory());
            player.displayClientMessage(Component.translatable(isBrick()?"brick.rats.transformed":"brick.rats.restored"),true);
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME,0.8F,1.0F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    private static final EntityDataAccessor<Optional<GlobalPos>> WORK_ANCHOR = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
 private static final EntityDataAccessor<Optional<GlobalPos>> CREATE_WORK_POS = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
    private static final EntityDataAccessor<Optional<GlobalPos>> BOUND_HOUSE_POS = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
    private static final EntityDataAccessor<Integer> RAT_EXPERIENCE = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> PROFILE_ATTACK = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Component> SPELL_BATTLE_STATUS=SynchedEntityData.defineId(TamedRat.class,EntityDataSerializers.COMPONENT);
    public Component spellBattleStatus(){return entityData.get(SPELL_BATTLE_STATUS);}
    public void spellBattleStatus(String key,long count){entityData.set(SPELL_BATTLE_STATUS,Component.translatable("rats.spell_mode."+key,(int)count));}
    public boolean canUseWeaponUpgrades(){return getCommand()!=RatCommand.SPELL_COMBAT;}
    public void syncPhysicalAttackGoal(){
        if(attackGoal==null||level().isClientSide())return;
        goalSelector.removeGoal(attackGoal);
        if(getCommand()!=RatCommand.SPELL_COMBAT)goalSelector.addGoal(1,attackGoal);
    }
    private static final EntityDataAccessor<Boolean> RAT_GENES_INITIALIZED = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> GENE_SPEED = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GENE_HEALTH = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GENE_ARMOR = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GENE_DAMAGE = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GENE_REGEN = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GENE_GOETY = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GENE_COOLDOWN = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GENE_IRON = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> LAST_PATH_FAILURE = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> CHEESE_RESERVE_SYNC = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHEESE_EATEN_TODAY_SYNC = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LAST_FEED_DAY_SYNC = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
    private static final String RAT_GENES_TAG = "RatGenes";
    public Direction createWorkFacing = Direction.UP;
    private int cheeseEatenToday = 0;
    private long lastFeedDay = -1L;
    private int cachedHungerEfficiencyTick = -1000;
    private float cachedHungerEfficiency = 0.0f;
    private int lastSmoothMoveTick = -100;
    private double lastSmoothMoveX = Double.NaN;
    private double lastSmoothMoveY = Double.NaN;
    private double lastSmoothMoveZ = Double.NaN;
    private Entity lastSmoothMoveEntity;

	private static final EntityDataAccessor<Boolean> TOGA = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> VISUAL_FLAG = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DANCE_MOVES = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HELD_RF = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RESPAWN_COUNTDOWN = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<GlobalPos>> PICKUP_POS = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
	private static final EntityDataAccessor<Optional<GlobalPos>> DEPOSIT_POS = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
	private static final EntityDataAccessor<Boolean> IS_IN_WHEEL = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> SPECIAL_DYE = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> MOUNT_RESPAWN_COOLDOWN = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);

	public Direction depositFacing = Direction.UP;
	public Direction pickupFacing = Direction.UP;
	public boolean crafting = false;
	public boolean climbingTube = false;
	public int cookingProgress = 0;
	public int coinCooldown = 0;
	public int breedCooldown = 0;
	public BlockPos jukeboxPos;
	public FluidStack transportingFluid = FluidStack.EMPTY;
	private Goal harvestGoal;
	private Goal pickupGoal;
	private Goal depositGoal;
	private Goal attackGoal;
	private Goal targetGoal;
	/*
	   0 = tamed navigator
	   1 = flight navigator
	   2 = tube navigator
	   3 = aquatic navigator
	   4 = ethereal navigator
	   5 = cage navigator
	 */
	protected int navigatorType;
	private int upgradeNavigatorType;
	public int rangedAttackCooldown = 0;
	public int visualCooldown = 0;
	private int poopCooldown = 0;
	public int pickpocketCooldown = 0;
	public int randomEffectCooldown = 0;
	public boolean isCurrentlyWorking;
	private final Multimap<Attribute, AttributeModifier> attributeChanges = HashMultimap.create();

	public TamedRat(EntityType<? extends TamableAnimal> type, Level level) {
		super(type, level);
		Arrays.fill(this.armorDropChances, 2.0F);
		Arrays.fill(this.handDropChances, 2.0F);
		this.xpReward = 0;
	}

	@Override
	public float maxUpStep() {
		return 1.0F;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 8.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.35D)
				.add(Attributes.FLYING_SPEED, 0.35D)
				.add(Attributes.ATTACK_DAMAGE, 1.0D)
				.add(Attributes.FOLLOW_RANGE, 12.0D);
	}

	@Override
	protected void registerGoals() {
        goalSelector.addGoal(0,new com.github.alexthe666.rats.server.brick.BrickCookGoal(this));
        goalSelector.addGoal(0,new com.github.alexthe666.rats.server.brick.BrickJobGoal(this));
this.goalSelector.addGoal(1,new RatSeekCheeseGoal(this));
this.goalSelector.addGoal(2,new RatReturnToHouseGoal(this,1.2D));
if(RatsMod.CREATE_LOADED)this.goalSelector.addGoal(2,new CreateWorkerGoal(this));
if(RatsMod.FARMERS_DELIGHT_LOADED)this.goalSelector.addGoal(2,new FarmersDelightChefGoal(this));

		this.harvestGoal = new RatHarvestCropsGoal(this);
		this.pickupGoal = new RatPickupGoal(this, RatPickupGoal.PickupType.INVENTORY);
		this.depositGoal = new RatDepositGoal(this, RatDepositGoal.DepositType.INVENTORY);
		this.attackGoal = new RatMeleeAttackGoal(this, 1.3D, true);
		this.targetGoal = new RatHuntGoal(this, true, new ArrayList<>());
		this.goalSelector.addGoal(0, new RatFloatGoal(this));
        this.goalSelector.addGoal(1,new com.github.alexthe666.rats.server.entity.ai.goal.SpellCombatGoal(this));
		registerSongGoal();
		if(getCommand()!=RatCommand.SPELL_COMBAT)this.goalSelector.addGoal(1, this.attackGoal);
		this.goalSelector.addGoal(2, new RatFollowOwnerGoal(this, 1.25D, 10.0F, 3.0F));
		this.goalSelector.addGoal(2, new RatStayCloseToHomeGoal(this, 1.1D));
		this.goalSelector.addGoal(3, this.harvestGoal);
		this.goalSelector.addGoal(4, this.depositGoal);
		this.goalSelector.addGoal(5, this.pickupGoal);
		this.goalSelector.addGoal(6, new RatTargetItemsGoal(this));
		this.goalSelector.addGoal(7, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(8, new RatWanderGoal(this, 1.25D));
		this.goalSelector.addGoal(8, new RatPatrolGoal(this));
		this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, LivingEntity.class, 6.0F) {
			@Override
			public boolean canUse() {
				return ((TamedRat) this.mob).sleepProgress <= 0.0F && super.canUse();
			}
		});
		this.goalSelector.addGoal(10, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				return ((TamedRat) this.mob).sleepProgress <= 0.0F && super.canUse();
			}
		});
		this.targetSelector.addGoal(1, this.targetGoal);
		this.targetSelector.addGoal(2, new RatOwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(3, new RatOwnerHurtTargetGoal(this));
	}

	protected void setupDynamicAI() {
		AtomicReference<Goal> newHarvest = new AtomicReference<>(new RatHarvestCropsGoal(this));
		AtomicReference<Goal> newDeposit = new AtomicReference<>(new RatDepositGoal(this, RatDepositGoal.DepositType.INVENTORY));
		AtomicReference<Goal> newPickup = new AtomicReference<>(new RatPickupGoal(this, RatPickupGoal.PickupType.INVENTORY));
		AtomicReference<Goal> newAttack = new AtomicReference<>(new RatMeleeAttackGoal(this, 1.45D, true));

		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof ChangesAIUpgrade, (stack, slot) ->
				((ChangesAIUpgrade) stack.getItem()).addNewWorkGoals(this).forEach(goal -> {
					if (!(goal instanceof RatWorkGoal workGoal)) {
						throw new IllegalArgumentException("Rat Goals must implement the interface RatWorkGoal! Goal" + goal.getClass().getName() + "doesnt do this!");
					}
					switch (workGoal.getRatTaskType()) {
						case ATTACK -> newAttack.set(goal);
						case DEPOSIT -> newDeposit.set(goal);
						case PICKUP -> newPickup.set(goal);
						case HARVEST -> newHarvest.set(goal);
					}
				})
		);

		if (!this.level().isClientSide()) {
			this.goalSelector.removeGoal(this.harvestGoal);
			this.goalSelector.removeGoal(this.depositGoal);
			this.goalSelector.removeGoal(this.pickupGoal);
			this.goalSelector.removeGoal(this.attackGoal);

			this.attackGoal = newAttack.get();
			this.depositGoal = newDeposit.get();
			this.pickupGoal = newPickup.get();
			this.harvestGoal = newHarvest.get();

			if(getCommand()!=RatCommand.SPELL_COMBAT)this.goalSelector.addGoal(1, this.attackGoal);
			this.goalSelector.addGoal(3, this.depositGoal);
			this.goalSelector.addGoal(4, this.pickupGoal);
			this.goalSelector.addGoal(5, this.harvestGoal);

			this.targetSelector.removeGoal(this.targetGoal);
			ItemStack filterUpgrade = RatUpgradeUtils.getUpgrade(this, RatsItemRegistry.RAT_UPGRADE_MOB_FILTER.get());

			if (!filterUpgrade.isEmpty()) {
				this.targetGoal = new RatHuntGoal(this, MobFilterUpgradeItem.isWhitelist(filterUpgrade), MobFilterUpgradeItem.getSelectedMobs(filterUpgrade));
			} else {
				this.targetGoal = new RatHuntGoal(this, true, new ArrayList<>());
			}

			this.targetSelector.addGoal(1, this.targetGoal);

			this.upgradeNavigatorType = this.hasFlightUpgrade() ? 1
					: RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_AQUATIC.get()) ? 3
					: RatUpgradeUtils.hasUpgrade(this, RatlantisItemRegistry.RAT_UPGRADE_ETHEREAL.get()) ? 4 : 0;
			updateNavigationMode();
		}
	}

	@Override
	protected void defineSynchedData() {
        entityData.define(BRICK,false);
        entityData.define(EVOLUTION,RatEvolution.NONE.ordinal());
        entityData.define(BRICK_COOKING,false);
        entityData.define(BRICK_ACTION,(byte)0);
getEntityData().define(WORK_ANCHOR,Optional.empty());

        this.getEntityData().define(CREATE_WORK_POS, Optional.empty());
        this.getEntityData().define(BOUND_HOUSE_POS, Optional.empty());
        entityData.define(RAT_EXPERIENCE, 0);
        entityData.define(PROFILE_ATTACK, 1.0F);
        this.entityData.define(SPELL_BATTLE_STATUS,Component.translatable("rats.spell_mode.search",0));
        this.getEntityData().define(RAT_GENES_INITIALIZED, false);
        this.getEntityData().define(GENE_SPEED, Float.valueOf(1.0f));
        this.getEntityData().define(GENE_HEALTH, Float.valueOf(1.0f));
        this.getEntityData().define(GENE_ARMOR, Float.valueOf(1.0f));
        this.getEntityData().define(GENE_DAMAGE, Float.valueOf(1.0f));
        this.getEntityData().define(GENE_REGEN, Float.valueOf(1.0f));
        this.getEntityData().define(GENE_GOETY, Float.valueOf(1.0f));
        entityData.define(GENE_COOLDOWN,1.0f);
        this.getEntityData().define(GENE_IRON, Float.valueOf(1.0f));
        this.getEntityData().define(LAST_PATH_FAILURE, "");
        this.getEntityData().define(CHEESE_EATEN_TODAY_SYNC, 0);
        this.getEntityData().define(CHEESE_RESERVE_SYNC, 0);
        this.getEntityData().define(LAST_FEED_DAY_SYNC, -1);

		super.defineSynchedData();
		this.getEntityData().define(TOGA, false);
		this.getEntityData().define(DANCING, false);
		this.getEntityData().define(DANCE_MOVES, 0);
		this.getEntityData().define(HELD_RF, 0);
		this.getEntityData().define(RESPAWN_COUNTDOWN, 0);
		this.getEntityData().define(VISUAL_FLAG, false);
		this.getEntityData().define(FLYING, false);
		this.getEntityData().define(DEPOSIT_POS, Optional.empty());
		this.getEntityData().define(PICKUP_POS, Optional.empty());
		this.getEntityData().define(IS_IN_WHEEL, false);
		this.getEntityData().define(SPECIAL_DYE, "rainbow");
		this.getEntityData().define(MOUNT_RESPAWN_COOLDOWN, 20);
	}

	@Override
	protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
		return new RatNavigation(this, level);
	}

	private void updateNavigationMode() {
		if (isTubeTransitActive()) return;
		var block = level().getBlockState(blockPosition()).getBlock();
		switchNavigator(block instanceof RatCageBlock ? 5 : block instanceof RatTubeBlock ? 2 : upgradeNavigatorType);
	}

	public void switchNavigator(int type) {
		if (type == this.navigatorType) return;
		if (type == 0) { //tamed
			this.moveControl = new RatMoveControl(this);
			this.navigation = new RatNavigation(this, this.level());
			this.navigatorType = 0;
		} else if (type == 1) { //flying
			this.moveControl = new RatFlightMoveControl(this, 1.0F);
			this.navigation = new RatFlightNavigation(this, this.level());
			this.navigatorType = 1;
		} else if (type == 2) { //tube
			this.moveControl = new RatTubeMoveControl(this);
			this.navigation = new RatNavigation(this, this.level());
			this.navigatorType = 2;
		} else if (type == 3) { //aquatic
			this.moveControl = new SmoothSwimmingMoveControl(this, 360, 360, 10.0F, 1.0F, true);
			this.navigation = new AmphibiousPathNavigation(this, this.level());
			this.navigatorType = 3;
		} else if (type == 4) { //ethereal
			this.moveControl = new EtherealRatMoveControl(this);
			this.navigation = new EtherealRatNavigation(this, this.level());
			this.navigatorType = 4;
		} else if (type == 5) { //cage
			this.moveControl = new RatCageMoveControl(this);
			this.navigation = new RatNavigation(this, this.level());
			this.navigatorType = 5;
		}
	}

	@Override
	public boolean isHoldingFood() {
		if (!this.getCommand().allowsEating && !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_ORE_DOUBLING.get()))
			return false;
		return !this.getMainHandItem().isEmpty() && RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.isRatHoldingFood(this), RatUtils.isRatFood(this.getMainHandItem()));
	}

	@Override
	public boolean isVisuallySitting() {
		return super.isVisuallySitting() || this.isDancing();
	}

	@Override
	public boolean isHoldingItemInHands() {
		return (this.isHoldingFood() || (!this.getMainHandItem().isEmpty() && this.cookingProgress > 0) || this.holdsItemInHandUpgrade() || this.getMBTransferRate() > 0) && this.sleepProgress <= 0.0F;
	}

	@Override
	public boolean doHurtTarget(Entity entity) {
        if(getCommand()==RatCommand.SPELL_COMBAT)return false;
		if (this.getVehicle() instanceof LivingEntity living && this.isRidingSpecialMount()) {
			return living.doHurtTarget(entity);
		}
		boolean flag = entity.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
		if (flag) {
			this.doEnchantDamageEffects(this, entity);
			this.getMainHandItem().hurtAndBreak(1, this, rat -> rat.broadcastBreakEvent(EquipmentSlot.MAINHAND));
			RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof PostAttackUpgrade, (stack, slot) -> ((PostAttackUpgrade) stack.getItem()).afterHit(this, (LivingEntity) entity));
		}
		return flag;
	}

	public boolean isInCage() {
		return this.level().getBlockState(this.blockPosition()).getBlock() instanceof RatCageBlock;
	}

	public boolean isInTube() {
		return this.level().getBlockState(this.blockPosition()).getBlock() instanceof RatTubeBlock;
	}

	@Override
	public boolean onClimbable() {
		return this.isInTube() ? this.climbingTube : super.onClimbable();
	}

	@Override
	public int getArmorValue() {
		return super.getArmorValue() * RatConfig.ratArmorMultiplier;
	}

	@Override
	public boolean removeWhenFarAway(double dist) {
		return false;
	}

	@Override
	protected MovementEmission getMovementEmission() {
		return RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_SCULKED.get()) ? MovementEmission.SOUNDS : MovementEmission.ALL;
	}

	@Override
	public boolean canDrownInFluidType(FluidType type) {
		return type == ForgeMod.WATER_TYPE.get() && (!RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_AQUATIC.get()) || !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_UNDERWATER.get()));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
        legion.save(tag);
        if(getEvolution()!=RatEvolution.NONE)tag.putString("RatEvolution",getEvolution().id);
        tag.putBoolean("BrickForm",isBrick()); kitchen.save(tag); work.save(tag);
getWorkAnchorPoint().flatMap(p -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE,p).result()).ifPresent(t -> tag.put("WorkAnchor",t));

		tag.putInt("RatExperience", getRatExperience());
this.saveRatGenes(tag); tag.putInt("CheeseEatenToday",this.cheeseEatenToday); tag.putLong("LastFeedDay",this.lastFeedDay); tag.putInt("CreateWorkFacing",this.createWorkFacing.ordinal());
this.getCreateWorkPos().flatMap(p -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE,p).result()).ifPresent(t -> tag.put("CreateWorkPos",t));
this.getBoundHousePos().flatMap(p -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE,p).result()).ifPresent(t -> tag.put("BoundHousePos",t));

		super.addAdditionalSaveData(tag);
		tag.putInt("CookingProgress", this.cookingProgress);
		tag.putInt("BreedCooldown", this.breedCooldown);
		tag.putInt("CoinCooldown", this.coinCooldown);
		tag.putInt("PickpocketCooldown", this.pickpocketCooldown);
		tag.putInt("MountCooldown", this.getMountCooldown());
		tag.putInt("TransportingRF", this.getHeldRF());
		tag.putInt("RespawnCountdown", this.getRespawnCountdown());
		tag.putInt("Command", this.getCommandInteger());
		tag.putBoolean("VisualFlag", this.getVisualFlag());
		tag.putBoolean("Dancing", this.isDancing());
		tag.putBoolean("Toga", this.hasToga());
		tag.putString("SpecialDye", this.getSpecialDye());
		this.getDepositPos().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error)).ifPresent(tag1 -> tag.put("DepositPos", tag1));
		this.getPickupPos().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error)).ifPresent(tag1 -> tag.put("PickupPos", tag1));
		tag.putInt("RandomEffectCooldown", this.randomEffectCooldown);
		if (this.transportingFluid != null) {
			CompoundTag fluidTag = new CompoundTag();
			this.transportingFluid.writeToNBT(fluidTag);
			tag.put("TransportingFluid", fluidTag);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
        legion.load(tag);
        entityData.set(EVOLUTION,RatEvolution.fromSaved(tag.getString("RatEvolution")).ordinal());
        entityData.set(BRICK,tag.getBoolean("BrickForm")); kitchen.load(tag); work.load(tag);
if(tag.contains("WorkAnchor"))setWorkAnchorPoint(GlobalPos.CODEC.parse(NbtOps.INSTANCE,tag.get("WorkAnchor")).result().orElse(null));

		entityData.set(RAT_EXPERIENCE, Mth.clamp(tag.getInt("RatExperience"), 0, RatProgression.MAX_EXPERIENCE));
this.loadRatGenes(tag);this.cheeseEatenToday=tag.getInt("CheeseEatenToday");this.lastFeedDay=tag.contains("LastFeedDay")?tag.getLong("LastFeedDay"):-1L;this.syncRatHungerData();
if(tag.contains("CreateWorkPos"))this.setCreateWorkPos(GlobalPos.CODEC.parse(NbtOps.INSTANCE,tag.get("CreateWorkPos")).result().orElse(null));
if(tag.contains("BoundHousePos"))this.setBoundHousePos(GlobalPos.CODEC.parse(NbtOps.INSTANCE,tag.get("BoundHousePos")).result().orElse(null));this.createWorkFacing=this.readSavedDirection(tag,"CreateWorkFacing",Direction.UP);

		super.readAdditionalSaveData(tag);
		this.cookingProgress = tag.getInt("CookingProgress");
		this.breedCooldown = tag.getInt("BreedCooldown");
		this.coinCooldown = tag.getInt("CoinCooldown");
		this.pickpocketCooldown = tag.getInt("PickpocketCooldown");
		this.setMountCooldown(tag.getInt("MountCooldown"));
		this.randomEffectCooldown = tag.getInt("RandomEffectCooldown");
		this.setHeldRF(tag.getInt("TransportingRF"));
		this.setRespawnCountdown(tag.getInt("RespawnCountdown"));
		this.setCommandInteger(tag.getInt("Command"));
		this.setDancing(tag.getBoolean("Dancing"));
		this.setVisualFlag(tag.getBoolean("VisualFlag"));
		this.setToga(tag.getBoolean("Toga"));
		this.setSpecialDye(tag.getString("SpecialDye"));
		if (tag.contains("DepositPos")) {
			this.setDepositPos(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("DepositPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
		}
		if (tag.contains("PickupPos")) {
			this.setPickupPos(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("PickupPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
		}
		if (tag.contains("DepositFacing")) {
			this.depositFacing = Direction.values()[tag.getInt("DepositFacing")];
		}
		if (tag.contains("PickupFacing")) {
			this.pickupFacing = Direction.values()[tag.getInt("PickupFacing")];
		}
		if (tag.contains("TransportingFluid")) {
			CompoundTag fluidTag = tag.getCompound("TransportingFluid");
			if (!fluidTag.isEmpty()) {
				this.transportingFluid = FluidStack.loadFluidStackFromNBT(fluidTag);
			}
		}
		if (!level().isClientSide() && getEntityData().get(RAT_GENES_INITIALIZED)) {
            rebuildRatAttributes();
            if (tag.contains("Health", Tag.TAG_ANY_NUMERIC)) setHealth(tag.getFloat("Health"));
        }
	}

    private ItemStack spellMedium;
    /** Native mob spells inspect the main hand while handling a cast. Keep the real inventory untouched. */
    public <V> V withSpellMedium(ItemStack medium,java.util.function.Supplier<V> action) {
        ItemStack previous=spellMedium;
        spellMedium=medium;
        try {return action.get();} finally {spellMedium=previous;}
    }
    @Override public ItemStack getItemBySlot(EquipmentSlot slot) {
        if(slot==EquipmentSlot.MAINHAND && spellMedium!=null)return spellMedium;
        return super.getItemBySlot(slot);
    }
	public final RatLegion legion = new RatLegion();

	@Override
	public void aiStep() {
        if (level() instanceof net.minecraft.server.level.ServerLevel world){Mastery.tick(this);legion.tick(this,world);}
        if(!level().isClientSide())getEvolution().tickMovement(this);
        if (!level().isClientSide() && tickCount % 5 == 0)
            entityData.set(PROFILE_ATTACK, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
if (!level().isClientSide() && tickCount % 10 == 0 && getMainHandItem().isEmpty() && isTargetCommand()) {
			for (int i = RatContainer.BACKPACK; i < RatContainer.SIZE; i++) {
				ItemStack stack = getInventory().getItem(i);
				if (!stack.isEmpty() && !stack.canEquip(EquipmentSlot.HEAD, this) && !RatLegion.isCarrier(stack) && !shouldDepositItem(stack)) {
					setItemInHand(InteractionHand.MAIN_HAND, getInventory().removeItemNoUpdate(i));
					break;
				}
			}
		}
if (!this.level().isClientSide()) { this.ensureRatGenesInitialized("tick"); this.tickRatHunger(); this.getEntityData().set(CHEESE_RESERVE_SYNC,this.getCheeseReserveCount()); if(this.isTubeTransitActive()) RatTubeTransit.tick(this); }

		super.aiStep();
		if (this.isBaby() && this.getCommand() != RatCommand.SIT) {
			this.setCommand(RatCommand.SIT);
		}
		if (this.breedCooldown > 0) {
			this.breedCooldown--;
		}
		if (this.isOrderedToSit() && this.getTarget() != null) {
			this.setTarget(null);
		}

		if (this.getTarget() != null && this.getMountCooldown() > 0) {
			this.setTarget(null);
		}

		if (this.getRespawnCountdown() > 0) {
			if (this.level().isClientSide() && this.tickCount % 5 == 0) {
				double d0 = this.position().x();
				double d1 = this.position().y() + 0.25D;
				double d2 = this.position().z();
				double d3 = ((double) this.getRandom().nextFloat() - 0.5D) * 0.15D;
				double d4 = ((double) this.getRandom().nextFloat() - 0.5D) * 0.15D;
				double d5 = ((double) this.getRandom().nextFloat() - 0.5D) * 0.15D;
				this.level().addParticle(ParticleTypes.END_ROD, d0, d1, d2, d3, d4, d5);
			}
			this.setRespawnCountdown(this.getRespawnCountdown() - 1);
		}
		this.setNoAi(this.getRespawnCountdown() > 0);

		if (!this.level().isClientSide() && this.getMountEntityType() != null && !this.isPassenger() && this.getMountCooldown() == 0) {
			Entity entity = this.getMountEntityType().create(this.level());
			entity.copyPosition(this);
			if (entity instanceof Mob mob && this.level() instanceof ServerLevelAccessor accessor) {
				ForgeEventFactory.onFinalizeSpawn(mob, accessor, this.level().getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
			}
			this.level().addFreshEntity(entity);

			this.level().broadcastEntityEvent(this, (byte) 127);
			this.startRiding(entity, true);
		}

		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof TickRatUpgrade, (stack, slot) -> ((TickRatUpgrade) stack.getItem()).tick(this));

		if (RatConfig.upgradeRegenRate > 0) {
			RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof StatBoostingUpgrade, (stack, slot) -> {
				if (((StatBoostingUpgrade) stack.getItem()).regeneratesHealth() && this.getHealth() < this.getMaxHealth() && this.tickCount % RatConfig.upgradeRegenRate == 0) {
					this.heal(this.getRatRegenGene() * this.getRatHungerEfficiency());
				}
			});
		}

		if (!this.level().isClientSide()) updateNavigationMode();

		this.setNoGravity(this.isFlying());
		if (this.isFlying()) {
			if (this.isOrderedToSit() || this.verticalCollisionBelow || this.onGround()) {
				this.setFlying(false);
			}
			if (Math.abs(this.getDeltaMovement().x()) < 0.01D && Math.abs(this.getDeltaMovement().z()) < 0.01D) {
				if (Math.abs(this.getDeltaMovement().y()) > 0.0D) {
					this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.1D, 1.0D));
				}
			}
		}

		if (this.isInWheel() && !this.level().getBlockState(this.blockPosition()).is(RatsBlockRegistry.RAT_CAGE_WHEEL.get())) {
			this.setInWheel(false);
		}

		if (this.getMountCooldown() > 0) {
			this.setMountCooldown(this.getMountCooldown() - 1);
		}
		if (this.rangedAttackCooldown > 0) {
			this.rangedAttackCooldown--;
		}
		if (this.visualCooldown > 0) {
			this.visualCooldown--;
		}
		if (this.poopCooldown > 0) {
			this.poopCooldown--;
		}
		if (this.isDancing() && this.getAnimation() != this.getDanceAnimation()) {
			this.setAnimation(this.getDanceAnimation());
		}
		if (this.isDancing() && (this.jukeboxPos == null || this.jukeboxPos.distToCenterSqr(this.getX(), this.getY(), this.getZ()) > 256.0D || !this.level().getBlockState(this.jukeboxPos).is(Blocks.JUKEBOX))) {
			this.setDancing(false);
		}
		if (!this.level().isClientSide() && this.level().getBlockState(this.blockPosition()).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) && this.level().isEmptyBlock(this.blockPosition().above())) {
			this.setPos(this.getX(), this.getY() + 1, this.getZ());
			this.getNavigation().stop();
		}

		if (this.jumping && !this.level().isClientSide() && this.level().getBlockState(this.blockPosition().above()).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) && this.level().isEmptyBlock(this.blockPosition().above(2))) {
			this.setPos(this.getX(), this.getY() + 1, this.getZ());
			this.getNavigation().stop();
		}
	}

	@Override
	public boolean fireImmune() {
		return this.getRespawnCountdown() > 0 || super.fireImmune();
	}

	@Override
	public boolean isPushedByFluid(FluidType type) {
		return !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_QUARRY.get()) && !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_AQUATIC.get()) && super.isPushedByFluid(type);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isBaby() || this.isInvulnerableTo(source) || (source.is(DamageTypes.IN_WALL) && this.isPassenger())) {
			return false;
		} else {
			Entity entity = source.getEntity();

			if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
				amount = (amount + 1.0F) / 2.0F;
			}

			boolean flag = super.hurt(source, amount);

			if (flag && this.getVehicle() != null && this.isRidingSpecialMount()) {
				this.getVehicle().hurt(source, amount);
				this.invulnerableTime = 20;
				return false;
			}

			return flag;
		}
	}

	@Override
	public ItemStack getPickedResult(HitResult target) {
		return new ItemStack(ForgeSpawnEggItem.fromEntityType(RatsEntityRegistry.RAT.get()));
	}

	public void setFlying(boolean flying) {
		this.getEntityData().set(FLYING, flying);
	}

	public boolean isFlying() {
		return this.getEntityData().get(FLYING);
	}

	public void setToga(boolean toga) {
		this.getEntityData().set(TOGA, toga);
	}

	public boolean hasToga() {
		return this.getEntityData().get(TOGA);
	}

	public boolean getVisualFlag() {
		return this.getEntityData().get(VISUAL_FLAG);
	}

	public void setVisualFlag(boolean flag) {
		this.getEntityData().set(VISUAL_FLAG, flag);
	}

	public boolean isDancing() {
		return this.getEntityData().get(DANCING);
	}

	public void setDancing(boolean dancing) {
		this.getEntityData().set(DANCING, dancing);
	}

	public int getDanceMoves() {
		return this.getEntityData().get(DANCE_MOVES);
	}

	public void setDanceMoves(int moves) {
		this.getEntityData().set(DANCE_MOVES, moves);
	}

	public int getHeldRF() {
		return this.getEntityData().get(HELD_RF);
	}

	public void setHeldRF(int rf) {
		this.getEntityData().set(HELD_RF, rf);
	}

	public int getRespawnCountdown() {
		return this.getEntityData().get(RESPAWN_COUNTDOWN);
	}

	public void setRespawnCountdown(int respawn) {
		this.getEntityData().set(RESPAWN_COUNTDOWN, respawn);
	}

	public boolean isInWheel() {
		return this.getEntityData().get(IS_IN_WHEEL);
	}

	public void setInWheel(boolean wheel) {
		this.getEntityData().set(IS_IN_WHEEL, wheel);
	}

	public String getSpecialDye() {
		return this.getEntityData().get(SPECIAL_DYE);
	}

	public void setSpecialDye(String keyword) {
		this.getEntityData().set(SPECIAL_DYE, keyword);
	}


	public Optional<GlobalPos> getPickupPos() {
		return this.getEntityData().get(PICKUP_POS);
	}

	public void setPickupPos(@Nullable GlobalPos pos) {
		this.getEntityData().set(PICKUP_POS, Optional.ofNullable(pos));
	}

	public Optional<GlobalPos> getDepositPos() {
		return this.getEntityData().get(DEPOSIT_POS);
	}

	public void setDepositPos(@Nullable GlobalPos pos) {
		this.getEntityData().set(DEPOSIT_POS, Optional.ofNullable(pos));
	}

	public int getMountCooldown() {
		return this.getEntityData().get(MOUNT_RESPAWN_COOLDOWN);
	}

	public void setMountCooldown(int cooldown) {
		this.getEntityData().set(MOUNT_RESPAWN_COOLDOWN, cooldown);
	}

	@Override
	public void onItemEaten() {
if (isCheeseReserveItem(this.getMainHandItem()) && !this.level().isClientSide()) this.recordRatCheeseMeal("hand");

		ItemStack held = this.getMainHandItem();
		ItemStack attemptedSmelt = RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_ORE_DOUBLING.get())
				? OreDoublingRatUpgradeItem.getProcessingResult(this.level(), held) : ItemStack.EMPTY;
		if (!attemptedSmelt.isEmpty()) {
			if (!attemptedSmelt.is(held.getItem())) {
				ItemStack nugget = OreRatNuggetItem.saveResourceToNugget(attemptedSmelt).copyWithCount(2);
				if (RatConfig.ratFartNoises) {
					this.playSound(RatsSoundRegistry.RAT_POOP.get(), 0.5F + this.getRandom().nextFloat() * 0.5F, 1.0F + this.getRandom().nextFloat() * 0.5F);
				}
				if (!this.level().isClientSide()) {
					this.spawnAtLocation(nugget, 0.0F);
				}
				this.getMainHandItem().shrink(1);
			}
		} else if (this.getRandom().nextFloat() <= 0.05F) {
			ItemStack nugget = new ItemStack(RatsItemRegistry.RAT_NUGGET.get());
			if (RatConfig.ratFartNoises) {
				this.playSound(RatsSoundRegistry.RAT_POOP.get(), 0.5F + this.getRandom().nextFloat() * 0.5F, 1.0F + this.getRandom().nextFloat() * 0.5F);
			}
			if (!this.level().isClientSide()) {
				this.spawnAtLocation(nugget, 0.0F);
			}
		} else {
			this.getMainHandItem().shrink(1);
		}
	}

    public void createBabiesFrom(TamedRat mother, TamedRat father) {
        TamedRat baby = new TamedRat((EntityType<? extends TamableAnimal>)((EntityType)RatsEntityRegistry.TAMED_RAT.get()), this.level());
        baby.setMale(this.getRandom().nextBoolean());
        baby.setColorVariant(this.chooseBabyColorVariant(mother, father));
        baby.setPos(mother.getX() - 0.5 + (double)mother.getRandom().nextFloat(), mother.getY(), mother.getZ() - 0.5 + (double)mother.getRandom().nextFloat());
        baby.setAge(-2400);
        baby.setCommand(RatCommand.SIT);
        if (mother.isTame()) {
            baby.setTame(true);
            baby.setOwnerUUID(mother.getOwnerUUID());
        } else if (father.isTame()) {
            baby.setTame(true);
            baby.setOwnerUUID(father.getOwnerUUID());
        }
        baby.inheritRatGenesFromParents(mother, father);
        this.level().addFreshEntity((Entity)baby);
        this.tryInheritBabyUpgrade(father, mother, baby);
    }

	@Override
	public boolean isPickable() {
		return !(this.getVehicle() instanceof Player);
	}

	public ItemStack getResultForRecipe(RecipeType<? extends SingleItemRecipe> recipe, ItemStack stack) {
		Optional<? extends SingleItemRecipe> optional = this.level().getRecipeManager().getRecipeFor(recipe, new SimpleContainer(stack), this.level());
		if (optional.isPresent()) {
			ItemStack itemstack = optional.get().getResultItem(this.level().registryAccess());
			if (!itemstack.isEmpty()) {
				ItemStack itemstack1 = itemstack.copy();
				itemstack1.setCount(stack.getCount() * itemstack.getCount());
				return itemstack1;
			}
		}
		return ItemStack.EMPTY;
	}

	public boolean tryDepositItemInContainers(ItemStack burntItem) {
		if (this.level().getBlockEntity(this.blockPosition()) != null) {
			BlockEntity te = this.level().getBlockEntity(this.blockPosition());
			if (te != null) {
				LazyOptional<IItemHandler> handler = te.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
				if (handler.resolve().isPresent()) {
					if (ItemHandlerHelper.insertItem(handler.resolve().get(), burntItem, true).isEmpty()) {
						ItemHandlerHelper.insertItem(handler.resolve().get(), burntItem, false);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
		if (!this.crafting) {
			entity.push(this);
		}
	}

	public Animation getDanceAnimation() {
		if (this.getDanceMoves() == 0) {
			return ANIMATION_DANCE;
		}
		return NO_ANIMATION;
	}

	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean playerKill) {
		if (this.hasToga()) {
			this.spawnAtLocation(new ItemStack(RatlantisItemRegistry.RAT_TOGA.get()), 0.0F);
		}
		for (int i = 7; i < RatContainer.SIZE; i++) {
			ItemStack stack = getInventory().removeItemNoUpdate(i);
			if (!stack.isEmpty()) spawnAtLocation(stack);
		}
		kitchen.dropCargo();
		super.dropCustomDeathLoot(source, looting, playerKill);
	}

	public void spawnAngelCopy() {
		TamedRat copy = RatsEntityRegistry.TAMED_RAT.get().create(this.level());
		CompoundTag tag = new CompoundTag();
		this.addAdditionalSaveData(tag);
		tag.putShort("HurtTime", (short) 0);
		tag.putInt("HurtByTimestamp", 0);
		tag.putShort("DeathTime", (short) 0);
		copy.readAdditionalSaveData(tag);
		copy.setHealth(copy.getMaxHealth());
		copy.copyPosition(this);
		copy.setRespawnCountdown(1200);
		if (copy.isVisuallySitting()) {
			copy.sitProgress = 20.0F;
		}
		if (this.hasCustomName()) {
			copy.setCustomName(this.getCustomName());
		}
		copy.clearFire();
		this.level().addFreshEntity(copy);
	}

	@Override
	public boolean canBeAffected(MobEffectInstance instance) {
        if(getEvolution()==RatEvolution.VENOM && instance.getEffect()==net.minecraft.world.effect.MobEffects.POISON)return false;
		if (instance.getEffect() == MobEffects.POISON && (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_POISON.get()) || RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_DAMAGE_PROTECTION.get()))) {
			return false;
		}
		return super.canBeAffected(instance);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).getItem() instanceof com.github.alexthe666.rats.server.items.RatPressItem press)
			return press.capture(player, this);
        ItemStack offered=player.getItemInHand(hand);
        if(offered.getItem() instanceof LegionBaton baton) return baton.assign(player,this,offered);
        RatEvolution evolution=RatEvolution.forStone(offered);
        if(evolution!=RatEvolution.NONE)return useEvolutionStone(player,offered,evolution);
        if(player.getItemInHand(hand).is(RatsItemRegistry.BRICK_CONVERTER.get())) return toggleBrick(player);
        if(player.getItemInHand(hand).is(RatsItemRegistry.KING_SCEPTER.get())) return com.github.alexthe666.rats.server.brick.KingScepterItem.useOnBrick(this,player,player.getItemInHand(hand));
        if (player.getItemInHand(hand).getItem() instanceof RatTrainingItem training)
            return training.feed(this, player, hand);
		if (player.getItemInHand(hand).is(RatsItemRegistry.RAT_MUSIC_NOTE.get())) return toggleSong(player);
		if (player.getItemInHand(hand).getItem() instanceof DyeItem) return dyeByPlayer(player, player.getItemInHand(hand));
if (this.isOwnedBy(player) && !player.isSecondaryUseActive() && isCheeseReserveItem(player.getItemInHand(hand)) && this.getCheeseEatenToday() < this.getDailyCheeseRequirement()) { if (!level().isClientSide()) { var feeding=player.getAbilities().instabuild?player.getItemInHand(hand).copy():player.getItemInHand(hand); feeding.shrink(1); this.recordRatCheeseMeal("owner"); } return InteractionResult.sidedSuccess(level().isClientSide()); }

		ItemStack itemstack = player.getItemInHand(hand);
		if (this.getRespawnCountdown() > 0 || itemstack.is(ForgeRegistries.ITEMS.getValue(new ResourceLocation(RatsMod.MODID, "rat_spawn_egg")))) {
			return InteractionResult.PASS;
		}
		if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_CARRAT.get())) {
			if (player.getFoodData().needsFood()) {
				player.getFoodData().eat(1, 0.1F);
				player.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
				for (int i = 0; i < 8; i++) {
					double d0 = this.getRandom().nextGaussian() * 0.02D;
					double d1 = this.getRandom().nextGaussian() * 0.02D;
					double d2 = this.getRandom().nextGaussian() * 0.02D;
					this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CARROT)), this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double) this.getBbHeight(), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d0, d1, d2);
				}
				if (RatConfig.carratDamagePerBite > 0.0D) {
					this.hurt(this.damageSources().playerAttack(player), (float) RatConfig.carratDamagePerBite);
				}
				return InteractionResult.SUCCESS;
			}
		}
		if (this.isBaby()) {
			if (itemstack.is(RatsItemTags.CHEESE_ITEMS)) {
				if (!player.getAbilities().instabuild) {
					itemstack.shrink(1);
				}
				this.ageUp(getSpeedUpSecondsWhenFeeding(-this.getAge()), true);
				return InteractionResult.sidedSuccess(this.level().isClientSide());
			}
			if (itemstack.is(RatsItemRegistry.CREATIVE_CHEESE.get())) {
				this.ageUp(-(this.getAge() / 20), true);
				return InteractionResult.sidedSuccess(this.level().isClientSide());
			}
		}
		if (!this.isBaby() && this.isOwnedBy(player)) {
			if (player.isSecondaryUseActive() && !this.isPassenger()) {
				if (player.getPassengers().size() < 3) {
					this.startRiding(player, true);
					player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_DISMOUNT), true);
				}
				return InteractionResult.sidedSuccess(this.level().isClientSide());
			}
			if (itemstack.is(RatsItemRegistry.RAT_PAPERS.get())) {
				InteractionResult result = itemstack.interactLivingEntity(player, this, hand);
				if (result.consumesAction()) {
					return result;
				}
			} else if (itemstack.is(RatlantisItemRegistry.RAT_TOGA.get())) {
				if (!this.hasToga()) {
					if (!player.isCreative()) {
						itemstack.shrink(1);
					}
				} else {
					if (!this.level().isClientSide()) {
						this.spawnAtLocation(new ItemStack(RatlantisItemRegistry.RAT_TOGA.get()), 0.0F);
					}
				}
				this.setToga(!this.hasToga());
				this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1F, 1.5F);
				return InteractionResult.SUCCESS;
			} else if (itemstack.is(RatsBlockRegistry.DYE_SPONGE.get().asItem()) && this.isDyed()) {
				this.setDyed(false);
				this.setDyeColor(0);
				this.setSpecialDye("");
				for (int i = 0; i < 8; i++) {
					double d0 = this.getRandom().nextGaussian() * 0.02D;
					double d1 = this.getRandom().nextGaussian() * 0.02D;
					double d2 = this.getRandom().nextGaussian() * 0.02D;
					this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(RatsBlockRegistry.DYE_SPONGE.get())), this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double) this.getBbHeight(), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d0, d1, d2);
				}
				this.playSound(RatsSoundRegistry.DYE_SPONGE_USED.get(), this.getSoundVolume(), this.getVoicePitch());
				return InteractionResult.SUCCESS;
			} else if (itemstack.is(RatsItemRegistry.RATBOW_ESSENCE.get()) && this.applySpecialDyeIfPossible(itemstack)) {
				return InteractionResult.SUCCESS;

			} else if (itemstack.is(RatsItemRegistry.RAT_SACK.get())) {
				if (RatSackItem.getRatsInSack(itemstack) >= RatConfig.ratSackCapacity) {
					player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_SACK_TOO_FULL).withStyle(ChatFormatting.RED), true);
					return InteractionResult.PASS;
				} else {
					RatSackItem.packRatIntoSack(itemstack, this, RatSackItem.getRatsInSack(itemstack) + 1);
					this.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1, 1);
					this.discard();
					player.swing(hand);
					return InteractionResult.SUCCESS;
				}
			} else if (itemstack.getItem() instanceof RatStaffItem) {
				player.getCapability(RatsCapabilityRegistry.SELECTED_RAT).ifPresent(cap -> cap.setSelectedRat(this));
				player.swing(hand);
				if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
					RatsNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ManageRatStaffPacket(this.getId(), BlockPos.ZERO, Direction.NORTH.ordinal(), false, false));
				}
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_STAFF_BIND, this.getName()), true);
				return InteractionResult.SUCCESS;
			} else if (itemstack.is(Items.ARROW)) {
				itemstack.shrink(1);
				ItemStack arrow = new ItemStack(RatsItemRegistry.RAT_ARROW.get());
				CompoundTag tag = new CompoundTag();
				CompoundTag ratTag = new CompoundTag();
				this.addAdditionalSaveData(ratTag);
				if (this.hasCustomName()) {
					ratTag.putString("CustomName", Component.Serializer.toJson(this.getCustomName()));
				}
				tag.put("Rat", ratTag);
				arrow.setTag(tag);
				if (itemstack.isEmpty()) {
					player.setItemInHand(hand, arrow);
				} else if (!player.getInventory().add(arrow)) {
					player.drop(arrow, false);
				}
				this.playSound(RatsSoundRegistry.RAT_HURT.get(), 1, 1);
				player.swing(hand);
				this.discard();
				return InteractionResult.SUCCESS;
			} else {
				return super.mobInteract(player, hand);
			}
		}
		return InteractionResult.PASS;
	}

	public boolean applySpecialDyeIfPossible(ItemStack stack) {
		String name = stack.getHoverName().getString();
		if (!this.getSpecialDye().equals(name) || !this.isDyed()) {
			if (!this.isDyed()) {
				this.setDyed(true);
			}
			this.setDyeColor(100);
			this.setSpecialDye(name);
			for (int i = 0; i < 8; i++) {
				double d0 = this.getRandom().nextGaussian() * 0.02D;
				double d1 = this.getRandom().nextGaussian() * 0.02D;
				double d2 = this.getRandom().nextGaussian() * 0.02D;
				this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(RatsItemRegistry.RATBOW_ESSENCE.get())), this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double) this.getBbHeight(), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d0, d1, d2);
			}
			this.playSound(RatsSoundRegistry.ESSENCE_APPLIED.get(), this.getSoundVolume(), this.getVoicePitch());
			stack.shrink(1);
			return true;
		}
		return false;
	}

	@Override
	public void setTame(boolean tamed) {
		if (tamed) {
			Arrays.fill(this.armorDropChances, 1.1F);
			Arrays.fill(this.handDropChances, 1.1F);
		}
		super.setTame(tamed);
	}

	@Override
	public int getAmbientSoundInterval() {
		return RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_CHRISTMAS.get()) ? 1000 : 200;
	}

	@Override
	public boolean causeFallDamage(float dist, float mult, DamageSource source) {
		if (!this.isInvulnerableTo(source) && !this.isPassenger()) {
			return super.causeFallDamage(dist, mult, source);
		}
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
		if (!this.hasFlightUpgrade()) {
			super.checkFallDamage(y, onGround, state, pos);
		}
	}

	@Override
	public void rideTick() {
		super.rideTick();
		if (this.getVehicle() instanceof Player player) {
			this.updateRiding(player);
		}
	}

	public void updateRiding(Player riding) {
		int i = riding.getPassengers().indexOf(this);
		float radius = (i == 0 ? 0F : 0.4F) + (riding.isFallFlying() ? 1 : 0);
		float angle = (0.01745329251F * riding.yBodyRot + (i == 2 ? -92.5F : i == 1 ? 92.5F : 0));
		double extraX = radius * Mth.sin((float) (Math.PI + angle));
		double extraZ = radius * Mth.cos(angle);
		double extraY = (riding.isCrouching() ? 1.2D : riding.isFallFlying() ? 0.25D : 1.4D);
		this.setYRot(riding.yHeadRot);
		this.yHeadRot = riding.yHeadRot;
		this.yRotO = riding.yHeadRot;
		this.setPos(riding.getX() + extraX, riding.getY() + extraY, riding.getZ() + extraZ);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 85) {
			this.crafting = true;
		} else if (id == 86) {
			this.crafting = false;
		} else if (id == 127) {
			for (int k = 0; k < 20; ++k) {
				double d2 = this.getRandom().nextGaussian() * 0.02D;
				double d0 = this.getRandom().nextGaussian() * 0.02D;
				double d1 = this.getRandom().nextGaussian() * 0.02D;
				this.level().addParticle(ParticleTypes.POOF, this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight()), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d2, d0, d1);
			}
		} else {
			super.handleEntityEvent(id);
		}
	}

	protected SoundEvent getAmbientSound() {
		if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_CHRISTMAS.get())) {
			return RatsSoundRegistry.RAT_SANTA.get();
		}
		if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
			SoundEvent possibleDragonSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("iceandfire", "firedragon_child_idle"));
			if (possibleDragonSound != null) {
				return possibleDragonSound;
			}
		}
		return super.getAmbientSound();
	}

	protected SoundEvent getDeathSound() {
		if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
			SoundEvent possibleDragonSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("iceandfire", "firedragon_child_death"));
			if (possibleDragonSound != null) {
				return possibleDragonSound;
			}
		}
		return super.getDeathSound();
	}

	protected SoundEvent getHurtSound(DamageSource source) {
		if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
			SoundEvent possibleDragonSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("iceandfire", "firedragon_child_hurt"));
			if (possibleDragonSound != null) {
				return possibleDragonSound;
			}
		}
		return super.getHurtSound(source);
	}

	public boolean onHearFlute(Player player, RatCommand ratCommand) {
		if (this.isOwnedBy(player) && !this.isBaby() && !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_NO_FLUTE.get())) {
			this.setCommand(ratCommand);
			return true;
		}
		return false;
	}

	public boolean canRatPickupItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if ((RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get()) || RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_WHITELIST.get()))) {
			CompoundTag tag;
			if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get())) {
				tag = RatUpgradeUtils.getUpgrade(this, RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get()).getTag();
			} else {
				tag = RatUpgradeUtils.getUpgrade(this, RatsItemRegistry.RAT_UPGRADE_WHITELIST.get()).getTag();
			}
			String ourItemID = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).toString();
			if (tag != null && tag.contains("Items", 9)) {
				ListTag list = tag.getList("Items", 10);
				if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get())) {
					for (int i = 0; i < list.size(); ++i) {
						String itemID = list.getCompound(i).getString("id");
						if (ourItemID.equals(itemID)) {
							return false;
						}
					}
					return true;
				} else {
					//whitelist
					for (int i = 0; i < list.size(); ++i) {
						String itemID = list.getCompound(i).getString("id");
						if (ourItemID.equals(itemID)) {
							return true;
						}
					}
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public MobType getMobType() {
		if (this.getInventory() != null) {
			if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_UNDEAD.get())) {
				return MobType.UNDEAD;
			}
			if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_AQUATIC.get())) {
				return MobType.WATER;
			}
		}
		return super.getMobType();
	}

	@Override
	public boolean isPushable() {
		return !this.isBaby() && super.isPushable();
	}

	@Override
	public boolean isCurrentlyGlowing() {
		if (this.level().isClientSide() && ForgeClientEvents.isRatSelectedOnStaff(this)) return true;
		return super.isCurrentlyGlowing();
	}

	public void attemptTeleport(double x, double y, double z) {
		double d0 = this.getX();
		double d1 = this.getY();
		double d2 = this.getZ();
		this.setPos(x, y, z);
		this.level().broadcastEntityEvent(this, (byte) 84);
		boolean flag = false;
		BlockPos blockpos = this.blockPosition();

		if (this.level().isLoaded(blockpos)) {
			boolean flag1 = false;

			while (!flag1 && blockpos.getY() > 0) {
				BlockPos blockpos1 = blockpos.below();
				BlockState state = this.level().getBlockState(blockpos1);

				if (state.blocksMotion()) {
					flag1 = true;
				} else {
					this.setPos(this.getX(), this.getY() - 1, this.getZ());
					blockpos = blockpos1;
				}
			}

			if (flag1) {
				this.teleportTo(this.getX(), this.getY(), this.getZ());

				if (this.level().noCollision(this) && !this.level().containsAnyLiquid(this.getBoundingBox())) {
					flag = true;
				}
			}
		}

		if (!flag) {
			this.teleportTo(d0, d1, d2);
		} else {
			this.playSound(RatsSoundRegistry.RAT_TELEPORT.get(), 1, 1);
		}
	}

	public boolean isDirectPathBetweenPoints(Vec3 target) {
		BlockHitResult result = this.level().clip(new ClipContext(this.position(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		BlockPos pos = result.getBlockPos();
		BlockPos sidePos = result.getBlockPos().relative(result.getDirection());
		if (!this.level().isEmptyBlock(pos) || !this.level().isEmptyBlock(sidePos)) {
			return true;
		} else {
			return result.getType() == HitResult.Type.MISS;
		}
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_UNDEAD.get()) && super.canBeSeenAsEnemy();
	}

	@Override
	public boolean isNoAi() {
		return super.isNoAi() || this.getRespawnCountdown() > 0;
	}

	public boolean holdsItemInHandUpgrade() {
		boolean bool = RatUpgradeUtils.forEachUpgradeBool(this, stack -> stack instanceof HoldsItemUpgrade upgrade && !upgrade.isFakeHandRender(), false);

		return !this.isInWheel() && bool;
	}

	@Override
	public boolean shouldPlayIdleAnimations() {
		boolean bool = RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.playIdleAnimation(this), true);
		return super.shouldPlayIdleAnimations() && !this.isInTube() && !this.isInWheel() && this.cookingProgress <= 0 && bool;
	}

	public boolean hasAnyUpgrades() {
		for (int index : RatContainer.UPGRADES) {
			if (!this.upgradeInSlot(index).isEmpty()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean canDigThroughBlocks() {
		return false;
	}

	public void onUpgradeChanged() {
		this.setupDynamicAI();

		Entity vehicle = this.getVehicle();
		if (!this.level().isClientSide() && vehicle instanceof RatMount mount) {
			if (!RatUpgradeUtils.hasUpgrade(this, mount.getUpgradeItem())) {
				this.level().broadcastEntityEvent(this, (byte) 127);
				this.stopRiding();
				vehicle.discard();
			}
		}

		rebuildRatAttributes();

		if (this.getHeldRF() > this.getRFTransferRate()) {
			this.setHeldRF(this.getRFTransferRate());
		}
		this.heal(this.getMaxHealth());
		this.setFlying(this.isFlying() && this.hasFlightUpgrade());
		this.setNoGravity(RatUpgradeUtils.hasUpgrade(this, RatlantisItemRegistry.RAT_UPGRADE_ETHEREAL.get()) || this.isFlying());
	}

	private void rebuildRatAttributes() {
		if (!this.attributeChanges.isEmpty()) {
			this.getAttributes().removeAttributeModifiers(this.attributeChanges);
			this.attributeChanges.clear();
		}

		this.applyRatGeneAttributeModifiers();
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof StatBoostingUpgrade, (stack, slot) ->
				((StatBoostingUpgrade) stack.getItem()).getAttributeBoosts().forEach((attribute, aDouble) -> this.tryIncreaseStat(stack.getHoverName().getString(), attribute, aDouble)));
        entityData.set(PROFILE_ATTACK, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

	public void tryIncreaseStat(String itemName, Attribute stat, double value) {
		AttributeModifier modifier = new AttributeModifier(itemName + " " + Component.translatable(stat.getDescriptionId()).getString() + " Modifier", value, AttributeModifier.Operation.ADDITION);
		Objects.requireNonNull(this.getAttribute(stat)).addTransientModifier(modifier);
		this.attributeChanges.put(stat, modifier);
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		if (this.getRespawnCountdown() > 0) {
			return true;
		}
		AtomicBoolean upgradePrevented = new AtomicBoolean(false);
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof DamageImmunityUpgrade, (stack, slot) -> {
			if (((DamageImmunityUpgrade) stack.getItem()).isImmuneToDamageSource(this, source)) {
				upgradePrevented.set(true);
			}
		});

		if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_CREATIVE.get())) {
			return source.getEntity() == null || source.getEntity() instanceof LivingEntity living && !this.isOwnedBy(living);
		}
		return upgradePrevented.get() || super.isInvulnerableTo(source);
	}

	@Override
	public boolean isInvulnerable() {
		return super.isInvulnerable() || this.getRespawnCountdown() > 0;
	}

	@Override
	public void setRecordPlayingNearby(BlockPos pos, boolean partying) {
		int moves = this.getRandom().nextInt(4);
		if (!this.isDancing() && partying) {
			this.setDanceMoves(moves);
		}
		this.setDancing(partying);
		this.jukeboxPos = pos;
		if (this.level().isClientSide()) {
			RatsNetworkHandler.CHANNEL.sendToServer(new SetDancingRatPacket(this.getId(), partying, pos.asLong(), moves));
		}
	}

	public boolean shouldDepositItem(ItemStack item) {
		if (RatLegion.isCarrier(item)) return false;
		return RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.shouldDepositItem(this, item), true);
	}

	public boolean shouldCollectItem(ItemStack item) {
		return RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.shouldCollectItem(this, item), true);
	}

	public int getRFTransferRate() {
		AtomicInteger energy = new AtomicInteger();
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof EnergyRatUpgradeItem, (stack, slot) ->
				energy.set(((EnergyRatUpgradeItem) stack.getItem()).getRFTransferRate()));

		return energy.get();
	}

	public int getMBTransferRate() {
		AtomicInteger fluid = new AtomicInteger();
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof BucketRatUpgradeItem, (stack, slot) ->
				fluid.set(((BucketRatUpgradeItem) stack.getItem()).getMbTransferRate()));

		return fluid.get();
	}

	@Override
	public boolean isAlliedTo(Entity entity) {
		LivingEntity livingentity = this.getOwner();
		if (entity == livingentity) {
			return true;
		} else if (entity instanceof RatMountBase mount) {
			return mount.getRat() != null && this.isAlliedTo(mount.getRat());
		} else if (livingentity != null && entity instanceof TamableAnimal animal) {
			return animal.isOwnedBy(livingentity);
		} else {
			return livingentity != null ? livingentity.isAlliedTo(entity) : super.isAlliedTo(entity);
		}
	}

	@Nullable
	public EntityType<?> getMountEntityType() {
		AtomicReference<EntityType<?>> type = new AtomicReference<>(null);
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof MountRatUpgradeItem, (stack, slot) -> type.set(((MountRatUpgradeItem<?>) stack.getItem()).getEntityType()));
		return type.get();
	}

	public double getRatDistanceCenterSq(double x, double y, double z) {
		double d0 = this.getX() - x - 0.5D;
		double d1 = this.getY() - y - 0.5D;
		double d2 = this.getZ() - z - 0.5D;
		if (this.getVehicle() != null && getMountEntityType() != null && this.getVehicle().getType() == getMountEntityType()) {
			d0 = this.getVehicle().getX() - x - 0.5D;
			d1 = this.getVehicle().getY() - y - 0.5D;
			d2 = this.getVehicle().getZ() - z - 0.5D;
		}
		return d0 * d0 + d1 * d1 + d2 * d2;
	}

	public double getRatDistanceSq(double x, double y, double z) {
		double d0 = this.getX() - x;
		double d1 = this.getY() - y;
		double d2 = this.getZ() - z;
		if (this.getVehicle() != null && this.getMountEntityType() != null && this.getVehicle().getType() == this.getMountEntityType()) {
			d0 = this.getVehicle().getX() - x;
			d1 = this.getVehicle().getY() - y;
			d2 = this.getVehicle().getZ() - z;
		}
		return d0 * d0 + d1 * d1 + d2 * d2;
	}

	@Override
	public double distanceToSqr(Vec3 vec) {
		double d0 = this.getX() - vec.x();
		double d1 = this.getY() - vec.y();
		double d2 = this.getZ() - vec.z();
		if (this.getVehicle() != null && this.getMountEntityType() != null && this.getVehicle().getType() == this.getMountEntityType()) {
			d0 = this.getVehicle().getX() - vec.x();
			d1 = this.getVehicle().getY() - vec.y();
			d2 = this.getVehicle().getZ() - vec.z();
		}
		return d0 * d0 + d1 * d1 + d2 * d2;
	}


	public boolean isRidingSpecialMount() {
		boolean ret = false;
		if (this.getVehicle() != null && this.getMountEntityType() != null) {
			ret = this.getVehicle().getType().equals(this.getMountEntityType());
		}
		return ret;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !this.isRidingSpecialMount() || !RatUtils.isRidingOrBeingRiddenBy(this, target);
	}

	public double getRatHarvestDistance(double expansion) {
		return (3.5F + expansion) * this.getRatDistanceModifier();
	}

	public double getRatDistanceModifier() {
		if (this.isRidingSpecialMount()) {
			Entity entity = this.getVehicle();
			if (entity != null) {
				if (entity instanceof RatBiplaneMount) {
					return 3.95D;
				}
				return 1.5D;
			}

		}
		return 1.0D;
	}

	public boolean hasFlightUpgrade() {
		return RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.canFly(this), false);
	}

    private Direction readSavedDirection(CompoundTag tag, String key, Direction fallback) {
        try {
            int ordinal = tag.getInt(key);
            Direction[] directions = Direction.values();
            if (ordinal < 0 || ordinal >= directions.length) {
                RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u0430 \u0431\u0438\u0442\u043e\u0435 \u043d\u0430\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 {}={} \u0432 NBT, \u043f\u0440\u0438\u043c\u0435\u043d\u044f\u044e {}", this.getUUID(), key, ordinal, fallback);
                return fallback;
            }
            return directions[ordinal];
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u043d\u0430\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 {} \u043a\u0440\u044b\u0441\u044b {}", key, this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean canWorkBySchedule() {
        try {
            return RatDaySchedule.canWork(this.level());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0440\u0430\u0441\u043f\u043e\u0440\u044f\u0434\u043e\u043a \u0440\u0430\u0431\u043e\u0442\u044b \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public void setLastPathFailure(String reason) {
        try {
            this.getEntityData().set(LAST_PATH_FAILURE, (reason == null ? "" : reason));
            if (reason != null && !reason.isBlank()) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043f\u043e\u0441\u0442\u0440\u043e\u0438\u043b\u0430 \u043f\u0443\u0442\u044c: {}", this.getUUID(), reason);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u043f\u0438\u0441\u0430\u0442\u044c \u0441\u0431\u043e\u0439 \u043f\u0443\u0442\u0438 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public String getLastPathFailure() {
        try {
            return (String)this.getEntityData().get(LAST_PATH_FAILURE);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0441\u0431\u043e\u0439 \u043f\u0443\u0442\u0438 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public void clearLastPathFailure() {
        this.setLastPathFailure("");
    }

    public Optional<GlobalPos> getCreateWorkPos() {
        try {
            return (Optional)this.getEntityData().get(CREATE_WORK_POS);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0442\u043e\u0447\u043a\u0443 \u043f\u0440\u0438\u043c\u0435\u043d\u0435\u043d\u0438\u044f Create \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public void setCreateWorkPos(@Nullable GlobalPos pos) {
        try {
            this.getEntityData().set(CREATE_WORK_POS, Optional.ofNullable(pos));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0434\u0430\u0442\u044c \u0442\u043e\u0447\u043a\u0443 \u043f\u0440\u0438\u043c\u0435\u043d\u0435\u043d\u0438\u044f Create \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public Optional<GlobalPos> getBoundHousePos() {
        try {
            return (Optional)this.getEntityData().get(BOUND_HOUSE_POS);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u043d\u043d\u044b\u0439 \u0434\u043e\u043c\u0438\u043a \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public void setBoundHousePos(@Nullable GlobalPos pos) {
        try {
            this.getEntityData().set(BOUND_HOUSE_POS, Optional.ofNullable(pos));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0434\u0430\u0442\u044c \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u043d\u043d\u044b\u0439 \u0434\u043e\u043c\u0438\u043a \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private RatVariant chooseBabyColorVariant(TamedRat mother, TamedRat father) {
        try {
            boolean hasExclusiveParent;
            RatVariant motherVariant = mother.getColorVariant();
            RatVariant fatherVariant = father.getColorVariant();
            boolean bl = hasExclusiveParent = motherVariant.isBreedingExclusive() || fatherVariant.isBreedingExclusive();
            if (hasExclusiveParent && this.getRandom().nextFloat() <= 0.16666667f) {
                RatVariant variant = this.getRandomDifferentColorVariant(true, motherVariant, fatherVariant);
                RatsMod.LOGGER.info("\u041a\u0440\u044b\u0441\u0430 \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u0430 \u0440\u0435\u0434\u043a\u0443\u044e \u043c\u0443\u0442\u0430\u0446\u0438\u044e \u043e\u043a\u0440\u0430\u0441\u0430 \u043f\u043e\u0442\u043e\u043c\u0441\u0442\u0432\u0430: {}", RatVariant.getVariantId(variant));
                return variant;
            }
            if (this.getRandom().nextFloat() <= 0.1f) {
                RatVariant variant = this.getRandomDifferentColorVariant(false, motherVariant, fatherVariant);
                RatsMod.LOGGER.info("\u041a\u0440\u044b\u0441\u0430 \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u0430 \u043c\u0443\u0442\u0430\u0446\u0438\u044e \u043e\u0431\u044b\u0447\u043d\u043e\u0433\u043e \u043e\u043a\u0440\u0430\u0441\u0430 \u043f\u043e\u0442\u043e\u043c\u0441\u0442\u0432\u0430: {}", RatVariant.getVariantId(variant));
                return variant;
            }
            return this.getRandom().nextBoolean() ? fatherVariant : motherVariant;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0431\u0440\u0430\u0442\u044c \u043e\u043a\u0440\u0430\u0441 \u0434\u0435\u0442\u0451\u043d\u044b\u0448\u0430 \u043e\u0442 \u0440\u043e\u0434\u0438\u0442\u0435\u043b\u0435\u0439 {} \u0438 {}", mother.getUUID(), father.getUUID(), exception);
            throw exception;
        }
    }

    private RatVariant getRandomDifferentColorVariant(boolean breedingExclusive, RatVariant motherVariant, RatVariant fatherVariant) {
        try {
            ArrayList<RatVariant> variants = new ArrayList<>(RatVariantRegistry.RAT_VARIANT_REGISTRY.get().getValues());
            variants.removeIf(variant -> variant.isBreedingExclusive() != breedingExclusive || variant == motherVariant || variant == fatherVariant);
            if (variants.isEmpty()) {
                throw new IllegalStateException("\u041d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b\u0445 \u0432\u0430\u0440\u0438\u0430\u043d\u0442\u043e\u0432 \u043e\u043a\u0440\u0430\u0441\u0430 \u0434\u043b\u044f \u043c\u0443\u0442\u0430\u0446\u0438\u0438, breedingExclusive=" + breedingExclusive);
            }
            return (RatVariant)variants.get(this.getRandom().nextInt(variants.size()));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0431\u0440\u0430\u0442\u044c \u0441\u043b\u0443\u0447\u0430\u0439\u043d\u044b\u0439 \u043e\u043a\u0440\u0430\u0441 \u043c\u0443\u0442\u0430\u0446\u0438\u0438 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private float sizeProgress() {
        return (getRatLevel() - 1f) / (RatProgression.MAX_LEVEL - 1f);
    }

    public float getLevelModelScale() {
        // The rat renderer starts at 0.6 scale. Match the adult wolf's body length at graduation.
        return Mth.lerp(sizeProgress(), getRatModelScaleFromGenes(), 2.2f);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        var base = super.getDimensions(pose);
        if (entityData == null || !entityData.hasItem(RAT_EXPERIENCE)) return base;
        var wolf = EntityType.WOLF.getDimensions();
        float progress = sizeProgress();
        return base.scale(Mth.lerp(progress, 1f, wolf.width / getType().getWidth()),
                Mth.lerp(progress, 1f, wolf.height / getType().getHeight()));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == RAT_EXPERIENCE) refreshDimensions();
    }

    public float getRatModelScaleFromGenes() {
        try {
            if (!((Boolean)this.getEntityData().get(RAT_GENES_INITIALIZED)).booleanValue()) {
                return 1.0f;
            }
            return this.calculateRatModelScale(((Float)this.getEntityData().get(GENE_HEALTH)).floatValue());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0440\u0430\u0437\u043c\u0435\u0440 \u043c\u043e\u0434\u0435\u043b\u0438 \u043f\u043e \u0433\u0435\u043d\u0430\u043c \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private float getRatRegenGene() {
        try {
            return this.getRatGene(GENE_REGEN);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0433\u0435\u043d \u0440\u0435\u0433\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u0438 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public float getRatSpeedGeneForDisplay() {
        try {
            return this.getRatGene(GENE_SPEED);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0433\u0435\u043d \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u0438 \u043a\u0440\u044b\u0441\u044b {} \u0434\u043b\u044f GUI", this.getUUID(), exception);
            throw exception;
        }
    }

    public float getRatHealthGeneForDisplay() {
        try {
            return this.getRatGene(GENE_HEALTH);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0433\u0435\u043d \u0437\u0434\u043e\u0440\u043e\u0432\u044c\u044f \u043a\u0440\u044b\u0441\u044b {} \u0434\u043b\u044f GUI", this.getUUID(), exception);
            throw exception;
        }
    }

    public float getRatArmorGeneForDisplay() {
        try {
            return this.getRatGene(GENE_ARMOR);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0433\u0435\u043d \u0431\u0440\u043e\u043d\u0438 \u043a\u0440\u044b\u0441\u044b {} \u0434\u043b\u044f GUI", this.getUUID(), exception);
            throw exception;
        }
    }

    public float getRatDamageGeneForDisplay() {
        try {
            return this.getRatGene(GENE_DAMAGE);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0433\u0435\u043d \u0443\u0440\u043e\u043d\u0430 \u043a\u0440\u044b\u0441\u044b {} \u0434\u043b\u044f GUI", this.getUUID(), exception);
            throw exception;
        }
    }

    public float getGoetyPowerGene() {
        return getRatGene(GENE_GOETY);
    }

    public float getCooldownGene(){return getRatGene(GENE_COOLDOWN);}
    public double spellCooldownReduction(){return .90*RatProgression.growth(getRatLevel())*(getCooldownGene()-.5)/2.0;}
    public int spellCooldownTicks(int base){
        return Math.max(1,(int)Math.ceil(Math.max(1,base)*(1.0-spellCooldownReduction())-1.0E-8));
    }

    public float getIronPowerGene() {
        return getRatGene(GENE_IRON);
    }

    public float getRatRegenGeneForDisplay() {
        try {
            return this.getRatGene(GENE_REGEN);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0433\u0435\u043d \u0440\u0435\u0433\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u0438 \u043a\u0440\u044b\u0441\u044b {} \u0434\u043b\u044f GUI", this.getUUID(), exception);
            throw exception;
        }
    }

    public void setRatGivenName(String name) {
        try {
            String trimmedName;
            String string = trimmedName = name == null ? "" : name.trim();
            if (trimmedName.isBlank()) {
                this.setCustomName(null);
                this.setCustomNameVisible(false);
                return;
            }
            if (trimmedName.length() > 48) {
                throw new IllegalArgumentException("\u0418\u043c\u044f \u043a\u0440\u044b\u0441\u044b \u0434\u043b\u0438\u043d\u043d\u0435\u0435 48 \u0441\u0438\u043c\u0432\u043e\u043b\u043e\u0432: " + trimmedName.length());
            }
            this.setCustomName((Component)Component.literal((String)trimmedName));
            this.setCustomNameVisible(true);
            RatsMod.LOGGER.info("\u041a\u0440\u044b\u0441\u0435 {} \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u043e \u0438\u043c\u044f '{}'", this.getUUID(), trimmedName);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0437\u043d\u0430\u0447\u0438\u0442\u044c \u0438\u043c\u044f \u043a\u0440\u044b\u0441\u0435 {}: {}", this.getUUID(), name, exception);
            throw exception;
        }
    }

    public static boolean isCheeseReserveItem(ItemStack stack) {
        try {
            return !stack.isEmpty() && (stack.is(RatsItemTags.CHEESE_ITEMS) || stack.is((Item)RatsItemRegistry.CREATIVE_CHEESE.get()));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0441\u044b\u0440\u043d\u044b\u0439 \u0441\u0442\u0435\u043a \u043a\u0440\u044b\u0441\u044b {}", stack, exception);
            throw exception;
        }
    }

    private void tickRatHunger() {
        try {
            if (Math.floorMod(this.tickCount + this.getId(), 20) != 0) {
                return;
            }
            this.resetRatHungerDayIfNeeded();
            if (this.needsCheeseNow()) {
                this.consumeCheeseFromReserve("\u043b\u0438\u0447\u043d\u044b\u0439 \u0437\u0430\u043f\u0430\u0441");
            }
            if (Math.floorMod(this.tickCount + this.getId(), 100) == 0) {
                this.shareCheeseWithNearbyRats();
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u0442\u044c \u0433\u043e\u043b\u043e\u0434 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private long getRatHungerDay() {
        try {
            return this.level().getGameTime() / 24000L;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0434\u0435\u043d\u044c \u0433\u043e\u043b\u043e\u0434\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private void resetRatHungerDayIfNeeded() {
        try {
            long currentDay = this.getRatHungerDay();
            if (this.level().isClientSide()) {
                this.cheeseEatenToday = (Integer)this.getEntityData().get(CHEESE_EATEN_TODAY_SYNC);
                this.lastFeedDay = ((Integer)this.getEntityData().get(LAST_FEED_DAY_SYNC)).intValue();
                return;
            }
            if (this.lastFeedDay != currentDay) {
                this.cheeseEatenToday = 0;
                this.lastFeedDay = currentDay;
                this.cachedHungerEfficiencyTick = this.tickCount;
                this.cachedHungerEfficiency = 0.0f;
                this.syncRatHungerData();
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0430\u0447\u0430\u043b\u0430 \u043d\u043e\u0432\u044b\u0439 \u0434\u0435\u043d\u044c \u0433\u043e\u043b\u043e\u0434\u0430 {}", this.getUUID(), currentDay);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u0434\u0435\u043d\u044c \u0433\u043e\u043b\u043e\u0434\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private void syncRatHungerData() {
        try {
            this.getEntityData().set(CHEESE_EATEN_TODAY_SYNC, this.cheeseEatenToday);
            this.getEntityData().set(LAST_FEED_DAY_SYNC, Mth.clamp((int)((int)this.lastFeedDay), (int)-1, (int)Integer.MAX_VALUE));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0433\u043e\u043b\u043e\u0434 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public int getDailyCheeseRequirement() {
        try {
            return 4;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u043d\u043e\u0440\u043c\u0443 \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean isSparkCharged() {
        return getEvolution()==RatEvolution.SPARK && getCheeseEatenToday()>=4;
    }

    public int getCheeseEatenToday() {
        try {
            this.resetRatHungerDayIfNeeded();
            return this.cheeseEatenToday;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0441\u0447\u0451\u0442\u0447\u0438\u043a \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public float getRatHungerEfficiency() {
        try {
            if (!this.level().isClientSide() && this.tickCount - this.cachedHungerEfficiencyTick <= 20) {
                return this.cachedHungerEfficiency;
            }
            this.resetRatHungerDayIfNeeded();
            int requirement = this.getDailyCheeseRequirement();
            if (requirement <= 0) {
                throw new IllegalStateException("\u0421\u0443\u0442\u043e\u0447\u043d\u0430\u044f \u043d\u043e\u0440\u043c\u0430 \u0441\u044b\u0440\u0430 \u0434\u043e\u043b\u0436\u043d\u0430 \u0431\u044b\u0442\u044c \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0439");
            }
            float efficiency = 2.0F * Mth.clamp(this.cheeseEatenToday, 0, requirement) / requirement;
            if (!this.level().isClientSide()) {
                this.cachedHungerEfficiencyTick = this.tickCount;
                this.cachedHungerEfficiency = efficiency;
            }
            return efficiency;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0441\u044b\u0442\u043e\u0441\u0442\u044c \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean canWorkFromHunger() {
        try {
            return this.getRatHungerEfficiency() > 0.0f;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u0443 \u043f\u043e \u0441\u044b\u0442\u043e\u0441\u0442\u0438 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public double getHungerAdjustedWorkSpeed(double baseSpeed) {
        try {
            if (baseSpeed <= 0.0) {
                throw new IllegalArgumentException("\u0411\u0430\u0437\u043e\u0432\u0430\u044f \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0440\u0430\u0431\u043e\u0447\u0435\u0439 \u0446\u0435\u043b\u0438 \u0434\u043e\u043b\u0436\u043d\u0430 \u0431\u044b\u0442\u044c \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0439: " + baseSpeed);
            }
            float efficiency = this.getRatHungerEfficiency();
            if (efficiency <= 0.0f) {
                return 0.0;
            }
            return baseSpeed * (double)efficiency;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u0430\u0442\u044c \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u044b \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean moveSmoothlyTo(double x, double y, double z, double speed) {
        try {
            if (this.shouldUseDirectNavigation()) {
                return this.getNavigation().moveTo(x, y, z, speed);
            }
            if (this.canReuseSmoothPath(x, y, z)) {
                return true;
            }
            this.rememberSmoothMoveTarget(x, y, z, null);
            return this.getNavigation().moveTo(x, y, z, speed);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043b\u0430\u0432\u043d\u043e \u043f\u043e\u0439\u0442\u0438 \u043a {}, {}, {}", this.getUUID(), x, y, z, exception);
            throw exception;
        }
    }

    public boolean moveSmoothlyTo(Entity target, double speed) {
        try {
            if (this.shouldUseDirectNavigation()) {
                return this.getNavigation().moveTo(target, speed);
            }
            if (this.lastSmoothMoveEntity == target && !this.getNavigation().isDone() && this.tickCount - this.lastSmoothMoveTick <= 8) {
                return true;
            }
            this.rememberSmoothMoveTarget(target.getX(), target.getY(), target.getZ(), target);
            return this.getNavigation().moveTo(target, speed);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043b\u0430\u0432\u043d\u043e \u043f\u043e\u0439\u0442\u0438 \u043a \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0438 {}", this.getUUID(), target.getUUID(), exception);
            throw exception;
        }
    }

    public boolean moveSmoothlyTo(Path path, double speed) {
        try {
            if (this.shouldUseDirectNavigation()) {
                return this.getNavigation().moveTo(path, speed);
            }
            if (!this.getNavigation().isDone() && this.tickCount - this.lastSmoothMoveTick <= 8) {
                return true;
            }
            this.lastSmoothMoveTick = this.tickCount;
            this.lastSmoothMoveEntity = null;
            return this.getNavigation().moveTo(path, speed);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043b\u0430\u0432\u043d\u043e \u043f\u043e\u0439\u0442\u0438 \u043f\u043e \u0433\u043e\u0442\u043e\u0432\u043e\u043c\u0443 \u043f\u0443\u0442\u0438", this.getUUID(), exception);
            throw exception;
        }
    }

    private boolean shouldUseDirectNavigation() {
        try {
            return RatUpgradeUtils.hasUpgrade(this,RatsItemRegistry.RAT_UPGRADE_QUARRY.get()) || this.navigatorType == 2 || this.navigatorType == 5;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0440\u0435\u0436\u0438\u043c \u043f\u043b\u0430\u0432\u043d\u043e\u0439 \u043d\u0430\u0432\u0438\u0433\u0430\u0446\u0438\u0438", this.getUUID(), exception);
            throw exception;
        }
    }

    private boolean canReuseSmoothPath(double x, double y, double z) {
        try {
            if (this.getNavigation().isDone() || this.tickCount - this.lastSmoothMoveTick > 8) {
                return false;
            }
            double dx = this.lastSmoothMoveX - x;
            double dy = this.lastSmoothMoveY - y;
            double dz = this.lastSmoothMoveZ - z;
            return dx * dx + dy * dy + dz * dz <= 1.0;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043f\u043e\u0432\u0442\u043e\u0440\u043d\u043e\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u0443\u0442\u0438", this.getUUID(), exception);
            throw exception;
        }
    }

    private void rememberSmoothMoveTarget(double x, double y, double z, @Nullable Entity target) {
        try {
            this.lastSmoothMoveTick = this.tickCount;
            this.lastSmoothMoveX = x;
            this.lastSmoothMoveY = y;
            this.lastSmoothMoveZ = z;
            this.lastSmoothMoveEntity = target;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u043f\u043e\u043c\u043d\u0438\u0442\u044c \u0446\u0435\u043b\u044c \u043f\u043b\u0430\u0432\u043d\u043e\u0433\u043e \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u044f", this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean needsCheeseNow() {
        try {
            int interval;
            this.resetRatHungerDayIfNeeded();
            int requirement = this.getDailyCheeseRequirement();
            if (requirement <= 0) {
                throw new IllegalStateException("\u0421\u0443\u0442\u043e\u0447\u043d\u0430\u044f \u043d\u043e\u0440\u043c\u0430 \u0441\u044b\u0440\u0430 \u0434\u043e\u043b\u0436\u043d\u0430 \u0431\u044b\u0442\u044c \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0439");
            }
            long dayProgress = this.level().getGameTime() % 24000L;
            int requiredByNow = Mth.clamp((int)((int)(dayProgress / (long)(interval = Math.max(1, 24000 / requirement))) + 1), (int)0, (int)requirement);
            return this.cheeseEatenToday < requiredByNow;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043f\u043e\u0442\u0440\u0435\u0431\u043d\u043e\u0441\u0442\u044c \u0432 \u0441\u044b\u0440\u0435 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean consumeCheeseFromReserve(String sourceName) {
        try {
            if (this.getInventory() == null) {
                throw new IllegalStateException("\u0423 \u043a\u0440\u044b\u0441\u044b \u043d\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f \u0434\u043b\u044f \u043b\u0438\u0447\u043d\u043e\u0433\u043e \u0437\u0430\u043f\u0430\u0441\u0430 \u0441\u044b\u0440\u0430");
            }
            ItemStack reserve = this.getInventory().getItem(6);
            if (reserve.isEmpty()) {
                return false;
            }
            if (!TamedRat.isCheeseReserveItem(reserve)) {
                return false;
            }
            boolean consumed = this.eatCheeseFromStack(reserve, sourceName);
            if (consumed) {
                if (reserve.isEmpty()) {
                    this.getInventory().setItem(6, ItemStack.EMPTY);
                } else {
                    this.getInventory().setChanged();
                }
            }
            return consumed;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u044a\u0435\u0441\u0442\u044c \u0441\u044b\u0440 \u0438\u0437 \u043b\u0438\u0447\u043d\u043e\u0433\u043e \u0437\u0430\u043f\u0430\u0441\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public int getMaxCheeseReserve() {
        try {
            return 16;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u043c\u0430\u043a\u0441\u0438\u043c\u0443\u043c \u043b\u0438\u0447\u043d\u043e\u0433\u043e \u0437\u0430\u043f\u0430\u0441\u0430 \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public int getCheeseReserveCount() {
        try {
            if(this.level().isClientSide()) return this.getEntityData().get(CHEESE_RESERVE_SYNC);
            if (this.getInventory() == null) {
                throw new IllegalStateException("\u0423 \u043a\u0440\u044b\u0441\u044b \u043d\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f \u0434\u043b\u044f \u043f\u043e\u0434\u0441\u0447\u0451\u0442\u0430 \u0441\u044b\u0440\u0430");
            }
            ItemStack reserve = this.getInventory().getItem(6);
            return TamedRat.isCheeseReserveItem(reserve) ? reserve.getCount() : 0;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u0441\u0447\u0438\u0442\u0430\u0442\u044c \u043b\u0438\u0447\u043d\u044b\u0439 \u0441\u044b\u0440 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public int getCheeseReserveFreeSpace(ItemStack stack) {
        try {
            if (!TamedRat.isCheeseReserveItem(stack)) {
                throw new IllegalArgumentException("\u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u043c\u0435\u0441\u0442\u0430 \u0432 \u0437\u0430\u043f\u0430\u0441\u0435 \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u0430 \u043d\u0435 \u0441\u044b\u0440: " + String.valueOf(stack));
            }
            if (this.getInventory() == null) {
                throw new IllegalStateException("\u0423 \u043a\u0440\u044b\u0441\u044b \u043d\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f \u0434\u043b\u044f \u043b\u0438\u0447\u043d\u043e\u0433\u043e \u0437\u0430\u043f\u0430\u0441\u0430 \u0441\u044b\u0440\u0430");
            }
            ItemStack reserve = this.getInventory().getItem(6);
            if (reserve.isEmpty()) {
                return this.getMaxCheeseReserve();
            }
            if (!TamedRat.isCheeseReserveItem(reserve) || !ItemStack.isSameItemSameTags((ItemStack)reserve, (ItemStack)stack)) {
                return 0;
            }
            return Math.max(0, this.getMaxCheeseReserve() - reserve.getCount());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043c\u0435\u0441\u0442\u043e \u043f\u043e\u0434 \u0441\u044b\u0440 \u0432 \u0437\u0430\u043f\u0430\u0441\u0435 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean hasCheeseReserveSpace() {
        try {
            if (this.getInventory() == null) {
                throw new IllegalStateException("\u0423 \u043a\u0440\u044b\u0441\u044b \u043d\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f \u0434\u043b\u044f \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0438 \u0437\u0430\u043f\u0430\u0441\u0430 \u0441\u044b\u0440\u0430");
            }
            ItemStack reserve = this.getInventory().getItem(6);
            if (reserve.isEmpty()) {
                return true;
            }
            return TamedRat.isCheeseReserveItem(reserve) && reserve.getCount() < this.getMaxCheeseReserve();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u043e\u0435 \u043c\u0435\u0441\u0442\u043e \u0437\u0430\u043f\u0430\u0441\u0430 \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean shouldRefillCheeseReserve() {
        try {
            return this.hasCheeseReserveSpace() && this.getCheeseReserveCount() <= 2;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043f\u043e\u0440\u043e\u0433 \u043f\u043e\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public int addCheeseToReserve(ItemStack source) {
        try {
            if (!TamedRat.isCheeseReserveItem(source)) {
                throw new IllegalArgumentException("\u0412 \u043b\u0438\u0447\u043d\u044b\u0439 \u0437\u0430\u043f\u0430\u0441 \u043a\u0440\u044b\u0441\u044b \u043c\u043e\u0436\u043d\u043e \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u044c \u0442\u043e\u043b\u044c\u043a\u043e \u0441\u044b\u0440: " + String.valueOf(source));
            }
            if (this.getInventory() == null) {
                throw new IllegalStateException("\u0423 \u043a\u0440\u044b\u0441\u044b \u043d\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f \u0434\u043b\u044f \u043f\u043e\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f \u0441\u044b\u0440\u0430");
            }
            int space = this.getCheeseReserveFreeSpace(source);
            if (space <= 0) {
                return 0;
            }
            int moved = Math.min(space, source.getCount());
            ItemStack reserve = this.getInventory().getItem(6);
            if (reserve.isEmpty()) {
                ItemStack inserted = source.split(moved);
                inserted.setCount(Math.min(inserted.getCount(), this.getMaxCheeseReserve()));
                this.getInventory().setItem(6, inserted);
            } else {
                reserve.grow(moved);
                source.shrink(moved);
                this.getInventory().setChanged();
            }
            return moved;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u044c \u043b\u0438\u0447\u043d\u044b\u0439 \u0437\u0430\u043f\u0430\u0441 \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public ItemStack takeCheeseFromReserve(int amount) {
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("\u041a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u0431\u0438\u0440\u0430\u0435\u043c\u043e\u0433\u043e \u0441\u044b\u0440\u0430 \u0434\u043e\u043b\u0436\u043d\u043e \u0431\u044b\u0442\u044c \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u043c: " + amount);
            }
            if (this.getInventory() == null) {
                throw new IllegalStateException("\u0423 \u043a\u0440\u044b\u0441\u044b \u043d\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f \u0434\u043b\u044f \u0432\u044b\u0434\u0430\u0447\u0438 \u0441\u044b\u0440\u0430");
            }
            ItemStack reserve = this.getInventory().getItem(6);
            if (!TamedRat.isCheeseReserveItem(reserve)) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = reserve.split(Math.min(amount, reserve.getCount()));
            if (reserve.isEmpty()) {
                this.getInventory().setItem(6, ItemStack.EMPTY);
            } else {
                this.getInventory().setChanged();
            }
            return removed;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0431\u0440\u0430\u0442\u044c \u0441\u044b\u0440 \u0438\u0437 \u043b\u0438\u0447\u043d\u043e\u0433\u043e \u0437\u0430\u043f\u0430\u0441\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private void shareCheeseWithNearbyRats() {
        try {
            if (this.getOwnerUUID() == null || this.getCheeseReserveCount() <= 1) {
                return;
            }
            TamedRat receiver = this.level().getEntitiesOfClass(TamedRat.class, this.getBoundingBox().inflate(8.0), other -> other != this && other.isAlive() && Objects.equals(other.getOwnerUUID(), this.getOwnerUUID()) && other.getCheeseReserveCount() + 1 < this.getCheeseReserveCount()).stream().min((first, second) -> Integer.compare(first.getCheeseReserveCount(), second.getCheeseReserveCount())).orElse(null);
            if (receiver == null) {
                return;
            }
            ItemStack moved = this.takeCheeseFromReserve(1);
            if (moved.isEmpty()) {
                return;
            }
            int accepted = receiver.addCheeseToReserve(moved);
            if (accepted <= 0) {
                this.addCheeseToReserve(moved);
                return;
            }
            if (!moved.isEmpty()) {
                this.addCheeseToReserve(moved);
            }
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u043e\u0434\u0435\u043b\u0438\u043b\u0430\u0441\u044c \u0441\u044b\u0440\u043e\u043c \u0441 \u043a\u0440\u044b\u0441\u043e\u0439 {}: \u0434\u043e\u043d\u043e\u0440={}, \u043f\u043e\u043b\u0443\u0447\u0430\u0442\u0435\u043b\u044c={}", this.getUUID(), receiver.getUUID(), this.getCheeseReserveCount(), receiver.getCheeseReserveCount());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u0434\u0435\u043b\u0438\u0442\u044c\u0441\u044f \u0441\u044b\u0440\u043e\u043c \u0443 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    public boolean eatCheeseFromStack(ItemStack stack, String sourceName) {
        try {
            if (stack.isEmpty()) {
                return false;
            }
            if (!TamedRat.isCheeseReserveItem(stack)) {
                throw new IllegalArgumentException("\u041a\u0440\u044b\u0441\u0430 \u043c\u043e\u0436\u0435\u0442 \u0435\u0441\u0442\u044c \u0442\u043e\u043b\u044c\u043a\u043e \u0441\u044b\u0440, \u043f\u043e\u043b\u0443\u0447\u0435\u043d \u0441\u0442\u0435\u043a: " + String.valueOf(stack));
            }
            if (!this.needsCheeseNow()) {
                return false;
            }
            stack.shrink(1);
            this.recordRatCheeseMeal(sourceName);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u043a\u043e\u0440\u043c\u0438\u0442\u044c \u043a\u0440\u044b\u0441\u0443 {} \u0438\u0437 \u0438\u0441\u0442\u043e\u0447\u043d\u0438\u043a\u0430 {}", this.getUUID(), sourceName, exception);
            throw exception;
        }
    }

    private void recordRatCheeseMeal(String sourceName) {
        try {
            int requirement = this.getDailyCheeseRequirement();
            this.cheeseEatenToday = Mth.clamp((int)(this.cheeseEatenToday + 1), (int)0, (int)requirement);
            this.lastFeedDay = this.getRatHungerDay();
            this.cachedHungerEfficiencyTick = -1000;
            this.syncRatHungerData();
            this.playSound(SoundEvents.GENERIC_EAT, 0.6f, 0.9f + this.getRandom().nextFloat() * 0.2f);
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0441\u044a\u0435\u043b\u0430 \u0441\u044b\u0440 \u0438\u0437 \u0438\u0441\u0442\u043e\u0447\u043d\u0438\u043a\u0430 '{}': {}/{}", this.getUUID(), sourceName, this.cheeseEatenToday, requirement);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u043f\u0438\u0441\u0430\u0442\u044c \u043a\u043e\u0440\u043c\u043b\u0435\u043d\u0438\u0435 \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private void ensureRatGenesInitialized(String reason) {
        try {
            if (((Boolean)this.getEntityData().get(RAT_GENES_INITIALIZED)).booleanValue()) {
                return;
            }
            this.initializeRandomRatGenes(reason);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0433\u0435\u043d\u044b \u043a\u0440\u044b\u0441\u044b {} \u043f\u043e \u043f\u0440\u0438\u0447\u0438\u043d\u0435 {}", this.getUUID(), reason, exception);
            throw exception;
        }
    }

    private void initializeRandomRatGenes(String reason) {
        try {
            RandomSource random = this.getRandom();
            this.setRatGenes(
                0.5f + random.nextFloat() * 2.0f,
                0.5f + random.nextFloat() * 2.0f,
                0.5f + random.nextFloat() * 2.0f,
                0.5f + random.nextFloat() * 2.0f,
                0.5f + random.nextFloat() * 2.0f,
                0.5f + random.nextFloat() * 2.0f,
                0.5f + random.nextFloat() * 2.0f,
                0.5f + random.nextFloat() * 2.0f);
            this.onUpgradeChanged();
            RatsMod.LOGGER.info("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u0430 \u0441\u043b\u0443\u0447\u0430\u0439\u043d\u044b\u0435 \u0433\u0435\u043d\u044b \u043f\u0440\u0438 {}: \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c={}, \u0437\u0434\u043e\u0440\u043e\u0432\u044c\u0435={}, \u0431\u0440\u043e\u043d\u044f={}, \u0443\u0440\u043e\u043d={}, \u0440\u0435\u0433\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u044f={}", this.getUUID(), reason, Float.valueOf(this.getRatGene(GENE_SPEED)), Float.valueOf(this.getRatGene(GENE_HEALTH)), Float.valueOf(this.getRatGene(GENE_ARMOR)), Float.valueOf(this.getRatGene(GENE_DAMAGE)), Float.valueOf(this.getRatGene(GENE_REGEN)));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u0441\u043b\u0443\u0447\u0430\u0439\u043d\u044b\u0435 \u0433\u0435\u043d\u044b \u043a\u0440\u044b\u0441\u044b {} \u043f\u0440\u0438 {}", this.getUUID(), reason, exception);
            throw exception;
        }
    }

    private void inheritRatGenesFromParents(TamedRat mother, TamedRat father) {
        try {
            mother.ensureRatGenesInitialized("mother_breeding");
            father.ensureRatGenesInitialized("father_breeding");
            this.setRatGenes(
                this.mixParentGene(mother.getRatGene(GENE_SPEED), father.getRatGene(GENE_SPEED), "speed"),
                this.mixParentGene(mother.getRatGene(GENE_HEALTH), father.getRatGene(GENE_HEALTH), "health"),
                this.mixParentGene(mother.getRatGene(GENE_ARMOR), father.getRatGene(GENE_ARMOR), "armor"),
                this.mixParentGene(mother.getRatGene(GENE_DAMAGE), father.getRatGene(GENE_DAMAGE), "damage"),
                this.mixParentGene(mother.getRatGene(GENE_REGEN), father.getRatGene(GENE_REGEN), "regen"),
                this.mixParentGene(mother.getRatGene(GENE_GOETY), father.getRatGene(GENE_GOETY), "goety"),
                this.mixParentGene(mother.getRatGene(GENE_IRON), father.getRatGene(GENE_IRON), "iron"),
                mixParentGene(mother.getCooldownGene(),father.getCooldownGene(),"cooldown"));
            this.onUpgradeChanged();
            RatsMod.LOGGER.info("\u041a\u0440\u044b\u0441\u0430 {} \u0443\u043d\u0430\u0441\u043b\u0435\u0434\u043e\u0432\u0430\u043b\u0430 \u0433\u0435\u043d\u044b: \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c={}, \u0437\u0434\u043e\u0440\u043e\u0432\u044c\u0435={}, \u0431\u0440\u043e\u043d\u044f={}, \u0443\u0440\u043e\u043d={}, \u0440\u0435\u0433\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u044f={}", this.getUUID(), Float.valueOf(this.getRatGene(GENE_SPEED)), Float.valueOf(this.getRatGene(GENE_HEALTH)), Float.valueOf(this.getRatGene(GENE_ARMOR)), Float.valueOf(this.getRatGene(GENE_DAMAGE)), Float.valueOf(this.getRatGene(GENE_REGEN)));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0443\u043d\u0430\u0441\u043b\u0435\u0434\u043e\u0432\u0430\u0442\u044c \u0433\u0435\u043d\u044b \u043a\u0440\u044b\u0441\u044b {} \u043e\u0442 \u0440\u043e\u0434\u0438\u0442\u0435\u043b\u0435\u0439 {} \u0438 {}", this.getUUID(), mother.getUUID(), father.getUUID(), exception);
            throw exception;
        }
    }

    private float mixParentGene(float firstParentGene, float secondParentGene, String geneName) {
        try {
            float min = Math.min(firstParentGene, secondParentGene);
            float max = Math.max(firstParentGene, secondParentGene);
            float mixed = min + this.getRandom().nextFloat() * (max - min);
            if (this.getRandom().nextFloat() <= 0.3f) {
                mixed = this.mutateRatGene(mixed, min, max, geneName);
            }
            return this.clampRatGene(mixed);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043c\u0435\u0448\u0430\u0442\u044c \u0433\u0435\u043d {} \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", geneName, this.getUUID(), exception);
            throw exception;
        }
    }

    private float mutateRatGene(float mixed, float parentMin, float parentMax, String geneName) {
        try {
            boolean canMutateUp;
            float mutation = 0.05f + this.getRandom().nextFloat() * 0.2f;
            boolean canMutateDown = parentMin > 0.5f;
            boolean bl = canMutateUp = parentMax < 2.5f;
            if (!canMutateDown && !canMutateUp) {
                return this.clampRatGene(mixed);
            }
            boolean mutateUp = canMutateUp && (!canMutateDown || this.getRandom().nextBoolean());
            float mutated = mutateUp ? parentMax + mutation : parentMin - mutation;
            RatsMod.LOGGER.debug("\u041c\u0443\u0442\u0430\u0446\u0438\u044f \u0433\u0435\u043d\u0430 {} \u0443 \u043a\u0440\u044b\u0441\u044b {}: {} -> {}", geneName, this.getUUID(), Float.valueOf(mixed), Float.valueOf(mutated));
            return this.clampRatGene(mutated);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043c\u0443\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0433\u0435\u043d {} \u0443 \u043a\u0440\u044b\u0441\u044b {}", geneName, this.getUUID(), exception);
            throw exception;
        }
    }

    private void setRatGenes(float speed, float health, float armor, float damage, float regen, float goety, float iron,float cooldown) {
        try {
            this.getEntityData().set(GENE_SPEED, Float.valueOf(this.clampRatGene(speed)));
            this.getEntityData().set(GENE_HEALTH, Float.valueOf(this.clampRatGene(health)));
            this.getEntityData().set(GENE_ARMOR, Float.valueOf(this.clampRatGene(armor)));
            this.getEntityData().set(GENE_DAMAGE, Float.valueOf(this.clampRatGene(damage)));
            this.getEntityData().set(GENE_REGEN, Float.valueOf(this.clampRatGene(regen)));
            getEntityData().set(GENE_GOETY, clampRatGene(goety));
            getEntityData().set(GENE_IRON, clampRatGene(iron));
            entityData.set(GENE_COOLDOWN,clampRatGene(cooldown));
            this.getEntityData().set(RAT_GENES_INITIALIZED, true);
            this.refreshDimensions();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u043f\u0438\u0441\u0430\u0442\u044c \u0433\u0435\u043d\u044b \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private void saveRatGenes(CompoundTag tag) {
        try {
            if (!((Boolean)this.getEntityData().get(RAT_GENES_INITIALIZED)).booleanValue()) {
                return;
            }
            CompoundTag genes = new CompoundTag();
            genes.putFloat("Speed", this.getRatGene(GENE_SPEED));
            genes.putFloat("Health", this.getRatGene(GENE_HEALTH));
            genes.putFloat("Armor", this.getRatGene(GENE_ARMOR));
            genes.putFloat("Damage", this.getRatGene(GENE_DAMAGE));
            genes.putFloat("Regen", this.getRatGene(GENE_REGEN));
            genes.putFloat("GoetyPower", getGoetyPowerGene());
            genes.putFloat("IronPower", getIronPowerGene());
            genes.putFloat("SpellCooldown",getCooldownGene());
            tag.put(RAT_GENES_TAG, (Tag)genes);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0433\u0435\u043d\u044b \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private void loadRatGenes(CompoundTag tag) {
        try {
            if (!tag.contains(RAT_GENES_TAG, 10)) {
                this.getEntityData().set(RAT_GENES_INITIALIZED, false);
                return;
            }
            CompoundTag genes = tag.getCompound(RAT_GENES_TAG);
            this.setRatGenes(genes.getFloat("Speed"), genes.getFloat("Health"), genes.getFloat("Armor"), genes.getFloat("Damage"), genes.getFloat("Regen"),
                readPowerGene(genes, "GoetyPower"), readPowerGene(genes, "IronPower"),readPowerGene(genes,"SpellCooldown"));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0433\u0435\u043d\u044b \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private float readPowerGene(CompoundTag genes, String key) {
        if (genes.contains(key, Tag.TAG_ANY_NUMERIC)) return genes.getFloat(key);
        long seed = getUUID().getMostSignificantBits() ^ getUUID().getLeastSignificantBits() ^ key.hashCode();
        return 0.5F + RandomSource.create(seed).nextFloat() * 2.0F;
    }


    public int getRatExperience() {
        return entityData.get(RAT_EXPERIENCE);
    }

    public int getRatLevel() {
        return RatProgression.levelFor(getRatExperience());
    }

    void grantRatExperience(int amount) {
        int oldTotal = getRatExperience();
        int total = Math.min(RatProgression.MAX_EXPERIENCE, oldTotal + amount);
        if (total == oldTotal) return;
        int oldLevel = getRatLevel();
        ensureRatGenesInitialized("experience");
        entityData.set(RAT_EXPERIENCE, total);
        if (getRatLevel() > oldLevel) {
            refreshGrowthPreservingHealth();
            playSound(SoundEvents.PLAYER_LEVELUP, 0.35F, 1.35F);
        }
    }

    public boolean eatLevelCandy(int levels) {
        int level = getRatLevel();
        if (level == RatProgression.MAX_LEVEL) return false;
        int target = Math.min(RatProgression.MAX_LEVEL, level + levels);
        grantRatExperience(RatProgression.threshold(target) - RatProgression.threshold(level));
        return true;
    }

    public float improveGene(RatProgression.Gene gene) {
        ensureRatGenesInitialized("gene_treat");
        EntityDataAccessor<Float> key = switch (gene) {
            case HEALTH -> GENE_HEALTH;
            case DAMAGE -> GENE_DAMAGE;
            case ARMOR -> GENE_ARMOR;
            case SPEED -> GENE_SPEED;
            case REGEN -> GENE_REGEN;
            case GOETY -> GENE_GOETY;
            case IRON -> GENE_IRON;
            case COOLDOWN -> GENE_COOLDOWN;
        };
        float old = getRatGene(key);
        if (old >= 2.5F) return 0;
        float value = clampRatGene(old + 0.25F);
        entityData.set(key, value);
        if (gene == RatProgression.Gene.HEALTH) refreshDimensions();
        refreshGrowthPreservingHealth();
        return value;
    }

    private void refreshGrowthPreservingHealth() {
        float fraction = getHealth() / getMaxHealth();
        rebuildRatAttributes();
        setHealth(fraction * getMaxHealth());
    }

    @Override
    public boolean killedEntity(net.minecraft.server.level.ServerLevel level, LivingEntity victim) {
        boolean result = super.killedEntity(level, victim);
        if (isTame() && victim instanceof net.minecraft.world.entity.monster.Enemy) {
            int earned=10+Mth.ceil(Math.min(980.0F,victim.getMaxHealth())/2.0F);
            int award=getEvolution()==RatEvolution.PLAIN?Math.round(earned*1.15F):earned;
            grantRatExperience(award);
            Mastery.training(this,award);
        }
        return result;
    }

    public float getProfileAttack() {
        return entityData.get(PROFILE_ATTACK);
    }

    float grownSpellPower(float gene) {
        return (float) (gene * (1.0 + 3.0 * RatProgression.growth(getRatLevel())));
    }

    private void applySpellPowerGenes() {
        RatSpellAttributes.apply(this, RatSpellAttributes.GOETY, grownSpellPower(getGoetyPowerGene()), AttributeModifier.Operation.ADDITION);
        RatSpellAttributes.apply(this, RatSpellAttributes.IRON, grownSpellPower(getIronPowerGene()) - 1.0, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    private void applyRatGeneAttributeModifiers() {
        try {
            if (!((Boolean)this.getEntityData().get(RAT_GENES_INITIALIZED)).booleanValue()) {
                return;
            }
            this.addRatGeneAttributeModifier(Attributes.MOVEMENT_SPEED, "gene_speed", this.getRatGene(GENE_SPEED));
            this.addRatGeneAttributeModifier(Attributes.FLYING_SPEED, "gene_flying_speed", this.getRatGene(GENE_SPEED));
            applyGrowingAttribute(Attributes.MAX_HEALTH, "gene_health", getRatGene(GENE_HEALTH), 3000.0 + 1000.0 * (getRatGene(GENE_HEALTH) - 0.5));
            applyGrowingAttribute(Attributes.ARMOR, "gene_armor", getRatGene(GENE_ARMOR), 20.0);
            applyGrowingAttribute(Attributes.ATTACK_DAMAGE, "gene_damage", getRatGene(GENE_DAMAGE), 100.0 + 100.0 * (getRatGene(GENE_DAMAGE) - 0.5));
            applySpellPowerGenes();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c \u0433\u0435\u043d\u044b \u043a \u0430\u0442\u0440\u0438\u0431\u0443\u0442\u0430\u043c \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private void applyGrowingAttribute(Attribute attr, String name, float gene, double graduate) {
        addRatGeneAttributeModifier(attr, name, gene);
        if (getRatLevel() == 1) return;
        AttributeInstance instance = Objects.requireNonNull(getAttribute(attr));
        // Flat modifiers are applied before MULTIPLY_BASE. Retain the gene scaling on equipment.
        double addition = (graduate / gene - instance.getBaseValue()) * RatProgression.growth(getRatLevel());
        AttributeModifier modifier = new AttributeModifier(name + "_level", addition, AttributeModifier.Operation.ADDITION);
        instance.addTransientModifier(modifier);
        attributeChanges.put(attr, modifier);
    }

    private void addRatGeneAttributeModifier(Attribute attribute, String modifierName, float multiplier) {
        try {
            AttributeInstance attributeInstance = this.getAttribute(attribute);
            if (attributeInstance == null) {
                throw new IllegalStateException("\u0423 \u043a\u0440\u044b\u0441\u044b \u043d\u0435\u0442 \u0430\u0442\u0440\u0438\u0431\u0443\u0442\u0430 " + modifierName);
            }
            AttributeModifier modifier = new AttributeModifier(java.util.UUID.nameUUIDFromBytes(("rats:"+modifierName).getBytes(java.nio.charset.StandardCharsets.UTF_8)), modifierName, (double)multiplier - 1.0, AttributeModifier.Operation.MULTIPLY_BASE);
            attributeInstance.addTransientModifier(modifier);
            this.attributeChanges.put(attribute, modifier);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0433\u0435\u043d\u0435\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043c\u043e\u0434\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u0440 {} \u043a\u0440\u044b\u0441\u0435 {}", modifierName, this.getUUID(), exception);
            throw exception;
        }
    }

    private float getRatGene(EntityDataAccessor<Float> gene) {
        try {
            if (!((Boolean)this.getEntityData().get(RAT_GENES_INITIALIZED)).booleanValue()) {
                return 1.0f;
            }
            return this.clampRatGene(((Float)this.getEntityData().get(gene)).floatValue());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u0433\u0435\u043d \u043a\u0440\u044b\u0441\u044b {}", this.getUUID(), exception);
            throw exception;
        }
    }

    private float clampRatGene(float value) {
        try {
            return Mth.clamp((float)value, (float)0.5f, (float)2.5f);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0438\u0442\u044c \u0433\u0435\u043d \u043a\u0440\u044b\u0441\u044b {} \u0441\u043e \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435\u043c {}", this.getUUID(), Float.valueOf(value), exception);
            throw exception;
        }
    }

    private float calculateRatModelScale(float healthGene) {
        try {
            float normalized = (this.clampRatGene(healthGene) - 0.5f) / 2.0f;
            return 0.75f + normalized * 0.75f;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u0441\u0447\u0438\u0442\u0430\u0442\u044c \u0440\u0430\u0437\u043c\u0435\u0440 \u043c\u043e\u0434\u0435\u043b\u0438 \u043a\u0440\u044b\u0441\u044b {} \u043f\u043e \u0433\u0435\u043d\u0443 \u0437\u0434\u043e\u0440\u043e\u0432\u044c\u044f {}", this.getUUID(), Float.valueOf(healthGene), exception);
            throw exception;
        }
    }

    private void tryInheritBabyUpgrade(TamedRat father, TamedRat mother, TamedRat baby) {
        try {
            if (this.getRandom().nextFloat() > 0.35f) {
                return;
            }
            ArrayList<ItemStack> pool = new ArrayList<ItemStack>();
            this.collectParentUpgradesForGenetics(father, pool);
            this.collectParentUpgradesForGenetics(mother, pool);
            pool.removeIf(stack -> stack.isEmpty() || false || !(stack.getItem() instanceof BaseRatUpgradeItem) || stack.getItem() instanceof CombinedRatUpgradeItem);
            if (pool.size() < 2) {
                return;
            }
            
            for (int attempt = 0; attempt < 12; ++attempt) {
                ItemStack combined;
                ItemStack second;
                ItemStack first = ((ItemStack)pool.get(this.getRandom().nextInt(pool.size()))).copy();
                if (ItemStack.isSameItemSameTags((ItemStack)first, (ItemStack)(second = ((ItemStack)pool.get(this.getRandom().nextInt(pool.size()))).copy())) || !CombinedRatUpgradeItem.canCombineWithUpgrade(combined = new ItemStack((ItemLike)RatsItemRegistry.RAT_UPGRADE_COMBINED.get()), first) || !CombinedRatUpgradeItem.canCombineWithUpgrade(combined = UpgradeCombinerBlockEntity.mergeUpgradeIntoCombined(combined, first), second)) continue;
                combined = UpgradeCombinerBlockEntity.mergeUpgradeIntoCombined(combined, second);
                for (int index : RatContainer.UPGRADES) {
                    if (!baby.upgradeInSlot(index).isEmpty()) continue;
                    baby.getInventory().setItem(index, combined);
                    return;
                }
                return;
            }
        }
        catch (Exception e) {
            RatsMod.LOGGER.error("tryInheritBabyUpgrade: {}", e.getMessage(), e);
        }
    }

    private void collectParentUpgradesForGenetics(TamedRat parent, List<ItemStack> pool) {
        for (int index : RatContainer.UPGRADES) {
            ItemStack equipped = parent.upgradeInSlot(index);
            if (equipped.isEmpty()) continue;
            Item item = equipped.getItem();
            if (item instanceof CombinedUpgrade) {
                CombinedUpgrade combined = (CombinedUpgrade)item;
                CompoundTag tag = equipped.getTag();
                if (tag == null || !tag.contains("Items", 9)) continue;
                NonNullList<ItemStack> contents = NonNullList.withSize((int)combined.getUpgradeSlots(), ItemStack.EMPTY);
                ContainerHelper.loadAllItems((CompoundTag)tag, (NonNullList)contents);
                for (ItemStack inner : contents) {
                    if (inner.isEmpty()) continue;
                    pool.add(inner);
                }
                continue;
            }
            if (!(equipped.getItem() instanceof BaseRatUpgradeItem)) continue;
            pool.add(equipped);
        }
    }
 public boolean hasWorkerUpgrade() { return RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_WORKER.get()); }
 public void scheduleUpgradeRefreshAfterLoad() { onUpgradeChanged(); }

public Optional<GlobalPos> getWorkAnchorPoint() { return getEntityData().get(WORK_ANCHOR); }

public void setWorkAnchorPoint(GlobalPos p) { getEntityData().set(WORK_ANCHOR,Optional.ofNullable(p)); }

private int tubeClimbDirection;
private RatTubeTransit.Session tubeTransit;
    public void setTubeClimbDirection(int direction) {
        this.tubeClimbDirection = direction;
    }
    public int getTubeClimbDirection() {
        return this.tubeClimbDirection;
    }
    public RatTubeTransit.Session getTubeTransit() {
        return this.tubeTransit;
    }
    public void setTubeTransit(@Nullable RatTubeTransit.Session session) {
        this.tubeTransit = session;
    }
    public void clearTubeTransit() {
        this.tubeTransit = null;
    }
    public boolean isTubeTransitActive() {
        return this.tubeTransit != null && this.tubeTransit.isActive();
    }
    public void onTubeSegmentTick(BlockPos pos, BlockState state) {
        try {
            if (!this.isInTube()) {
                return;
            }
            this.fallDistance = 0.0f;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041e\u0448\u0438\u0431\u043a\u0430 \u0442\u0438\u043a\u0430 \u043a\u0440\u044b\u0441\u044b {} \u0432 \u0442\u0440\u0443\u0431\u0435 {}: {}", this.getUUID(), pos, exception.getMessage(), exception);
            throw exception;
        }
    }
    public boolean canCollideWith(Entity entity) {
        TamedRat other;
        if (this.isInTube() && entity instanceof TamedRat && (other = (TamedRat)entity).isInTube()) {
            return false;
        }
        return super.canCollideWith(entity);
    }

@Override public BlockPos getSearchCenter(){var anchor=getWorkAnchorPoint();return anchor.filter(p->p.dimension().equals(level().dimension())).map(GlobalPos::pos).orElseGet(super::getSearchCenter);}
}
