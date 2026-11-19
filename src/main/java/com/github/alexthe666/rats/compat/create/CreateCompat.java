package com.github.alexthe666.rats.compat.create;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

public final class CreateCompat {
    private static final double CREATE_SEAT_RETURN_DISTANCE = 4.5;
    private static final Set<String> CREATE_WORK_BLOCK_IDS = Set.of("depot", "weighted_ejector", "basin", "mechanical_crafter", "deployer", "mechanical_press", "mechanical_mixer", "mechanical_saw", "mechanical_drill", "belt");
    private static final Set<String> CREATE_OPERATOR_BLOCK_IDS = Set.of("hand_crank", "valve_handle", "stockpile_switch", "factory_gauge", "analog_lever", "sequenced_gearshift", "gearshift", "clutch");
    private static final Set<String> CREATE_SEAT_BLOCK_IDS = Set.of("seat", "white_seat", "orange_seat", "magenta_seat", "light_blue_seat", "yellow_seat", "lime_seat", "pink_seat", "gray_seat", "light_gray_seat", "cyan_seat", "purple_seat", "blue_seat", "brown_seat", "green_seat", "red_seat", "black_seat");
    private static final Set<String> APPLICATION_ITEM_IDS = Set.of("create:andesite_alloy", "create:brass_ingot", "minecraft:copper_ingot", "create:sturdy_sheet");
    private static final Map<ResourceLocation, List<Recipe<?>>> RECIPE_TYPE_CACHE = new HashMap();
    private static final Map<String, List<Recipe<?>>> SEQUENCED_APPLICATION_CACHE = new HashMap();
    private static final Map<String, Optional<Recipe<?>>> APPLICATION_MATCH_CACHE = new HashMap();
    private static final Map<String, Field> FIELD_CACHE = new HashMap<String, Field>();
    private static final Map<String, Method> METHOD_CACHE = new HashMap<String, Method>();
    private static final Set<String> MISSING_METHOD_CACHE = new HashSet<String>();
    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap();
    private static final Map<Class<?>, Boolean> APPLICATION_RECIPE_CLASS_CACHE = new HashMap();
    private static final Map<String, Boolean> APPLICATION_ITEM_CACHE = new HashMap<String, Boolean>();
    private static final Map<Recipe<?>, List<Recipe<?>>> SEQUENCED_STEP_RECIPE_CACHE = new IdentityHashMap();
    private static final ApplicationResult EMPTY_APPLICATION_RESULT = new ApplicationResult(List.of(), false, "none");
    @Nullable
    private static Object cachedRecipeManager;

    private CreateCompat() {
        throw new IllegalStateException("????????? ????? Create ?? ????????? ????????");
    }

    public static boolean isCreateWorkBlock(BlockState state) {
        try {
            return CreateCompat.isCreateLoadedBlock(state) && (state.is(RatsBlockTags.CREATE_WORK_BLOCKS) || CREATE_WORK_BLOCK_IDS.contains(CreateCompat.getBlockIdPath(state)));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??????? ???? Create {}", CreateCompat.getBlockIdSafely(state), exception);
            throw exception;
        }
    }

    public static boolean isCreateOperatorBlock(BlockState state) {
        try {
            return CreateCompat.isCreateLoadedBlock(state) && (state.is(RatsBlockTags.CREATE_OPERATOR_BLOCKS) || CREATE_OPERATOR_BLOCK_IDS.contains(CreateCompat.getBlockIdPath(state)));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??????????? ???? Create {}", CreateCompat.getBlockIdSafely(state), exception);
            throw exception;
        }
    }

    public static boolean isCreateSeatBlock(BlockState state) {
        try {
            return CreateCompat.isCreateSeat(state);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??????? Create {}", CreateCompat.getBlockIdSafely(state), exception);
            throw exception;
        }
    }

    public static boolean isCreateBeltBlock(BlockState state) {
        try {
            return CreateCompat.isCreateBelt(state);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ????? Create {}", CreateCompat.getBlockIdSafely(state), exception);
            throw exception;
        }
    }

    public static boolean canWorkerUse(BlockState state) {
        try {
            return CreateCompat.isCreateWorkBlock(state) || CreateCompat.isCreateOperatorBlock(state) || CreateCompat.isCreateSeat(state);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ???????? ???????? Create ??? ????? {}", CreateCompat.getBlockIdSafely(state), exception);
            throw exception;
        }
    }

    public static boolean canWorkerReachRemotely(BlockState state) {
        try {
            return CreateCompat.isCreateBelt(state);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ????????????? ????? Create {}", CreateCompat.getBlockIdSafely(state), exception);
            throw exception;
        }
    }

    public static boolean isKnownApplicationItem(ItemStack stack) {
        try {
            return !stack.isEmpty() && APPLICATION_ITEM_IDS.contains(CreateCompat.getItemId(stack));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ???????? ?????????? Create {}", stack, exception);
            throw exception;
        }
    }

