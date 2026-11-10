package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A minecart with a jukebox in it. Put a music disc in and the music follows the cart.
 * Made by shift-right-clicking a jukebox onto a minecart.
 */
public class JukeboxMinecart extends AbstractMinecart {

    // The record must be defined before PLAY_ID so the client has it when the play id changes.
    private static final EntityDataAccessor<ItemStack> DATA_RECORD = SynchedEntityData.defineId(JukeboxMinecart.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING = SynchedEntityData.defineId(JukeboxMinecart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PLAY_ID = SynchedEntityData.defineId(JukeboxMinecart.class, EntityDataSerializers.INT);

    private int ticksPlaying;

    public JukeboxMinecart(EntityType<? extends JukeboxMinecart> entityType, Level level) {
        super(entityType, level);
    }

    public JukeboxMinecart(Level level, double x, double y, double z) {
        super(ModEntityType.JUKEBOX_MINECART.get(), level, x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RECORD, ItemStack.EMPTY);
        builder.define(DATA_PLAYING, false);
        builder.define(DATA_PLAY_ID, 0);
    }

    public ItemStack getRecord() {
        return this.entityData.get(DATA_RECORD);
    }

    public boolean hasRecord() {
        return !getRecord().isEmpty();
    }

    public boolean isPlaying() {
        return this.entityData.get(DATA_PLAYING);
    }

    /** Changes every time a disc is put in, so the client knows to start a new song. */
    public int getPlayId() {
        return this.entityData.get(DATA_PLAY_ID);
    }

    public Optional<Holder<JukeboxSong>> getSong() {
        ItemStack record = getRecord();
        if (record.isEmpty()) return Optional.empty();
        return JukeboxSong.fromStack(this.registryAccess(), record);
    }

    @Override
    public Type getMinecartType() {
        // Not RIDEABLE, so it doesn't scoop up mobs.
        return Type.CHEST;
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hasRecord()) {
            if (!this.level().isClientSide()) {
                ejectRecord(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (held.has(DataComponents.JUKEBOX_PLAYABLE)) {
            if (!this.level().isClientSide()) {
                insertRecord(held.consumeAndReturn(1, player));
                player.awardStat(Stats.PLAY_RECORD);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    private void insertRecord(ItemStack record) {
        this.entityData.set(DATA_RECORD, record.copyWithCount(1));
        this.ticksPlaying = 0;
        this.entityData.set(DATA_PLAYING, true);
        this.entityData.set(DATA_PLAY_ID, getPlayId() + 1);
    }

    private void ejectRecord(Player player) {
        ItemStack record = getRecord().copy();
        this.entityData.set(DATA_RECORD, ItemStack.EMPTY);
        this.entityData.set(DATA_PLAYING, false);
        this.ticksPlaying = 0;
        if (!player.addItem(record)) {
            this.spawnAtLocation(record, 0.5F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (isPlaying()) {
                this.ticksPlaying++;
                int length = getSong().map(song -> song.value().lengthInTicks()).orElse(0);
                if (this.ticksPlaying > length) {
                    // The song finished. The disc stays in until someone takes it out, like a jukebox.
                    this.entityData.set(DATA_PLAYING, false);
                }
            }
        } else if (isPlaying() && this.tickCount % 20 == 0) {
            this.level().addParticle(ParticleTypes.NOTE, this.getX(), this.getY() + 1.2, this.getZ(),
                    this.random.nextInt(25) / 24.0, 0.0, 0.0);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level().isClientSide() && (DATA_PLAY_ID.equals(key) || DATA_PLAYING.equals(key)) && isPlaying() && hasRecord()) {
            JukeboxMinecartSounds.play(this);
        }
    }

    /** Detector rails give the same comparator signal a jukebox would. */
    @Override
    public int getComparatorLevel() {
        if (!isPlaying()) return 0;
        return getSong().map(song -> song.value().comparatorOutput()).orElse(0);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, hasRecord());
    }

    @Override
    protected @NotNull Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.JUKEBOX);
    }

    @Override
    public void destroy(Item dropItem) {
        super.destroy(dropItem);
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(Items.JUKEBOX);
            if (hasRecord()) {
                this.spawnAtLocation(getRecord().copy());
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRecord()) {
            tag.put("RecordItem", getRecord().save(this.registryAccess()));
        }
        tag.putBoolean("Playing", isPlaying());
        tag.putInt("TicksPlaying", this.ticksPlaying);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RecordItem")) {
            this.entityData.set(DATA_RECORD, ItemStack.parse(this.registryAccess(), tag.getCompound("RecordItem")).orElse(ItemStack.EMPTY));
        }
        this.entityData.set(DATA_PLAYING, tag.getBoolean("Playing") && hasRecord());
        this.ticksPlaying = tag.getInt("TicksPlaying");
    }
}
