package net.luckystudio.spelunkers_charm.entity.custom.cave_borer;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.datagen.blockTags.ModBlockTags;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The Cave Borer: a segmented worm that swims through natural rock and ambushes players.
 *
 * <p>The loop, run on the server in {@link #customServerAiStep()}:
 * <ol>
 *     <li>{@link State#BURROWED}: hidden and untouchable. Tunnels to a block face near its target.</li>
 *     <li>{@link State#WARNING}: sits behind that face, cracking it and spraying its particles.</li>
 *     <li>{@link State#LUNGE}: bursts out on an arc through where the player stood, biting what it touches.</li>
 *     <li>{@link State#DIVE}: its head entered a diggable block, the body follows it in, then back to BURROWED.</li>
 *     <li>{@link State#STUNNED}: it hit a block it can't dig. Normal physics, exposed, then digs down or crawls off.</li>
 * </ol>
 *
 * <p>The body is a chain of segments that follow the head (segment 0). Segments 1+ are {@link CaveBorerPart}
 * hitboxes. The chain is simulated on both sides from the head's position, so it needs no extra syncing.
 */
public class CaveBorer extends Monster {

    public enum State {
        BURROWED, WARNING, LUNGE, DIVE, STUNNED;

        private static final State[] VALUES = values();

        public static State byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : BURROWED;
        }
    }

    /**
     * Sizes run from 0 (small) to {@link #MAX_SIZE}. Every per-size stat comes from the formulas below.
     * Sizes 0 to 2 grow quickly (small, medium, large); from 3 up they grow more slowly so the
     * biggest borers stay giant rather than absurd. Size 8: 18 segments, 3.5x scale, 36 health, 11 damage.
     */
    public static final int MAX_SIZE = 8;

    /** Model scale: 1, 1.5, 2, then +0.25 per size. */
    public static float scaleFor(int size) {
        return size <= 2 ? 1.0F + 0.5F * size : 2.0F + 0.25F * (size - 2);
    }

    /** Segments including head and tail: 6, 9, 12, then +1 per size. Long bodies are easier to hit. */
    public static int segmentsFor(int size) {
        return size <= 2 ? 6 + 3 * size : 12 + (size - 2);
    }

    /** Health: 4 per size step (2 hearts small, 18 hearts at size 8). */
    public static double healthFor(int size) {
        return 4.0 + 4.0 * size;
    }

    /** Bite damage: 2, 3, 5, then +1 per size. */
    public static double damageFor(int size) {
        return size <= 2 ? new double[]{2.0, 3.0, 5.0}[size] : 5.0 + (size - 2);
    }

    public static int xpFor(int size) {
        return 1 + 2 * size;
    }

    /** Lunge speed in blocks per tick. Bigger borers are a bit slower. */
    public static double lungeSpeedFor(int size) {
        return Math.max(0.45, 0.9 - 0.1 * size);
    }

    /** Multiplies the config's ambush distances, so bigger borers charge from farther away. */
    public static double rangeScaleFor(int size) {
        return size <= 2 ? 1.0 + 0.3 * size : 1.6 + 0.15 * (size - 2);
    }
    // How far past its max ambush distance the target may move before a charge is called off
    private static final double CHARGE_RANGE_GRACE = 2.0;
    // Small borers give up and leave after this many charges, so they don't drag on
    private static final int SMALL_MAX_CHARGES = 3;

    // Hitboxes are bigger than the model so the fast, thin worm is easier to hit. Bites still use the model size.
    private static final float HITBOX_SCALE = 1.5F;
    private static final int MAX_PARTS = segmentsFor(MAX_SIZE) - 1; // the head is the entity itself
    private static final double BURROW_SPEED = 0.5;
    private static final double LUNGE_GRAVITY = 0.04;
    private static final int MAX_LUNGE_TICKS = 40;
    private static final int STUN_TICKS = 60;
    private static final int TARGET_RANGE = 24;

    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData.defineId(CaveBorer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(CaveBorer.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Direction> DATA_FACE = SynchedEntityData.defineId(CaveBorer.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(CaveBorer.class, EntityDataSerializers.INT);

    private final CaveBorerPart[] parts;

    // Body chain, index 0 is the head. Centers, not feet positions.
    private final Vec3[] segmentPos = new Vec3[MAX_PARTS + 1];
    private final Vec3[] segmentPosO = new Vec3[MAX_PARTS + 1];

    // Server-side AI state
    private int stateTicks;
    private int restTicks;
    private int stunTicks = STUN_TICKS;
    private int failedSearches;
    @Nullable private BlockPos emergeBlock;
    @Nullable private Vec3 travelTarget;
    @Nullable private Vec3 lastAirCenter;
    @Nullable private Vec3 diveEntry;
    @Nullable private BlockState diveBlock;
    private double diveDistance;
    private double lungeGravity = LUNGE_GRAVITY;
    private int buriedSegments;
    private int charges; // bit i set once segment i has gone into the ground this dive
    private final IntSet bitten = new IntOpenHashSet();

    // Client-side animation
    private float jawOpen;
    private float jawOpenO;

    public CaveBorer(EntityType<? extends CaveBorer> type, Level level) {
        super(type, level);
        this.parts = new CaveBorerPart[MAX_PARTS];
        for (int i = 0; i < MAX_PARTS; i++) {
            this.parts[i] = new CaveBorerPart(this, i + 1);
        }
        // Reserve ids for the parts right after ours, like the Ender Dragon
        this.setId(ENTITY_COUNTER.getAndAdd(this.parts.length + 1) + 1);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.xpReward = xpFor(0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, healthFor(0))
                .add(Attributes.ATTACK_DAMAGE, damageFor(0))
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, TARGET_RANGE);
    }

    public static boolean checkCaveBorerSpawnRules(EntityType<CaveBorer> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!SpelunkersCharmConfig.CAVE_BORER_SPAWNING.get()) return false;
        if (pos.getY() > SpelunkersCharmConfig.CAVE_BORER_MAX_SPAWN_Y.get()) return false;
        if (pos.getY() < SpelunkersCharmConfig.CAVE_BORER_MIN_SPAWN_Y.get()) return false;
        if (level.getBiome(pos).is(Tags.Biomes.IS_MUSHROOM)) return false;
        if (!level.getBlockState(pos.below()).is(ModBlockTags.CAVE_BORER_DIGGABLE)) return false;
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    // ===== Data =====

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SIZE, 0);
        builder.define(DATA_STATE, (byte) State.BURROWED.ordinal());
        builder.define(DATA_FACE, Direction.UP);
        builder.define(DATA_VARIANT, CaveBorerVariant.STONE.ordinal());
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        if (DATA_SIZE.equals(key)) {
            this.refreshDimensions();
            if (this.parts != null) for (CaveBorerPart part : this.parts) {
                part.refreshDimensions();
            }
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Size", this.getSize());
        compound.putInt("Charges", this.charges);
        compound.putString("Variant", this.getVariant().getSerializedName());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // No saved health means it came from /summon with NBT, so start at full health
        this.setSize(compound.getInt("Size"), !compound.contains("Health"));
        this.charges = compound.getInt("Charges");
        this.setVariant(CaveBorerVariant.byName(compound.getString("Variant")));
        this.setState(State.BURROWED);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.setSize(rollSpawnSize(level.getRandom()), true);
        this.setVariant(CaveBorerVariant.forBiome(level.getBiome(this.blockPosition())));
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            // Natural spawns start tucked into the floor they spawned on
            this.setCenter(Vec3.atCenterOf(this.blockPosition().below()));
            this.setState(State.BURROWED);
        } else {
            // Spawn eggs, commands and spawners: show it briefly, then it digs in
            this.popOut(20);
        }
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    /** Random natural size up to the config's maxNaturalSize: 50% small, 35% medium, 15% large, each bigger size half as likely again. */
    private static int rollSpawnSize(RandomSource random) {
        int max = Mth.clamp(SpelunkersCharmConfig.CAVE_BORER_MAX_NATURAL_SIZE.get(), 0, MAX_SIZE);
        double[] weights = new double[max + 1];
        double total = 0.0;
        for (int size = 0; size <= max; size++) {
            weights[size] = size == 0 ? 50.0 : size == 1 ? 35.0 : 15.0 * Math.pow(0.5, size - 2);
            total += weights[size];
        }
        double roll = random.nextDouble() * total;
        for (int size = 0; size <= max; size++) {
            roll -= weights[size];
            if (roll < 0.0) return size;
        }
        return max;
    }

    public CaveBorerVariant getVariant() {
        return CaveBorerVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(CaveBorerVariant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSize(int size, boolean resetHealth) {
        int clamped = Mth.clamp(size, 0, MAX_SIZE);
        this.entityData.set(DATA_SIZE, clamped);
        this.reapplyPosition();
        this.refreshDimensions();
        Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthFor(clamped));
        Objects.requireNonNull(this.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(damageFor(clamped));
        if (resetHealth) {
            this.setHealth(this.getMaxHealth());
        }
        this.xpReward = xpFor(clamped);
    }

    public float getSizeScale() {
        return scaleFor(this.getSize());
    }

    public int getSegmentCount() {
        return segmentsFor(this.getSize());
    }

    public float getSegmentSpacing() {
        return 0.4F * this.getSizeScale();
    }

    /** Model width of a segment. The tail is a bit thinner. */
    public float getSegmentWidth(int segment) {
        float width = 0.5F * this.getSizeScale();
        return segment == this.getSegmentCount() - 1 ? width * 0.75F : width;
    }

    /** Hitbox width of a segment, bigger than the model to make it easier to hit. */
    public float getSegmentHitboxWidth(int segment) {
        return this.getSegmentWidth(segment) * HITBOX_SCALE;
    }

    public State getState() {
        return State.byId(this.entityData.get(DATA_STATE));
    }

    private void setState(State state) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
        this.stateTicks = 0;
    }

    public Direction getFace() {
        return this.entityData.get(DATA_FACE);
    }

    /** Hidden inside the rock: not rendered, can't be hit. */
    public boolean isBurrowed() {
        State state = this.getState();
        return state == State.BURROWED || state == State.WARNING;
    }

    public boolean isSegmentActive(int segment) {
        return segment < this.getSegmentCount() && !this.isBurrowed() && !this.isRemoved();
    }

    @Override
    public @NotNull EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
        return this.getType().getDimensions().scale(this.getSizeScale() * HITBOX_SCALE);
    }

    public Vec3 getCenter() {
        return this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
    }

    private void setCenter(Vec3 center) {
        this.setPos(center.x, center.y - this.getBbHeight() * 0.5, center.z);
    }

    // ===== Multipart =====

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (this.parts != null) {
            for (int i = 0; i < this.parts.length; i++) {
                this.parts[i].setId(id + i + 1);
            }
        }
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        AABB box = this.getBoundingBox();
        for (CaveBorerPart part : this.parts) {
            if (part.segment < this.getSegmentCount()) {
                box = box.minmax(part.getBoundingBox());
            }
        }
        return box;
    }

    // ===== Physics and damage =====

    @Override
    public boolean isPickable() {
        return !this.isBurrowed() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected @NotNull MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.FALL) || source.is(DamageTypes.CRAMMING)) {
            return true;
        }
        if (this.isBurrowed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (this.noPhysics) {
            // Scripted movement: the AI sets the velocity directly, no friction or gravity
            if (this.isControlledByLocalInstance()) {
                this.move(MoverType.SELF, this.getDeltaMovement());
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void tick() {
        // Only a stunned or dead borer obeys physics. Everything else passes through blocks.
        boolean physics = this.getState() == State.STUNNED || this.isDeadOrDying();
        this.noPhysics = !physics;
        this.setNoGravity(!physics);
        super.tick();
        this.updateSegments();
        if (this.level().isClientSide) {
            this.jawOpenO = this.jawOpen;
            float target = switch (this.getState()) {
                case LUNGE -> 1.0F;
                case DIVE -> 0.3F;
                case STUNNED -> 0.5F + 0.5F * Mth.sin(this.tickCount * 0.8F);
                default -> 0.0F;
            };
            this.jawOpen += (target - this.jawOpen) * 0.5F;
        }
    }

    public float getJawOpen(float partialTick) {
        return Mth.lerp(partialTick, this.jawOpenO, this.jawOpen);
    }

    // ===== Body chain =====

    private void updateSegments() {
        int count = this.getSegmentCount();
        float spacing = this.getSegmentSpacing();
        Vec3 head = this.getCenter();
        boolean seeded = this.segmentPos[0] != null;

        for (int i = 0; i <= MAX_PARTS; i++) {
            this.segmentPosO[i] = this.segmentPos[i];
        }

        State state = this.getState();
        if (!seeded || state == State.BURROWED || state == State.WARNING) {
            // Hidden: keep the body in a straight line running back into the wall,
            // so when it lunges the body pours out of the hole
            Vec3 back = Vec3.atLowerCornerOf(this.getFace().getNormal()).scale(-1.0);
            for (int i = 0; i <= MAX_PARTS; i++) {
                this.segmentPos[i] = head.add(back.scale(spacing * Math.min(i, count - 1)));
            }
        } else {
            this.segmentPos[0] = head;
            boolean slump = state == State.STUNNED || this.isDeadOrDying();
            for (int i = 1; i < count; i++) {
                Vec3 ahead = this.segmentPos[i - 1];
                Vec3 current = this.segmentPos[i];
                if (slump) {
                    // Let the body sag onto the floor instead of hanging in the air
                    Vec3 lowered = current.add(0.0, -0.15, 0.0);
                    BlockPos below = BlockPos.containing(lowered.x, lowered.y - this.getSegmentWidth(i) * 0.5, lowered.z);
                    if (this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty()) {
                        current = lowered;
                    }
                }
                Vec3 offset = current.subtract(ahead);
                double length = offset.length();
                if (length < 1.0E-4) {
                    offset = Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(-1.0);
                    length = 1.0;
                }
                this.segmentPos[i] = ahead.add(offset.scale(spacing / length));
            }
            for (int i = count; i <= MAX_PARTS; i++) {
                this.segmentPos[i] = this.segmentPos[count - 1];
            }
        }

        if (!seeded) {
            System.arraycopy(this.segmentPos, 0, this.segmentPosO, 0, this.segmentPos.length);
        }

        for (CaveBorerPart part : this.parts) {
            Vec3 pos = this.segmentPos[part.segment];
            part.setPos(pos.x, pos.y - part.getBbHeight() * 0.5, pos.z);
        }
    }

    /** Interpolated center of a body segment, for rendering. Segment 0 is the head. */
    public Vec3 getSegmentPosition(int segment, float partialTick) {
        Vec3 current = this.segmentPos[segment];
        Vec3 old = this.segmentPosO[segment];
        if (current == null) return this.getCenter();
        if (old == null) return current;
        return old.lerp(current, partialTick);
    }

    // ===== AI =====

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.stateTicks++;
        LivingEntity target = this.updateTarget();

        switch (this.getState()) {
            case BURROWED -> this.tickBurrowed(target);
            case WARNING -> this.tickWarning(target);
            case LUNGE -> this.tickLunge();
            case DIVE -> this.tickDive();
            case STUNNED -> this.tickStunned();
        }
    }

    @Nullable
    private LivingEntity updateTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || this.distanceToSqr(target) > this.getLoseTargetRange() * this.getLoseTargetRange()
                || (target instanceof Player player && (player.isCreative() || player.isSpectator())))) {
            this.setTarget(null);
            target = null;
        }
        if (target == null && this.tickCount % 10 == 0) {
            // No line of sight needed: it hunts by feeling footsteps through the rock
            TargetingConditions conditions = TargetingConditions.forCombat().range(this.getTargetRange()).ignoreLineOfSight();
            Player player = this.level().getNearestPlayer(conditions, this);
            if (player != null) {
                this.setTarget(player);
                target = player;
            }
        }
        return target;
    }

    private void tickBurrowed(@Nullable LivingEntity target) {
        if (this.restTicks > 0) this.restTicks--;

        if (this.travelTarget != null) {
            Vec3 center = this.getCenter();
            Vec3 toTarget = this.travelTarget.subtract(center);
            double distance = toTarget.length();
            if (distance > this.getLoseTargetRange() || distance <= BURROW_SPEED) {
                this.setDeltaMovement(Vec3.ZERO);
                this.setCenter(this.travelTarget);
                this.travelTarget = null;
                if (this.emergeBlock != null) {
                    if (this.level().getBlockState(this.emergeBlock).is(ModBlockTags.CAVE_BORER_DIGGABLE) && this.isInChargeRange(target, this.emergeBlock)) {
                        this.setState(State.WARNING);
                    } else {
                        this.emergeBlock = null;
                    }
                }
            } else {
                Vec3 velocity = toTarget.scale(BURROW_SPEED / distance);
                this.setDeltaMovement(velocity);
                this.faceDirection(velocity);
                if (this.tickCount % 10 == 0) {
                    this.playSound(ModSoundEvents.CAVE_BORER_BURROW.get(), 0.35F, 0.6F + this.random.nextFloat() * 0.3F);
                }
            }
            return;
        }

        this.setDeltaMovement(Vec3.ZERO);
        if (target != null) {
            if (this.restTicks <= 0 && this.stateTicks % 10 == 0) {
                if (this.findEmergeSpot(target)) {
                    this.failedSearches = 0;
                    this.travelTarget = Vec3.atCenterOf(Objects.requireNonNull(this.emergeBlock));
                } else if (++this.failedSearches >= 20) {
                    // Nowhere to come out near this player for 10 seconds, give up on them
                    this.failedSearches = 0;
                    this.setTarget(null);
                }
            }
        } else if (this.random.nextInt(120) == 0) {
            // Idle: drift slowly through the rock
            BlockPos wander = this.blockPosition().offset(this.random.nextInt(13) - 6, this.random.nextInt(7) - 3, this.random.nextInt(13) - 6);
            if (this.level().getBlockState(wander).is(ModBlockTags.CAVE_BORER_DIGGABLE)) {
                this.travelTarget = Vec3.atCenterOf(wander);
            }
        }
    }

    /**
     * Casts rays from the target's chest in random directions. The first block each ray hits is a face the
     * player can see, so floors, walls and ceilings all come up, and the lunge never has to go through a wall.
     */
    private double getAmbushMinDistance() {
        return SpelunkersCharmConfig.CAVE_BORER_AMBUSH_MIN_DISTANCE.get() * rangeScaleFor(this.getSize());
    }

    private double getAmbushMaxDistance() {
        double max = SpelunkersCharmConfig.CAVE_BORER_AMBUSH_MAX_DISTANCE.get() * rangeScaleFor(this.getSize());
        return Math.max(this.getAmbushMinDistance() + 1.0, max);
    }

    /** How far away it notices players. At least 24 blocks, more for borers that charge from far away. */
    private double getTargetRange() {
        return Math.max(TARGET_RANGE, this.getAmbushMaxDistance() + 10.0);
    }

    private double getLoseTargetRange() {
        return this.getTargetRange() + 8.0;
    }

    /** Whether the target is still close enough to the emerge block for a charge from it. */
    private boolean isInChargeRange(@Nullable LivingEntity target, BlockPos emerge) {
        if (target == null) return false;
        double range = this.getAmbushMaxDistance() + CHARGE_RANGE_GRACE;
        Vec3 chest = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        return chest.distanceToSqr(Vec3.atCenterOf(emerge)) <= range * range;
    }

    private boolean findEmergeSpot(LivingEntity target) {
        double minDistance = this.getAmbushMinDistance();
        double maxDistance = this.getAmbushMaxDistance();
        Vec3 origin = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        for (int attempt = 0; attempt < 24; attempt++) {
            double y = this.random.nextDouble() * 2.0 - 1.0;
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(1.0 - y * y);
            Vec3 direction = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3 end = origin.add(direction.scale(maxDistance));
            BlockHitResult hit = this.level().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            if (hit.getLocation().distanceTo(origin) < minDistance) continue;
            BlockPos pos = hit.getBlockPos();
            if (!this.level().getBlockState(pos).is(ModBlockTags.CAVE_BORER_DIGGABLE)) continue;
            this.emergeBlock = pos.immutable();
            this.entityData.set(DATA_FACE, hit.getDirection());
            return true;
        }
        return false;
    }

    private Vec3 faceNormal() {
        return Vec3.atLowerCornerOf(this.getFace().getNormal());
    }

    private void tickWarning(@Nullable LivingEntity target) {
        this.setDeltaMovement(Vec3.ZERO);
        BlockPos pos = this.emergeBlock;
        if (pos == null || !this.level().getBlockState(pos).is(ModBlockTags.CAVE_BORER_DIGGABLE)) {
            // Someone mined or replaced the block
            this.clearCrack();
            this.emergeBlock = null;
            this.setState(State.BURROWED);
            return;
        }
        if (!this.isInChargeRange(target, pos)) {
            // The player got away (or is gone): call off the charge and pick a new spot
            this.clearCrack();
            this.emergeBlock = null;
            this.restTicks = 10;
            this.setState(State.BURROWED);
            return;
        }

        BlockState state = this.level().getBlockState(pos);
        int warningTicks = Math.max(5, (int) (SpelunkersCharmConfig.CAVE_BORER_WARNING_SECONDS.get() * 20.0));
        Vec3 normal = this.faceNormal();
        Vec3 facePoint = Vec3.atCenterOf(pos).add(normal.scale(0.52));

        if (this.stateTicks == 1) {
            this.playSound(ModSoundEvents.CAVE_BORER_WARN.get(), 1.0F, 0.8F + this.random.nextFloat() * 0.2F);
        }
        if (this.stateTicks % 2 == 0 && this.level() instanceof ServerLevel serverLevel) {
            int count = 2 + this.stateTicks * 6 / warningTicks;
            double spread = 0.3 * this.getSizeScale();
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), facePoint.x, facePoint.y, facePoint.z, count,
                    normal.x != 0 ? 0.05 : spread, normal.y != 0 ? 0.05 : spread, normal.z != 0 ? 0.05 : spread, 0.15);
        }
        if (this.stateTicks % 4 == 0) {
            SoundEvent hitSound = state.getSoundType(this.level(), pos, this).getHitSound();
            this.level().playSound(null, pos, hitSound, SoundSource.HOSTILE, 0.9F, 0.5F + this.random.nextFloat() * 0.3F);
        }
        this.level().destroyBlockProgress(this.getId(), pos, Math.min(9, this.stateTicks * 10 / warningTicks));

        if (this.stateTicks >= warningTicks) {
            this.startLunge(target, pos, state);
        }
    }

    private void startLunge(@Nullable LivingEntity target, BlockPos pos, BlockState state) {
        this.clearCrack();
        Vec3 normal = this.faceNormal();
        Vec3 start = Vec3.atCenterOf(pos).add(normal.scale(0.6));
        // Aim is locked here, so stepping aside dodges it
        Vec3 aim = target != null ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0) : start.add(normal.scale(6.0));
        Vec3 offset = aim.subtract(start);
        double speed = lungeSpeedFor(this.getSize());
        double ticks = Math.max(4.0, offset.length() / speed);
        // Gravity scales with speed squared, so slow (big) borers fly the same arc shape as fast ones
        // instead of a tall lob that starts out going up
        this.lungeGravity = LUNGE_GRAVITY * (speed / lungeSpeedFor(0)) * (speed / lungeSpeedFor(0));
        // Solve for a velocity that passes through the aim point under our own gravity
        Vec3 velocity = offset.scale(1.0 / ticks).add(0.0, 0.5 * this.lungeGravity * ticks, 0.0);
        // Always leave the surface: never start off heading back into the wall or ceiling we came out of
        double away = velocity.dot(normal);
        double minAway = 0.4 * speed;
        if (away < minAway) {
            velocity = velocity.add(normal.scale(minAway - away));
        }

        this.setCenter(start);
        this.setDeltaMovement(velocity);
        this.faceDirection(velocity);
        this.lastAirCenter = start;
        this.bitten.clear();
        this.level().levelEvent(2001, pos, Block.getId(state)); // block break particles and sound
        this.playSound(ModSoundEvents.CAVE_BORER_EMERGE.get(), 1.0F, 1.0F);
        this.emergeBlock = null;
        this.charges++;
        this.setState(State.LUNGE);
    }

    private void tickLunge() {
        Vec3 velocity = this.getDeltaMovement().add(0.0, -this.lungeGravity, 0.0);
        this.setDeltaMovement(velocity);
        this.faceDirection(velocity);
        this.biteTouching();

        if (this.stateTicks > 3) {
            Vec3 center = this.getCenter();
            BlockPos pos = BlockPos.containing(center);
            BlockState state = this.level().getBlockState(pos);
            if (!state.getCollisionShape(this.level(), pos).isEmpty()) {
                if (state.is(ModBlockTags.CAVE_BORER_DIGGABLE)) {
                    this.startDive(pos, state, velocity);
                } else {
                    this.startStun();
                }
                return;
            }
            this.lastAirCenter = center;
        }
        if (this.stateTicks > MAX_LUNGE_TICKS) {
            this.startStun();
        }
    }

    private void startDive(BlockPos pos, BlockState state, Vec3 velocity) {
        this.diveEntry = this.getCenter();
        this.diveBlock = state;
        this.diveDistance = 0.0;
        this.buriedSegments = 1; // the head's entry already played the block break effect
        this.level().levelEvent(2001, pos, Block.getId(state));
        double speed = Math.max(0.35, velocity.length());
        this.setDeltaMovement(velocity.lengthSqr() > 1.0E-6 ? velocity.normalize().scale(speed) : new Vec3(0.0, -speed, 0.0));
        this.getNavigation().stop();
        this.setState(State.DIVE);
    }

    private void tickDive() {
        Vec3 velocity = this.getDeltaMovement().scale(0.92);
        double speed = Math.max(0.3, velocity.length());
        // Curve downward so it digs deeper instead of skimming just under the surface,
        // which left long bodies hanging out of the ground
        velocity = (velocity.lengthSqr() > 1.0E-6 ? velocity.normalize() : new Vec3(0.0, -1.0, 0.0)).add(0.0, -0.15, 0.0).normalize().scale(speed);
        this.setDeltaMovement(velocity);
        this.faceDirection(velocity);
        this.diveDistance += velocity.length();
        this.burySegments();
        this.biteTouching();

        if (this.diveEntry != null && this.diveBlock != null && this.stateTicks % 3 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, this.diveBlock), this.diveEntry.x, this.diveEntry.y, this.diveEntry.z,
                    4, 0.25 * this.getSizeScale(), 0.1, 0.25 * this.getSizeScale(), 0.1);
        }

        // Done once every segment has gone into the ground (or after 10 seconds as a fail-safe),
        // so a long body never vanishes while part of it is still sticking out
        int allSegments = (1 << this.getSegmentCount()) - 1;
        boolean allBuried = (this.buriedSegments & allSegments) == allSegments;
        if ((allBuried && this.diveDistance >= 1.0) || this.stateTicks > 200) {
            this.setDeltaMovement(Vec3.ZERO);
            this.diveEntry = null;
            this.diveBlock = null;
            this.travelTarget = null;
            this.restTicks = this.randomRestTicks();
            this.setState(State.BURROWED);
            if (this.getSize() == 0 && this.charges >= SMALL_MAX_CHARGES && !this.isPersistenceRequired()) {
                // Out of steam: it slinks off deeper into the rock (name-tagged ones stay)
                this.discard();
            }
        }
    }

    /** Plays a break sound and particles at each body segment as it goes into the ground. */
    private void burySegments() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        int count = this.getSegmentCount();
        for (int i = 1; i < count; i++) {
            if ((this.buriedSegments & (1 << i)) != 0) continue;
            Vec3 pos = this.segmentPos[i];
            if (pos == null) continue;
            BlockPos blockPos = BlockPos.containing(pos);
            BlockState state = this.level().getBlockState(blockPos);
            if (state.getCollisionShape(this.level(), blockPos).isEmpty()) continue;
            this.buriedSegments |= 1 << i;
            float width = this.getSegmentWidth(i);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.x, pos.y, pos.z,
                    6 + (int) (width * 8), width * 0.4, width * 0.4, width * 0.4, 0.15);
            SoundEvent breakSound = state.getSoundType(this.level(), blockPos, this).getBreakSound();
            // Pitch climbs a little along the body, so the segments sound like a quick run of digs
            this.level().playSound(null, blockPos, breakSound, SoundSource.HOSTILE, 0.6F, 0.8F + i * 0.06F + this.random.nextFloat() * 0.1F);
        }
    }

    private void startStun() {
        if (this.lastAirCenter != null) {
            // Back out of the block we bonked into so physics doesn't trap us in it
            this.setCenter(this.lastAirCenter);
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
        this.playSound(ModSoundEvents.CAVE_BORER_HURT.get(), 1.0F, 0.6F);
        this.popOut(STUN_TICKS);
    }

    /** Switch to the exposed, physics-driven state for a while. Used for bonks, splits and spawn eggs. */
    private void popOut(int ticks) {
        this.stunTicks = ticks;
        this.setState(State.STUNNED);
    }

    private void tickStunned() {
        if (this.stateTicks < this.stunTicks || this.stateTicks % 10 != 0) return;

        BlockPos below = this.blockPosition().below();
        BlockState belowState = this.level().getBlockState(below);
        if (this.onGround() && belowState.is(ModBlockTags.CAVE_BORER_DIGGABLE)) {
            this.startDive(below, belowState, new Vec3(0.0, -0.3, 0.0));
            return;
        }
        if (this.getNavigation().isDone()) {
            BlockPos dig = this.findNearbyDiggable(6);
            if (dig != null) {
                this.getNavigation().moveTo(dig.getX() + 0.5, dig.getY() + 1.0, dig.getZ() + 0.5, 1.0);
            }
        }
    }

    @Nullable
    private BlockPos findNearbyDiggable(int radius) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 1, radius))) {
            if (!this.level().getBlockState(pos).is(ModBlockTags.CAVE_BORER_DIGGABLE)) continue;
            BlockPos above = pos.above();
            if (!this.level().getBlockState(above).getCollisionShape(this.level(), above).isEmpty()) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private void biteTouching() {
        // Bite with the model's size, not the enlarged hitbox, so bites don't get harder to dodge
        double width = this.getSegmentWidth(0);
        AABB box = AABB.ofSize(this.getCenter(), width, width, width).inflate(0.25);
        LivingEntity target = this.getTarget();
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && !(e instanceof CaveBorer) && (e instanceof Player || e == target))) {
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) continue;
            if (!this.bitten.add(entity.getId())) continue;
            if (this.doHurtTarget(entity)) {
                this.playSound(ModSoundEvents.CAVE_BORER_BITE.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
                Vec3 velocity = this.getDeltaMovement();
                entity.knockback(0.4, -velocity.x, -velocity.z);
            }
        }
    }

    private void faceDirection(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6) return;
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yHeadRot = yaw;
        this.yBodyRot = yaw;
    }

    private int randomRestTicks() {
        double min = SpelunkersCharmConfig.CAVE_BORER_REST_MIN_SECONDS.get();
        double max = Math.max(min, SpelunkersCharmConfig.CAVE_BORER_REST_MAX_SECONDS.get());
        return (int) ((min + this.random.nextDouble() * (max - min)) * 20.0);
    }

    private void clearCrack() {
        if (this.emergeBlock != null) {
            this.level().destroyBlockProgress(this.getId(), this.emergeBlock, -1);
        }
    }

    // ===== Death, splitting and loot =====

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!this.level().isClientSide) {
            this.clearCrack();
            int size = this.getSize();
            if (size > 0 && this.isDeadOrDying() && reason == RemovalReason.KILLED && SpelunkersCharmConfig.CAVE_BORER_SPLITTING.get()) {
                // Pending Bryan's pick: everything from size 2 up splits into mediums, so big kills don't snowball
                this.split(Math.min(size - 1, 1));
            }
        }
        super.remove(reason);
    }

    /** Like a slime: a large splits into 2 to 3 mediums, a medium into 2 to 3 smalls. */
    private void split(int childSize) {
        int count = 2 + this.random.nextInt(2);
        Vec3 center = this.getCenter();
        for (int i = 0; i < count; i++) {
            CaveBorer child = ModEntityType.CAVE_BORER.get().create(this.level());
            if (child == null) continue;
            if (this.isPersistenceRequired()) child.setPersistenceRequired();
            child.setCustomName(this.getCustomName());
            child.setNoAi(this.isNoAi());
            child.setInvulnerable(this.isInvulnerable());
            child.setSize(childSize, true);
            child.setVariant(this.getVariant());
            double spread = 0.25 * this.getSizeScale();
            child.moveTo(center.x + (this.random.nextDouble() - 0.5) * spread, center.y - child.getBbHeight() * 0.5,
                    center.z + (this.random.nextDouble() - 0.5) * spread, this.random.nextFloat() * 360.0F, 0.0F);
            child.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.3, 0.25, (this.random.nextDouble() - 0.5) * 0.3);
            child.popOut(15);
            this.level().addFreshEntity(child);
        }
    }

    @Override
    public @NotNull ResourceKey<LootTable> getDefaultLootTable() {
        // Only the smallest size drops loot, like slimes
        // (or every size when splitting is turned off in the config)
        return this.getSize() == 0 || !SpelunkersCharmConfig.CAVE_BORER_SPLITTING.get() ? this.getVariant().getLootTable() : BuiltInLootTables.EMPTY;
    }

    // ===== Sounds =====

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return ModSoundEvents.CAVE_BORER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.CAVE_BORER_DEATH.get();
    }
}
