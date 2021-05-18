package ac.grim.grimac.utils.data;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.Collisions;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PredictionData {
    public GrimPlayer player;
    public double playerX;
    public double playerY;
    public double playerZ;
    public double teleportX;
    public double teleportY;
    public double teleportZ;
    public boolean teleportXRelative;
    public boolean teleportYRelative;
    public boolean teleportZRelative;
    public float xRot;
    public float yRot;
    public boolean onGround;
    public boolean isSprinting;
    public boolean isSneaking;
    public boolean isFlying;
    public boolean isClimbing;
    public boolean isFallFlying;
    public World playerWorld;
    public WorldBorder playerWorldBorder;

    public double movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float slowFallingAmplifier;
    public float dolphinsGraceAmplifier;
    public float flySpeed;

    public double fallDistance;

    // Debug, does nothing.
    public int number;

    public boolean inVehicle;
    public Entity playerVehicle;
    public float vehicleHorizontal;
    public float vehicleForward;

    public boolean isSprintingChange;
    public boolean isSneakingChange;

    public Vector firstBreadKB = null;
    public Vector requiredKB = null;

    public Vector firstBreadExplosion = null;
    public List<Vector> possibleExplosion = new ArrayList<>();
    public Vector lastTeleport;

    public int minimumTickRequiredToContinue;
    public int lastTransaction;

    // For regular movement
    public PredictionData(GrimPlayer player, double playerX, double playerY, double playerZ, float xRot, float yRot, boolean onGround) {
        this.player = player;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.onGround = onGround;
        this.inVehicle = player.playerVehicle != null;

        this.teleportX = player.packetTeleportX;
        this.teleportY = player.packetTeleportY;
        this.teleportZ = player.packetTeleportZ;

        this.teleportXRelative = player.packetTeleportXRelative;
        this.teleportYRelative = player.packetTeleportYRelative;
        this.teleportZRelative = player.packetTeleportZRelative;

        player.packetTeleportX = Double.NaN;
        player.packetTeleportY = Double.NaN;
        player.packetTeleportZ = Double.NaN;

        this.number = player.taskNumber.getAndIncrement();

        this.isSprinting = player.isPacketSprinting;
        this.isSneaking = player.isPacketSneaking;

        this.isSprintingChange = player.isPacketSprintingChange;
        this.isSneakingChange = player.isPacketSneakingChange;
        player.isPacketSprintingChange = false;
        player.isPacketSneakingChange = false;

        // Flying status is just really. really. complicated.  You shouldn't need to touch this, but if you do -
        // Don't let the player fly with packets
        // Accept even if bukkit says the player can't fly lag might allow them to
        // Accept that the server can change the player's packets without an update response from the player
        // Accept that the player's flying status lies when landing on the ground
        //
        // This isn't perfect but I'm not doubling required scenarios because of flying...

        // This will break on 1.7
        if (player.bukkitPlayer.getGameMode() == GameMode.SPECTATOR) {
            player.packetFlyingDanger = true;
        }

        this.isFlying = player.compensatedFlying.somewhatLagCompensatedIsPlayerFlying() && player.compensatedFlying.getCanPlayerFlyLagCompensated(player.lastTransactionBeforeLastMovement);


        this.isClimbing = Collisions.onClimbable(player);
        this.isFallFlying = player.bukkitPlayer.isGliding();
        this.playerWorld = player.bukkitPlayer.getWorld();
        this.fallDistance = player.bukkitPlayer.getFallDistance();
        this.movementSpeed = player.bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();

        // When a player punches a mob, bukkit thinks the player isn't sprinting (?)
        // But they are, so we need to multiply by sprinting speed boost until I just get the player's attributes from packets
        if (isSprinting && !player.bukkitPlayer.isSprinting()) this.movementSpeed *= 1.3;

        Collection<PotionEffect> playerPotionEffects = player.bukkitPlayer.getActivePotionEffects();

        this.jumpAmplifier = getHighestPotionEffect(playerPotionEffects, "JUMP", 0);
        this.levitationAmplifier = getHighestPotionEffect(playerPotionEffects, "LEVITATION", 9);
        this.slowFallingAmplifier = getHighestPotionEffect(playerPotionEffects, "SLOW_FALLING", 13);
        this.dolphinsGraceAmplifier = getHighestPotionEffect(playerPotionEffects, "DOLPHINS_GRACE", 13);

        this.flySpeed = player.bukkitPlayer.getFlySpeed() / 2;
        this.playerVehicle = player.bukkitPlayer.getVehicle();

        firstBreadKB = player.compensatedKnockback.getFirstBreadOnlyKnockback();
        requiredKB = player.compensatedKnockback.getRequiredKB();
        lastTeleport = player.packetLastTeleport;

        player.packetLastTeleport = null;

        firstBreadExplosion = player.compensatedExplosion.getFirstBreadAddedExplosion();
        possibleExplosion = player.compensatedExplosion.getPossibleExplosions();

        minimumTickRequiredToContinue = GrimAC.currentTick.get() + 1;
        lastTransaction = player.packetLastTransactionReceived;
    }

    // For boat movement
    public PredictionData(GrimPlayer player, double boatX, double boatY, double boatZ, float xRot, float yRot) {
        this.player = player;
        this.playerX = boatX;
        this.playerY = boatY;
        this.playerZ = boatZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.playerVehicle = player.bukkitPlayer.getVehicle();
        this.vehicleForward = player.packetVehicleForward;
        this.vehicleHorizontal = player.packetVehicleHorizontal;

        this.teleportX = player.packetTeleportX;
        this.teleportY = player.packetTeleportY;
        this.teleportZ = player.packetTeleportZ;

        this.teleportXRelative = player.packetTeleportXRelative;
        this.teleportYRelative = player.packetTeleportYRelative;
        this.teleportZRelative = player.packetTeleportZRelative;

        player.packetTeleportX = Double.NaN;
        player.packetTeleportY = Double.NaN;
        player.packetTeleportZ = Double.NaN;

        this.inVehicle = true;

        this.isFlying = false;
        this.isClimbing = false;
        this.isFallFlying = false;
        this.playerWorld = player.bukkitPlayer.getWorld();
        this.fallDistance = player.bukkitPlayer.getFallDistance();
        this.movementSpeed = player.bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();

        minimumTickRequiredToContinue = GrimAC.currentTick.get() + 1;
        lastTransaction = player.packetLastTransactionReceived;
    }

    private float getHighestPotionEffect(Collection<PotionEffect> effects, String typeName, int minimumVersion) {
        if (XMaterial.getVersion() < minimumVersion) return 0;

        PotionEffectType type = PotionEffectType.getByName(typeName);

        float highestEffect = 0;
        for (PotionEffect effect : effects) {
            if (effect.getType() == type && effect.getAmplifier() > highestEffect)
                highestEffect = effect.getAmplifier();
        }

        return highestEffect;
    }
}