    public static boolean isKnownApplicationItem(Level level, ItemStack stack) {
        try {
            ServerLevel serverLevel;
            block11: {
                block10: {
                    if (CreateCompat.isKnownApplicationItem(stack)) {
                        return true;
                    }
                    if (!(level instanceof ServerLevel)) break block10;
                    serverLevel = (ServerLevel)level;
                    if (!stack.isEmpty() && RatsMod.CREATE_LOADED) break block11;
                }
                return false;
            }
            CreateCompat.ensureRecipeCachesValid(serverLevel);
            String itemKey = CreateCompat.getItemId(stack);
            Boolean cached = APPLICATION_ITEM_CACHE.get(itemKey);
            if (cached != null) {
                return cached;
            }
            for (Recipe<?> holder : CreateCompat.getDeployingRecipes(serverLevel)) {
                Recipe recipe = holder;
                if (!CreateCompat.hasApplicationRecipeMethods(recipe) || !CreateCompat.ingredientAccepts(CreateCompat.getRequiredHeldItem(recipe), stack)) continue;
                APPLICATION_ITEM_CACHE.put(itemKey, true);
                return true;
            }
            for (Recipe<?> holder : CreateCompat.getSequencedAssemblyDefinitions(serverLevel)) {
                for (Recipe<?> recipe : CreateCompat.getSequencedAssemblyStepRecipes(holder)) {
                    if (!CreateCompat.hasApplicationRecipeMethods(recipe) || !CreateCompat.ingredientAccepts(CreateCompat.getRequiredHeldItem(recipe), stack)) continue;
                    APPLICATION_ITEM_CACHE.put(itemKey, true);
                    return true;
                }
            }
            APPLICATION_ITEM_CACHE.put(itemKey, false);
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ???????? ?????????? Create {} ?? ????????", stack, exception);
            throw exception;
        }
    }

