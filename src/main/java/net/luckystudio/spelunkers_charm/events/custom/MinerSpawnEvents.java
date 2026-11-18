package net.luckystudio.spelunkers_charm.events.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Turns some zombies and skeletons that spawn deep underground into "miners":
 * they wear a Mining Helmet or Candle Helmet and carry a pickaxe, and can drop both.
 * <p>
 * Which mobs can become miners is set by the {@code spelunkers_charm:can_spawn_as_miner} entity type tag.
 * <p>
 * The roll happens in {@link FinalizeSpawnEvent}, but the gear is given in {@link EntityJoinLevelEvent}.
 * FinalizeSpawnEvent fires before vanilla's own equipment roll, which would overwrite our gear,
 * while the join event fires right after it.
 */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MinerSpawnEvents {

    public static final TagKey<EntityType<?>> CAN_SPAWN_AS_MINER =
            TagKey.create(Registries.ENTITY_TYPE, SpelunkersCharm.id("can_spawn_as_miner"));

    private static final String PENDING_MINER_TAG = SpelunkersCharm.MOD_ID + ":pending_miner";

    /** Mobs must spawn below this Y level to become miners. */
    private static final int MAX_SPAWN_Y = 40;
    /** Chance for an eligible mob to become a miner. */
    private static final float MINER_CHANCE = 0.15F;
    /** Chance the helmet is a Candle Helmet instead of a Mining Helmet. */
    private static final float CANDLE_HELMET_CHANCE = 0.4F;
    /** Chance the pickaxe is iron instead of stone. */
    private static final float IRON_PICKAXE_CHANCE = 0.25F;
    /** Chance a skeleton miner trades its bow for a pickaxe (and fights in melee). Zombies always get one. */
    private static final float SKELETON_PICKAXE_CHANCE = 0.5F;
    /** Drop chances. Vanilla armor is 0.085. Dropped gear comes out damaged, like any non-guaranteed mob drop. */
    private static final float HELMET_DROP_CHANCE = 0.12F;
    private static final float PICKAXE_DROP_CHANCE = 0.085F;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.SPAWNER) return;
        if (!mob.getType().is(CAN_SPAWN_AS_MINER)) return;

        Level level = event.getLevel().getLevel();
        if (level.dimension() != Level.OVERWORLD) return;

        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (pos.getY() >= MAX_SPAWN_Y || level.canSeeSky(pos)) return;
        if (mob.getRandom().nextFloat() >= MINER_CHANCE) return;

        mob.getPersistentData().putBoolean(PENDING_MINER_TAG, true);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!mob.getPersistentData().getBoolean(PENDING_MINER_TAG)) return;

        mob.getPersistentData().remove(PENDING_MINER_TAG);
        equipAsMiner(mob, mob.getRandom());
    }

    public static void equipAsMiner(Mob mob, RandomSource random) {
        ItemStack helmet = random.nextFloat() < CANDLE_HELMET_CHANCE
                ? new ItemStack(ModItems.CANDLE_HELMET.get())
                : new ItemStack(ModItems.MINING_HELMET.get());
        mob.setItemSlot(EquipmentSlot.HEAD, helmet);
        mob.setDropChance(EquipmentSlot.HEAD, HELMET_DROP_CHANCE);

        boolean givePickaxe = !(mob instanceof AbstractSkeleton) || random.nextFloat() < SKELETON_PICKAXE_CHANCE;
        if (givePickaxe) {
            ItemStack pickaxe = new ItemStack(random.nextFloat() < IRON_PICKAXE_CHANCE ? Items.IRON_PICKAXE : Items.STONE_PICKAXE);
            // AbstractSkeleton#setItemSlot re-picks its attack goal, so a skeleton holding a pickaxe fights in melee.
            mob.setItemSlot(EquipmentSlot.MAINHAND, pickaxe);
            mob.setDropChance(EquipmentSlot.MAINHAND, PICKAXE_DROP_CHANCE);
        }
    }
}
