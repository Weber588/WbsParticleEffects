package wbs.particleeffects.instance;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import wbs.particleeffects.ParticleEffectSettings;
import wbs.particleeffects.PersistentEffect;
import wbs.particleeffects.PersistentEffectGroup;
import wbs.utils.util.configuration.WbsConfigReader;

import java.util.LinkedList;
import java.util.List;

public abstract class EffectGroupInstance {

    public static EffectGroupInstance buildInstance(PersistentEffectGroup type, ConfigurationSection section, ParticleEffectSettings settings, String directory) {
        ConfigurationSection ambientConfig = section.getConfigurationSection("ambient");

        if (ambientConfig != null) {
            return new AmbientEffectGroupInstance(type, section, settings, directory);
        } else {
            return new StaticEffectGroupInstance(type, section, settings, directory);
        }
    }

    public static EffectGroupInstance buildInstance(PersistentEffectGroup type, String id, Location loc1, Location loc2, boolean enabled) {
        if (loc1.equals(loc2)) {
            return new StaticEffectGroupInstance(type, id, loc1, enabled);
        } else {
            return new AmbientEffectGroupInstance(type, id, loc1, loc2, enabled);
        }
    }

    protected final String id;
    protected boolean enabled;

    // When locked, it cannot be moved or deleted in game, even by an op.
    private boolean locked;

    protected final List<PersistentEffect> effects = new LinkedList<>();
    private final PersistentEffectGroup type;

    private boolean updated = false;

    protected EffectGroupInstance(PersistentEffectGroup type, String id, boolean enabled) {
        this.type = type;
        this.id = id;
        this.enabled = enabled;
    }

    protected EffectGroupInstance(PersistentEffectGroup type, ConfigurationSection section, ParticleEffectSettings settings, String directory) {
        WbsConfigReader.requireNotNull(section, "enabled", settings, directory);

        id = section.getName();
        this.type = type;

        if (section.get("locked") != null) {
            locked = section.getBoolean("locked", false);
        }

        enabled = section.getBoolean("enabled");
    }

    protected Location loadLocationFromString(String locString, ParticleEffectSettings settings, String directory) {
        if (locString == null) {
            settings.logError("Malformed location string (x,y,z,world).", directory);
            return null;
        }
        String[] args = locString.split(",");

        if (args.length != 4) {
            settings.logError("Malformed location string (x,y,z,world).", directory);
            return null;
        }

        Location builtLocation = null;

        try {
            double x = Double.parseDouble(args[0].trim());
            double y = Double.parseDouble(args[1].trim());
            double z = Double.parseDouble(args[2].trim());
            String worldString = args[3].trim();

            World world = Bukkit.getWorld(worldString);
            if (world == null) {
                settings.logError("Malformed location string (invalid world (x,y,z,world)).", directory);
                return null;
            }

            builtLocation = new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            settings.logError("Malformed location string (bad co-ord value (x,y,z,world)).", directory);
            e.printStackTrace();
        }

        return builtLocation;
    }

    /**
     * Start this effect regardless of if it was enabled or not
     * @return True if the state changed. False if it was already enabled.
     */
    public boolean start() {
        boolean wasEnabled = enabled;
        enabled = true;
        for (PersistentEffect effect : effects) {
            effect.start();
        }

        if (!wasEnabled) {
            setUpdated();
        }

        return !wasEnabled;
    }

    public abstract void runEffect(PersistentEffect persistentEffect);

    /**
     * Enable this effect if it's disabled.
     * @return True if the state changed. False if it was already enabled.
     */
    public boolean enable() {
        if (enabled) return false;
        setUpdated();
        enabled = true;
        for (PersistentEffect effect : effects) {
            effect.start();
        }
        return true;
    }

    /**
     * Enable this effect if it's disabled.
     * @return True if the state changed. False if it was already disabled.
     */
    public boolean disable() {
        if (!enabled) return false;
        setUpdated();
        enabled = false;
        for (PersistentEffect effect : effects) {
            effect.stop();
        }
        return true;
    }

    /**
     * Toggle this group
     * @return The new state of this group
     */
    public boolean toggle() {
        setUpdated();
        if (enabled) {
            disable();
        } else {
            enable();
        }
        return enabled;
    }


    public String getId() {
        return id;
    }

    public void addEffect(PersistentEffect effect) {
        effects.add(effect);
    }

    public void readEffectsFromConfig(ConfigurationSection section, ParticleEffectSettings settings, String directory) {
        for (String effectKey : section.getKeys(false)) {
            PersistentEffect effect = new PersistentEffect(this, section.getConfigurationSection(effectKey), settings, directory + "/effects/" + effectKey);

            addEffect(effect);
        }
    }

    public void writeToConfig(ConfigurationSection effectsConfig) {
        effectsConfig.set(id + ".enabled", enabled);
        // Don't need to save locked as it shouldn't change in game
    }

    public boolean isActive() {
        return enabled;
    }

    public PersistentEffectGroup getType() {
        return type;
    }

    /**
     * Move the effect to a location
     * @param location The location to move to
     * @return True if it moved, false if it was locked.
     */
    public boolean move(Location location) {
        setUpdated();
        return !locked;
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * Mark this instance as updated & needing saving.
     * @return True if the instance was updated, or false if it was locked.
     */
    public boolean setUpdated() {
        if (!locked) {
            updated = true;
        }
        return !locked;
    }

    public abstract void teleportPlayer(Player player);

    public boolean isUpdated() {
        return updated;
    }
}