    public static boolean workBlock(TamedRat rat, BlockPos pos) {
        try {
            if (!RatsMod.CREATE_LOADED) {
                RatsMod.LOGGER.debug("????? {} ?? ????? ???????? ? Create ? {}: ??? Create ?? ????????", rat.getUUID(), pos);
                return false;
            }
            Level level = rat.level();
            if (!(level instanceof ServerLevel)) {
                throw new IllegalStateException("???????? Create ????????? ?????? ?? ??????? ???????");
            }
            ServerLevel serverLevel = (ServerLevel)level;
            BlockState state = serverLevel.getBlockState(pos);
            if (!CreateCompat.canWorkerUse(state)) {
                RatsMod.LOGGER.debug("????? {} ?? ????? ???????? ? {}: ???? ?? ?????? ? ???????????? Create-?????", rat.getUUID(), CreateCompat.getBlockIdSafely(state));
                return false;
            }
            if (CreateCompat.applyItemOnBelt(rat, serverLevel, pos)) {
                return true;
            }
            FakePlayer worker = FakePlayerFactory.getMinecraft((ServerLevel)serverLevel);
            worker.setPos(rat.getX(), rat.getY(), rat.getZ());
            worker.setItemInHand(InteractionHand.MAIN_HAND, rat.getMainHandItem().copy());
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf((Vec3i)pos), rat.createWorkFacing, pos, false);
            boolean used = CreateCompat.invokeUseItemOn(state, (Level)serverLevel, (Player)worker, hit);
            if (!used) {
                used = CreateCompat.invokeUseWithoutItem(state, (Level)serverLevel, (Player)worker, hit);
            }
            rat.setItemInHand(InteractionHand.MAIN_HAND, worker.getItemInHand(InteractionHand.MAIN_HAND).copy());
            if (CreateCompat.isCreateSeat(state)) {
                worker.stopRiding();
                CreateCompat.mountRatOnSeat(rat, pos);
                used = true;
            }
            if (used) {
                RatsMod.LOGGER.debug("????? {} ????????? Create-???????? ?? ????? {} ? {}", rat.getUUID(), CreateCompat.getBlockIdSafely(state), pos);
            }
            return used;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("????? {} ?? ?????? ????????? Create-???????? ? {}", rat.getUUID(), pos, exception);
            throw exception;
        }
    }

    public static boolean isMountedOnCreateSeat(TamedRat rat) {
        try {
            Entity vehicle = rat.getVehicle();
            return vehicle != null && CreateCompat.isSeatEntity(vehicle);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??????? Create ??? ????? {}", rat.getUUID(), exception);
            throw exception;
        }
    }

    public static boolean returnRatToCreateSeatIfConfigured(TamedRat rat) {
        try {
            Optional<GlobalPos> workAnchor = rat.getWorkAnchorPoint();
            if (workAnchor.isEmpty() || !workAnchor.get().dimension().equals(rat.level().dimension())) {
                return false;
            }
            BlockPos seatPos = workAnchor.get().pos();
            BlockState state = rat.level().getBlockState(seatPos);
            if (!CreateCompat.isCreateSeat(state)) {
                return false;
            }
            if (rat.distanceToSqr(Vec3.atCenterOf((Vec3i)seatPos)) > 20.25) {
                return false;
            }
            CreateCompat.mountRatOnSeat(rat, seatPos);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ??????? ????? {} ?? ??????? Create", rat.getUUID(), exception);
            throw exception;
        }
    }

    private static boolean applyItemOnBelt(TamedRat rat, ServerLevel level, BlockPos pos) {
        try {
            BlockEntity sourceEntity = level.getBlockEntity(pos);
            BlockEntity beltEntity = CreateCompat.resolveBeltBlockEntity(sourceEntity);
            if (beltEntity == null) {
                return false;
            }
            ItemStack held = rat.getMainHandItem();
            if (held.isEmpty()) {
                return false;
            }
            Object inventory = CreateCompat.getBeltInventory(beltEntity);
            List<?> transportedItems = CreateCompat.getTransportedItems(inventory);
            if (transportedItems.isEmpty()) {
                return false;
            }
            int targetSegment = CreateCompat.getBeltSegmentIndex(sourceEntity);
            for (Object transported : new ArrayList(transportedItems)) {
                if (!CreateCompat.isTransportedOnSegment(transported, targetSegment)) continue;
                ItemStack beltStack = CreateCompat.getTransportedStack(transported);
                ApplicationResult result = CreateCompat.getApplicationResult(level, rat.getRandom(), held, beltStack);
                if (result.results().isEmpty()) continue;
                if (!result.keepHeldItem()) {
                    held.shrink(1);
                }
                CreateCompat.applyBeltOutputs(level, inventory, transported, beltStack, result.results());
                CreateCompat.markBeltInventoryDirty(beltEntity);
                if (sourceEntity != null && sourceEntity != beltEntity) {
                    CreateCompat.markBeltInventoryDirty(sourceEntity);
                }
                RatsMod.LOGGER.debug("????? {} ????????? {} ? {} ?? ????? {} ?? ??????? {}", rat.getUUID(), held, beltStack, pos, result.recipeId());
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("????? {} ?? ?????? ????????? ??????? ?? ????? Create {}", rat.getUUID(), pos, exception);
            throw exception;
        }
    }

    private static ApplicationResult getApplicationResult(ServerLevel level,RandomSource random,ItemStack held,ItemStack target){
 if(held.isEmpty()||target.isEmpty())return ApplicationResult.empty();
 ensureRecipeCachesValid(level);RecipeWrapper wrapper=createRecipeWrapper(target,held);
 // Sequenced recipes carry a per-input forced output; refresh them for every operation.
 for(Recipe<?> recipe:getSequencedAssemblyApplicationRecipes(level,target)){
  if(!applicationRecipeAcceptsHeldItem(recipe,held))continue;
  ApplicationResult result=resolveApplicationRecipe(recipe,wrapper,level,random,held,target);
  if(!result.results().isEmpty())return result;
 }
 String key=getApplicationMatchCacheKey(held,target);
 Optional<Recipe<?>> cached=APPLICATION_MATCH_CACHE.get(key);
 if(cached!=null&&cached.isPresent()){
  ApplicationResult result=resolveApplicationRecipe(cached.get(),wrapper,level,random,held,target);
  if(!result.results().isEmpty())return result;
 }
 for(Recipe<?> recipe:getDeployingRecipes(level)){
  if(!applicationRecipeAcceptsHeldItem(recipe,held))continue;
  ApplicationResult result=resolveApplicationRecipe(recipe,wrapper,level,random,held,target);
  if(!result.results().isEmpty()){APPLICATION_MATCH_CACHE.put(key,Optional.of(recipe));return result;}
 }
 return ApplicationResult.empty();
}

    private static boolean applicationRecipeAcceptsHeldItem(Recipe<?> recipe, ItemStack held) {
        try {
            return CreateCompat.hasApplicationRecipeMethods(recipe) && CreateCompat.ingredientAccepts(CreateCompat.getRequiredHeldItem(recipe), held);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u043f\u0440\u0438\u043c\u0435\u043d\u0435\u043d\u0438\u044f Create {} \u0434\u043b\u044f \u0440\u0435\u0446\u0435\u043f\u0442\u0430 {}", held, recipe, exception);
            throw exception;
        }
    }

    private static void ensureRecipeCachesValid(ServerLevel level) {
        try {
            RecipeManager recipeManager = level.getRecipeManager();
            if (cachedRecipeManager == recipeManager) {
                return;
            }
            cachedRecipeManager = recipeManager;
            RECIPE_TYPE_CACHE.clear();
            SEQUENCED_APPLICATION_CACHE.clear();
            APPLICATION_MATCH_CACHE.clear();
            APPLICATION_RECIPE_CLASS_CACHE.clear();
            APPLICATION_ITEM_CACHE.clear();
            SEQUENCED_STEP_RECIPE_CACHE.clear();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043a\u0435\u0448 \u0440\u0435\u0446\u0435\u043f\u0442\u043e\u0432 Create", (Throwable)exception);
            throw exception;
        }
    }

    private static String getApplicationMatchCacheKey(ItemStack held, ItemStack target) {
        try {
            return CreateCompat.getItemId(held) + "|" + String.valueOf(held.getTag()) + "|" + CreateCompat.getItemId(target) + "|" + String.valueOf(target.getTag());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u043a\u043b\u044e\u0447 \u043a\u0435\u0448\u0430 \u0440\u0435\u0446\u0435\u043f\u0442\u0430 Create \u0434\u043b\u044f {} \u0438 {}", held, target, exception);
            throw exception;
        }
    }

    private static ApplicationResult resolveApplicationRecipe(Recipe<?> holder, RecipeWrapper wrapper, ServerLevel level, RandomSource random, ItemStack held, ItemStack target) {
        try {
            Recipe recipe = holder;
            if (!CreateCompat.hasApplicationRecipeMethods(recipe)) {
                return ApplicationResult.empty();
            }
            if (!CreateCompat.recipeMatches(recipe, wrapper, level)) {
                return ApplicationResult.empty();
            }
            List<ItemStack> outputs = CreateCompat.getRecipeOutputs(recipe, wrapper, level, random);
            if (outputs.isEmpty()) {
                RatsMod.LOGGER.error("?????? Create {} ???????, ?? ?? ????? ????????? ??? {} + {}", holder.getId(), target, held);
                return ApplicationResult.empty();
            }
            return new ApplicationResult(outputs, CreateCompat.shouldKeepHeldItem(recipe), holder.getId().toString());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ?????? ?????????? Create {}", holder.getId(), exception);
            throw exception;
        }
    }

    private static RecipeWrapper createRecipeWrapper(ItemStack target, ItemStack held) {
        try {
            ItemStackHandler handler = new ItemStackHandler(2);
            handler.setStackInSlot(0, target.copy());
            handler.setStackInSlot(1, held.copy());
            return new RecipeWrapper(handler);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ??????? ???? ??????? Create ??? {} ? {}", target, held, exception);
            throw exception;
        }
    }

    private static boolean recipeMatches(Recipe<?> recipe, RecipeWrapper wrapper, ServerLevel level) {
        try {
            return ((Recipe)recipe).matches(wrapper, (Level)level);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ?????? Create {}", recipe, exception);
            throw exception;
        }
    }

    private static List<ItemStack> getRecipeOutputs(Recipe<?> recipe, RecipeWrapper wrapper, ServerLevel level, RandomSource random) {
        try {
            ItemStack assembled;
            ArrayList<ItemStack> results = new ArrayList<ItemStack>();
            try {
                Object rolled;
                Optional<Method> rollResults = CreateCompat.findOptionalMethod(recipe.getClass(), "rollResults");
                if (rollResults.isPresent() && (rolled = rollResults.get().invoke(recipe)) instanceof List) {
                    List list = (List)rolled;
                    for (Object object : list) {
                        ItemStack stack;
                        if (!(object instanceof ItemStack) || (stack = (ItemStack)object).isEmpty()) continue;
                        results.add(stack.copy());
                    }
                }
            }
            catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("?? ??????? ???????? ????????? ?????????? ??????? Create", exception);
            }
            if (results.isEmpty() && !(assembled = ((Recipe)recipe).assemble(wrapper, level.registryAccess())).isEmpty()) {
                results.add(assembled.copy());
            }
            return results;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ????????? ??????? Create {}", recipe, exception);
            throw exception;
        }
    }

    private static boolean shouldKeepHeldItem(Recipe<?> recipe) {
        try {
            Boolean keep;
            Method method = CreateCompat.findMethod(recipe.getClass(), "shouldKeepHeldItem", new Class[0]);
            Object value = method.invoke(recipe, new Object[0]);
            return value instanceof Boolean && (keep = (Boolean)value) != false;
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?????? Create ?? ??????? ?????? ?????????: " + recipe.getClass().getName(), exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ?????? ????????? ??????? Create {}", recipe, exception);
            throw exception;
        }
    }

    private static boolean hasApplicationRecipeMethods(Recipe<?> recipe) {
        try {
            Boolean cached = APPLICATION_RECIPE_CLASS_CACHE.get(recipe.getClass());
            if (cached != null) {
                return cached;
            }
            boolean hasMethods = CreateCompat.findOptionalMethod(recipe.getClass(), "getRequiredHeldItem", new Class[0]).isPresent() && CreateCompat.findOptionalMethod(recipe.getClass(), "getProcessedItem", new Class[0]).isPresent();
            APPLICATION_RECIPE_CLASS_CACHE.put(recipe.getClass(), hasMethods);
            return hasMethods;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??? ??????? Create {}", recipe, exception);
            throw exception;
        }
    }

    private static Ingredient getRequiredHeldItem(Recipe<?> recipe) {
        try {
            Object ingredient = CreateCompat.findMethod(recipe.getClass(), "getRequiredHeldItem", new Class[0]).invoke(recipe, new Object[0]);
            if (ingredient instanceof Ingredient) {
                Ingredient typed = (Ingredient)ingredient;
                return typed;
            }
            throw new IllegalStateException("?????? Create ?????? ???????? ??? ????????????? ???????????: " + recipe.getClass().getName());
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ???????? ???????????? ?????????? ??????? Create: " + recipe.getClass().getName(), exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ???????????? ?????????? ??????? Create {}", recipe, exception);
            throw exception;
        }
    }

    private static boolean ingredientAccepts(Ingredient ingredient, ItemStack stack) {
        try {
            return ingredient.test(stack);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ?????????? Create ??? {}", stack, exception);
            throw exception;
        }
    }

    private static List<Recipe<?>> getDeployingRecipes(ServerLevel level) {
        try {
            CreateCompat.ensureRecipeCachesValid(level);
            ResourceLocation cacheKey = new ResourceLocation((String)"rats", (String)"create_deploying_bundle");
            List<Recipe<?>> cached = RECIPE_TYPE_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            List<Recipe<?>> recipes = new ArrayList();
            CreateCompat.addRecipesForType(level, recipes, new ResourceLocation((String)"create", (String)"item_application"));
            CreateCompat.addRecipesForType(level, recipes, new ResourceLocation((String)"create", (String)"deploying"));
            CreateCompat.addRecipesForType(level, recipes, new ResourceLocation((String)"create", (String)"sandpaper_polishing"));
            recipes = List.copyOf(recipes);
            RECIPE_TYPE_CACHE.put(cacheKey, recipes);
            return recipes;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ??????? ?????????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static List<Recipe<?>> getSequencedAssemblyApplicationRecipes(ServerLevel level, ItemStack target) {
        try {
            CreateCompat.ensureRecipeCachesValid(level);
            String cacheKey = CreateCompat.getItemId(target) + "|" + String.valueOf(target.getTag());

            RecipeType deployingType = (RecipeType)BuiltInRegistries.RECIPE_TYPE.get(new ResourceLocation((String)"create", (String)"deploying"));
            if (deployingType == null) {
                RatsMod.LOGGER.error("??? ??????? create:deploying ?? ??????, ???????????????? ?????? Create ??????????");
                return List.of();
            }
            Class<?> assemblyClass = CreateCompat.findClass("com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe");
            Class<?> deployerRecipeClass = CreateCompat.findClass("com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe");
            Method method = CreateCompat.findMethod(assemblyClass, "getRecipes", Level.class, ItemStack.class, RecipeType.class, Class.class);
            Object value = method.invoke(null, level, target.copy(), deployingType, deployerRecipeClass);
            if (value instanceof java.util.stream.Stream<?> stream) { value = stream.toList(); }
            if (value instanceof List) {
                List list = (List)value;
                List<Recipe<?>> recipes = List.copyOf(list);
                
                return recipes;
            }
            throw new IllegalStateException("Create ?????? ??????????? ?????? ????? ???????????????? ??????: " + String.valueOf(value));
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ???????? ???? ???????????????? ?????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??? ???????????????? ?????? Create ??? {}", target, exception);
            throw exception;
        }
    }

    private static List<Recipe<?>> getSequencedAssemblyDefinitions(ServerLevel level) {
        try {
            CreateCompat.ensureRecipeCachesValid(level);
            ResourceLocation cacheKey = new ResourceLocation((String)"rats", (String)"create_sequenced_assembly");
            List<Recipe<?>> cached = RECIPE_TYPE_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            List<Recipe<?>> recipes = new ArrayList();
            CreateCompat.addRecipesForType(level, recipes, new ResourceLocation((String)"create", (String)"sequenced_assembly"));
            recipes = List.copyOf(recipes);
            RECIPE_TYPE_CACHE.put(cacheKey, recipes);
            return recipes;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ??????? ???????????????? ?????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static List<Recipe<?>> getSequencedAssemblyStepRecipes(Recipe<?> recipe) {
        try {
            List<Recipe<?>> cached = SEQUENCED_STEP_RECIPE_CACHE.get(recipe);
            if (cached != null) {
                return cached;
            }
            Method getSequence = CreateCompat.findMethod(recipe.getClass(), "getSequence", new Class[0]);
            Object sequence = getSequence.invoke(recipe, new Object[0]);
            if (!(sequence instanceof List)) {
                SEQUENCED_STEP_RECIPE_CACHE.put(recipe, List.of());
                return List.of();
            }
            List list = (List)sequence;
            List<Recipe<?>> recipes = new ArrayList();
            for (Object step : list) {
                Method getRecipe = CreateCompat.findMethod(step.getClass(), "getRecipe", new Class[0]);
                Object inner = getRecipe.invoke(step, new Object[0]);
                if (!(inner instanceof Recipe)) continue;
                Recipe innerRecipe = (Recipe)inner;
                recipes.add(innerRecipe);
            }
            recipes = List.copyOf(recipes);
            SEQUENCED_STEP_RECIPE_CACHE.put(recipe, recipes);
            return recipes;
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ????????? ???? ???????????????? ?????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ???? ???????????????? ?????? Create {}", recipe, exception);
            throw exception;
        }
    }

    private static void addRecipesForType(ServerLevel level, List<Recipe<?>> recipes, ResourceLocation recipeTypeId) {
        try {
            RecipeType recipeType = (RecipeType)BuiltInRegistries.RECIPE_TYPE.get(recipeTypeId);
            if (recipeType == null) {
                RatsMod.LOGGER.error("??? ???????? {} ?? ??????, ??????? Create ??????????", recipeTypeId);
                return;
            }
            recipes.addAll(level.getRecipeManager().getAllRecipesFor(recipeType));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ??????? Create ???? {}", recipeTypeId, exception);
            throw exception;
        }
    }

    private static void applyBeltOutputs(ServerLevel level, Object inventory, Object transported, ItemStack beltStack, List<ItemStack> outputs) {
        try {
            if (outputs.isEmpty()) {
                throw new IllegalArgumentException("?????????? ??????? ?????, ????????? ? ????? Create ??????");
            }
            Object transportedResult = CreateCompat.createTransportedResult(transported, beltStack, outputs);
            float beltPosition = CreateCompat.getTransportedBeltPosition(transported);
            Method method = CreateCompat.findMethod(inventory.getClass(), "applyToEachWithin", Float.TYPE, Float.TYPE, Function.class);
            method.invoke(inventory, Float.valueOf(beltPosition), Float.valueOf(0.01f), (Function<Object,Object>)(candidate -> candidate == transported ? transportedResult : null));
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ???????? ????????? ??????? ? ????????? ????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ??????? ?? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static Object createTransportedResult(Object transported, ItemStack beltStack, List<ItemStack> outputs) {
        try {
            ArrayList<Object> transportedOutputs = new ArrayList<Object>();
            for (ItemStack output : outputs) {
                if (output.isEmpty()) continue;
                transportedOutputs.add(CreateCompat.copyTransportedWithStack(transported, output.copy()));
            }
            Class<?> resultClass = CreateCompat.findClass("com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour$TransportedResult");
            if (beltStack.getCount() <= 1) {
                Method convertTo = CreateCompat.findMethod(resultClass, "convertTo", List.class);
                return convertTo.invoke(null, transportedOutputs);
            }
            ItemStack remaining = beltStack.copy();
            remaining.shrink(1);
            Object heldOutput = CreateCompat.copyTransportedWithStack(transported, remaining);
            Method convertToAndLeaveHeld = CreateCompat.findMethod(resultClass, "convertToAndLeaveHeld", List.class, transported.getClass());
            return convertToAndLeaveHeld.invoke(null, transportedOutputs, heldOutput);
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ??????? ????????? ????????? ????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ??????????? ????????? ????????? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static Object copyTransportedWithStack(Object transported, ItemStack stack) {
        try {
            Object copy = CreateCompat.findMethod(transported.getClass(), "copy", new Class[0]).invoke(transported, new Object[0]);
            CreateCompat.setTransportedStack(copy, stack);
            CreateCompat.setTransportedBoolean(copy, "locked", false);
            CreateCompat.setTransportedAngle(copy, stack);
            return copy;
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ??????????? ??????? ?? ????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ??????????? ??????? ?? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static void spawnResult(ServerLevel level, BlockPos pos, ItemStack stack) {
        try {
            if (!stack.isEmpty()) {
                level.addFreshEntity((Entity)new ItemEntity((Level)level, (double)pos.getX() + 0.5, (double)pos.getY() + 0.9, (double)pos.getZ() + 0.5, stack));
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ????????? Create ? {}", pos, exception);
            throw exception;
        }
    }

    @Nullable
    private static BlockEntity resolveBeltBlockEntity(@Nullable BlockEntity blockEntity) {
        try {
            if (!CreateCompat.isBeltBlockEntity(blockEntity)) {
                return null;
            }
            try {
                Object controller;
                Optional<Method> getController = CreateCompat.findOptionalMethod(blockEntity.getClass(), "getControllerBE", new Class[0]);
                if (getController.isPresent() && (controller = getController.get().invoke(blockEntity, new Object[0])) instanceof BlockEntity) {
                    BlockEntity controllerEntity = (BlockEntity)controller;
                    return controllerEntity;
                }
            }
            catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("?? ??????? ???????? ?????????? ????? Create", exception);
            }
            return blockEntity;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ?????????? block entity ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static boolean isBeltBlockEntity(@Nullable Object blockEntity) {
        try {
            return blockEntity != null && blockEntity.getClass().getName().contains("content.kinetics.belt.BeltBlockEntity");
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? block entity ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static Object getBeltInventory(BlockEntity beltEntity) {
        try {
            return CreateCompat.findMethod(beltEntity.getClass(), "getInventory", new Class[0]).invoke(beltEntity, new Object[0]);
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ???????? ????????? ????? Create ? " + String.valueOf(beltEntity.getBlockPos()), exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ????????? ????? Create ? {}", beltEntity.getBlockPos(), exception);
            throw exception;
        }
    }

    private static List<?> getTransportedItems(Object inventory) {
        try {
            Object items = CreateCompat.findMethod(inventory.getClass(), "getTransportedItems", new Class[0]).invoke(inventory, new Object[0]);
            if (items instanceof List) {
                List list = (List)items;
                return list;
            }
            throw new IllegalStateException("????????? ????? Create ?????? ???????? ?????? ?????????: " + inventory.getClass().getName());
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ???????? ?????? ????????? ????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ?????? ????????? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static int getBeltSegmentIndex(BlockEntity blockEntity) {
        try {
            Field field = CreateCompat.findField(blockEntity.getClass(), "index");
            field.setAccessible(true);
            return field.getInt(blockEntity);
        }
        catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new IllegalStateException("??? ??????? ? ?????? ??????? ????? Create ? " + String.valueOf(blockEntity.getBlockPos()), exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ????? ??????? ????? Create ? {}", blockEntity.getBlockPos(), exception);
            throw exception;
        }
    }

    private static boolean isTransportedOnSegment(Object transported, int targetSegment) {
        try {
            float beltPosition = CreateCompat.getTransportedBeltPosition(transported);
            return beltPosition >= (float)targetSegment && beltPosition < (float)targetSegment + 1.0f;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??????? ???????? ?? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static float getTransportedBeltPosition(Object transported) {
        try {
            Field field = CreateCompat.findField(transported.getClass(), "beltPosition");
            field.setAccessible(true);
            return field.getFloat(transported);
        }
        catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new IllegalStateException("??? ??????? ? ????????? ???????? ?? ????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ????????? ???????? ?? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static ItemStack getTransportedStack(Object transported) {
        try {
            ItemStack itemStack;
            Field field = CreateCompat.findField(transported.getClass(), "stack");
            field.setAccessible(true);
            Object stack = field.get(transported);
            return stack instanceof ItemStack ? (itemStack = (ItemStack)stack) : ItemStack.EMPTY;
        }
        catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new IllegalStateException("??? ??????? ? ???????? ?? ????? Create: " + exception.getMessage(), exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ??????? ?? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static void setTransportedStack(Object transported, ItemStack stack) {
        try {
            Field field = CreateCompat.findField(transported.getClass(), "stack");
            field.setAccessible(true);
            field.set(transported, stack);
        }
        catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new IllegalStateException("??? ??????? ? ?????? ???????? ?? ????? Create: " + exception.getMessage(), exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ??????? ?? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static boolean invokeUseItemOn(BlockState state,Level level,Player player,BlockHitResult hit){return state.use(level,player,InteractionHand.MAIN_HAND,hit).consumesAction();}

    private static boolean invokeUseWithoutItem(BlockState state,Level level,Player player,BlockHitResult hit){return false;}

    private static boolean consumesInteractionResult(Object result) {
        try {
            Boolean value;
            if (result instanceof InteractionResult) {
                InteractionResult interactionResult = (InteractionResult)result;
                return interactionResult.consumesAction();
            }
            if (result == null) {
                return false;
            }
            Optional<Method> method = CreateCompat.findOptionalMethod(result.getClass(), "consumesAction", new Class[0]);
            if (method.isEmpty()) {
                return false;
            }
            Object consumed = method.get().invoke(result, new Object[0]);
            return consumed instanceof Boolean && (value = (Boolean)consumed) != false;
        }
        catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ????????? ????????? ?????????????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ????????? ?????????????? Create {}", result, exception);
            throw exception;
        }
    }

    private static void mountRatOnSeat(TamedRat rat, BlockPos pos) {
        try {
            rat.setOrderedToSit(false);
            AABB area = new AABB(pos).inflate(1.0);
            for (Entity entity : rat.level().getEntities((Entity)rat, area, CreateCompat::isSeatEntity)) {
                if (!rat.startRiding(entity, true)) continue;
                rat.setPos(entity.getX(), entity.getY() + 0.24, entity.getZ());
                RatsMod.LOGGER.debug("????? {} ???? ?? Create-??????? {} ????? ???????? {}", rat.getUUID(), pos, entity.getClass().getName());
                return;
            }
            rat.teleportTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.78, (double)pos.getZ() + 0.5);
            RatsMod.LOGGER.debug("????? {} ???? ?? Create-??????? {}, ?? ???????? ??????? ?? ???????", rat.getUUID(), pos);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ????? {} ?? ??????? Create {}", rat.getUUID(), pos, exception);
            throw exception;
        }
    }

    private static boolean isSeatEntity(Entity entity) {
        try {
            String name = entity.getClass().getName().toLowerCase();
            String simple = entity.getClass().getSimpleName().toLowerCase();
            return name.contains(".seat.") || simple.contains("seat");
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ???????? ??????? Create {}", entity, exception);
            throw exception;
        }
    }

    private static boolean isCreateLoadedBlock(BlockState state) {
        try {
            return RatsMod.CREATE_LOADED && "create".equals(CreateCompat.getBlockId(state).getNamespace());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ?????????????? ????? ? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static String getBlockIdPath(BlockState state) {
        try {
            return CreateCompat.getBlockId(state).getPath();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ???? ????? {}", state, exception);
            throw exception;
        }
    }

    private static ResourceLocation getBlockId(BlockState state) {
        try {
            return BuiltInRegistries.BLOCK.getKey(state.getBlock());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ????????????? ????? {}", state, exception);
            throw exception;
        }
    }

    private static String getBlockIdSafely(@Nullable BlockState state) {
        try {
            return state == null ? "unknown" : CreateCompat.getBlockId(state).toString();
        }
        catch (RuntimeException exception) {
            return "unknown";
        }
    }

    private static void setTransportedBoolean(Object transported, String fieldName, boolean value) {
        try {
            Field field = CreateCompat.findField(transported.getClass(), fieldName);
            field.setAccessible(true);
            field.setBoolean(transported, value);
        }
        catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new IllegalStateException("??? ??????? ? ???? " + fieldName + " ???????? ?? ????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ???? {} ???????? ?? ????? Create", fieldName, exception);
            throw exception;
        }
    }

    private static void setTransportedAngle(Object transported, ItemStack stack) {
        try {
            Field field = CreateCompat.findField(transported.getClass(), "angle");
            field.setAccessible(true);
            field.setInt(transported, CreateCompat.isBeltItemUpright(stack) ? 180 : Math.floorMod(stack.hashCode(), 360));
        }
        catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new IllegalStateException("??? ??????? ? ???? ???????? ?? ????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ???? ???????? ?? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static boolean isBeltItemUpright(ItemStack stack) {
        try {
            Boolean upright;
            Class<?> helperClass = CreateCompat.findClass("com.simibubi.create.content.kinetics.belt.BeltHelper");
            Method method = CreateCompat.findMethod(helperClass, "isItemUpright", ItemStack.class);
            Object value = method.invoke(null, stack);
            return value instanceof Boolean && (upright = (Boolean)value) != false;
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("?? ??????? ????????? ????????? ???????? ?? ????? Create", exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ????????? ???????? ?? ????? Create ??? {}", stack, exception);
            throw exception;
        }
    }

    private static String getItemId(ItemStack stack) {
        try {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ????????????? ???????? {}", stack, exception);
            throw exception;
        }
    }

    private static ItemStack createStack(String itemId) {
        try {
            Item item = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation((String)itemId));
            if (item == Items.AIR) {
                RatsMod.LOGGER.warn("?? ?????? ??????? ?????????? Create {}", itemId);
                return ItemStack.EMPTY;
            }
            return new ItemStack((ItemLike)item);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ??????? ??????? Create {}", itemId, exception);
            throw exception;
        }
    }

    private static boolean isCreateSeat(BlockState state) {
        try {
            return CreateCompat.isCreateLoadedBlock(state) && CREATE_SEAT_BLOCK_IDS.contains(CreateCompat.getBlockIdPath(state));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static boolean isCreateBelt(BlockState state) {
        try {
            return CreateCompat.isCreateLoadedBlock(state) && "belt".equals(CreateCompat.getBlockIdPath(state));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ????? Create", (Throwable)exception);
            throw exception;
        }
    }

    private static Class<?> findClass(String className) throws ClassNotFoundException {
        try {
            Class<?> cached = CLASS_CACHE.get(className);
            if (cached != null) {
                return cached;
            }
            Class<?> type = Class.forName(className);
            CLASS_CACHE.put(className, type);
            return type;
        }
        catch (ClassNotFoundException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0439\u0442\u0438 \u043a\u043b\u0430\u0441\u0441 Create {}", className, exception);
            throw exception;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?> ... parameterTypes) throws NoSuchMethodException {
        try {
            String cacheKey = CreateCompat.getMethodCacheKey(type, name, parameterTypes);
            Method cached = METHOD_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            if (MISSING_METHOD_CACHE.contains(cacheKey)) {
                throw new NoSuchMethodException(cacheKey);
            }
            try {
                Method method = type.getMethod(name, parameterTypes);
                method.setAccessible(true);
                METHOD_CACHE.put(cacheKey, method);
                return method;
            }
            catch (NoSuchMethodException exception) {
                MISSING_METHOD_CACHE.add(cacheKey);
                throw exception;
            }
        }
        catch (NoSuchMethodException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0439\u0442\u0438 \u043c\u0435\u0442\u043e\u0434 {} \u0443 \u043a\u043b\u0430\u0441\u0441\u0430 {}", name, type.getName(), exception);
            throw exception;
        }
    }

    private static Optional<Method> findOptionalMethod(Class<?> type, String name, Class<?> ... parameterTypes) {
        try {
            return Optional.of(CreateCompat.findMethod(type, name, parameterTypes));
        }
        catch (NoSuchMethodException exception) {
            return Optional.empty();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043c\u0435\u0442\u043e\u0434 {} \u0443 \u043a\u043b\u0430\u0441\u0441\u0430 {}", name, type.getName(), exception);
            throw exception;
        }
    }

    private static String getMethodCacheKey(Class<?> type, String name, Class<?> ... parameterTypes) {
        try {
            StringBuilder builder = new StringBuilder(type.getName()).append('#').append(name).append('(');
            for (Class<?> parameterType : parameterTypes) {
                builder.append(parameterType.getName()).append(',');
            }
            return builder.append(')').toString();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0431\u0440\u0430\u0442\u044c \u043a\u043b\u044e\u0447 \u043c\u0435\u0442\u043e\u0434\u0430 {} \u0443 \u043a\u043b\u0430\u0441\u0441\u0430 {}", name, type.getName(), exception);
            throw exception;
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        try {
            String cacheKey = type.getName() + "#" + name;
            Field cached = FIELD_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    FIELD_CACHE.put(cacheKey, field);
                    return field;
                }
                catch (NoSuchFieldException ignored) {
                    continue;
                }
            }
            throw new NoSuchFieldException(name);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????? ???? {} ? {}", name, type.getName(), exception);
            throw exception;
        }
    }

    private static void markBeltInventoryDirty(BlockEntity blockEntity) {
        try {
            blockEntity.setChanged();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ????????? ????? Create ?????????? {}", blockEntity.getBlockPos(), exception);
            throw exception;
        }
    }

    private static void markDirty(BlockEntity blockEntity) {
        try {
            blockEntity.setChanged();
            if (blockEntity.getLevel() != null) {
                BlockState state = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
                blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, 3);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? block entity Create ?????????? {}", blockEntity.getBlockPos(), exception);
            throw exception;
        }
    }

    private record ApplicationResult(List<ItemStack> results, boolean keepHeldItem, String recipeId) {
        private static ApplicationResult empty() {
            try {
                return EMPTY_APPLICATION_RESULT;
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("?? ??????? ??????? ?????? ????????? ?????????? Create", (Throwable)exception);
                throw exception;
            }
        }
    }

public static void invalidateRecipeCaches(){cachedRecipeManager=null;}
}
