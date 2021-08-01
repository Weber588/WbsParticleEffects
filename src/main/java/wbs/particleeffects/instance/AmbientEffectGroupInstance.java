package wbs.particleeffects.instance;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import wbs.particleeffects.ParticleEffectSettings;
import wbs.particleeffects.PersistentEffect;
import wbs.particleeffects.PersistentEffectGroup;
import wbs.utils.exceptions.InvalidConfigurationException;
import wbs.utils.util.configuration.WbsConfigReader;
import wbs.utils.util.particles.CuboidParticleEffect;
import wbs.utils.util.particles.WbsParticleEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AmbientEffectGroupInstance extends EffectGroupInstance {

    private World world;
    private Vector pos1, pos2;

    private Location center;

    private EffectGroupInstance instance;

    public AmbientEffectGroupInstance(PersistentEffectGroup type, String id, Location loc1, Location loc2, boolean enabled) {
        super(type, id, enabled);

        world = loc1.getWorld();
        pos1 = loc1.toVector();
        pos2 = loc2.toVector();
    }

    public AmbientEffectGroupInstance(PersistentEffectGroup type, ConfigurationSection section, ParticleEffectSettings settings, String directory) {
        super(type, section, settings, directory);

        WbsConfigReader.requireNotNull(section, "ambient", settings, directory);
        ConfigurationSection ambientSection = section.getConfigurationSection("ambient");

        assert ambientSection != null;

        WbsConfigReader.requireNotNull(ambientSection, "pos1", settings, directory);
        WbsConfigReader.requireNotNull(ambientSection, "pos2", settings, directory);
        WbsConfigReader.requireNotNull(ambientSection, "world", settings, directory);

        String worldName = ambientSection.getString("world");
        assert worldName != null;

        ConfigurationSection pos1Section = ambientSection.getConfigurationSection("pos1");
        if (pos1Section == null) {
            settings.logError("pos1 was an invalid section", directory + "/ambient/pos1");
            throw new InvalidConfigurationException();
        }

        pos1 = new Vector(
                pos1Section.getDouble("x"),
                pos1Section.getDouble("y"),
                pos1Section.getDouble("z")
        );

        ConfigurationSection pos2Section = ambientSection.getConfigurationSection("pos2");
        assert pos2Section != null;

        pos2 = new Vector(
                pos2Section.getDouble("x"),
                pos2Section.getDouble("y"),
                pos2Section.getDouble("z")
        );

        Vector temp = pos1.clone();

        pos1 = Vector.getMinimum(pos1, pos2);
        pos2 = Vector.getMaximum(temp, pos2);

        world = Bukkit.getWorld(worldName);

        if (world == null) {
            settings.logError("World not found: " + worldName, directory + "/world");
            throw new InvalidConfigurationException();
        }
    }

    @Override
    public void runEffect(PersistentEffect persistentEffect) {
        WbsParticleEffect effect = persistentEffect.getEffect();

        List<Player> playersInWorld = world.getPlayers();

        for (Player player : playersInWorld) {
            Location playerLoc = player.getLocation();
            if (playerLoc.toVector().isInAABB(pos1, pos2)) {
                effect.play(
                        persistentEffect.getParticle(),
                        playerLoc.add(persistentEffect.getOffset().val()),
                        player
                );
            }
        }
    }

    @Override
    public boolean move(Location location) {
        if (!super.move(location)) return false;

        calcCenter();

        Vector toPos1 = pos1.subtract(center.toVector());
        Vector toPos2 = pos2.subtract(center.toVector());

        pos1 = location.add(toPos1).toVector();
        pos2 = location.add(toPos2).toVector();
        return true;
    }

    @Override
    public void teleportPlayer(Player player) {
        calcCenter();
        player.teleport(center);
    }

    private void calcCenter() {
        center = new Location(
                world,
                (pos1.getX() + pos2.getX()) / 2,
                (pos1.getY() + pos2.getY()) / 2,
                (pos1.getZ() + pos2.getZ()) / 2
        );
    }

    @Override
    public void writeToConfig(ConfigurationSection effectsConfig) {
        super.writeToConfig(effectsConfig);

        effectsConfig.set(id + ".ambient.world", world.getName());

        effectsConfig.set(id + ".ambient.pos1.x", pos1.getX());
        effectsConfig.set(id + ".ambient.pos1.y", pos1.getY());
        effectsConfig.set(id + ".ambient.pos1.z", pos1.getZ());

        effectsConfig.set(id + ".ambient.pos2.x", pos2.getX());
        effectsConfig.set(id + ".ambient.pos2.y", pos2.getY());
        effectsConfig.set(id + ".ambient.pos2.z", pos2.getZ());
    }

    private final Map<UUID, BukkitRunnable> effectMap = new HashMap<>();

    @Override
    public boolean disable() {
        boolean disabled = super.disable();

        if (disabled) {
            for (BukkitRunnable runnable : effectMap.values()) {
                runnable.cancel();
            }
        }

        effectMap.clear();

        return disabled;
    }

    /**
     * Highlight the region this effect appears in to a given player
     * @param player The player to show the region to
     * @return The new state of the highlight (true = visible, false = invisible)
     */
    public boolean highlightFor(Player player, Plugin plugin) {
        UUID uuid = player.getUniqueId();

        if (effectMap.containsKey(uuid)) {
            effectMap.get(uuid).cancel();
            effectMap.remove(uuid);
            return false;
        }

        CuboidParticleEffect effect = new CuboidParticleEffect();
        effect.setAmount(2);
        effect.setScaleAmount(true);

        BukkitRunnable runnable = new BukkitRunnable() {
            public final CuboidParticleEffect cuboidEffect = effect;

            @Override
            public void run() {
                calcCenter();
                effect.setX(Math.abs(pos1.getX() - pos2.getX()));
                effect.setY(Math.abs(pos1.getY() - pos2.getY()));
                effect.setZ(Math.abs(pos1.getZ() - pos2.getZ()));

                cuboidEffect.build();
                cuboidEffect.play(Particle.END_ROD, center, player);
            }
        };

        runnable.runTaskTimer(plugin, 0, 5);
        effectMap.put(uuid, runnable);

        return true;
    }

    /**
     * Set the area of this effect
     * @param pos1 The first corner
     * @param pos2 The second corner
     * @return True if changed, false if the worlds differed between pos1 and pos2
     */
    public boolean setBoundingBox(Location pos1, Location pos2) {
        if (pos1.getWorld() != pos2.getWorld()) {
            return false;
        }

        this.pos1 = pos1.toVector();
        this.pos2 = pos2.toVector();
        this.world = pos1.getWorld();

        calcCenter();

        setUpdated();

        return true;
    }
}
