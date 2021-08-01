package wbs.particleeffects;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import wbs.particleeffects.instance.EffectGroupInstance;
import wbs.utils.exceptions.InvalidConfigurationException;
import wbs.utils.util.WbsColours;
import wbs.utils.util.WbsEnums;
import wbs.utils.util.configuration.NumProvider;
import wbs.utils.util.configuration.VectorProvider;
import wbs.utils.util.configuration.WbsConfigReader;
import wbs.utils.util.particles.WbsParticleEffect;

public class PersistentEffect {

    private final WbsParticleEffects plugin;

    public int interval; // In ticks
    public Particle particle;
    private final WbsParticleEffect effect;
    private final EffectGroupInstance parent;
    private final VectorProvider offset;

    private Object data; // For particles requiring data

    private boolean rainbow = false; // Only used when particle is redstone
    private NumProvider rainbowSpeed = new NumProvider(1);
    private NumProvider size = new NumProvider(1);
    private VectorProvider rgbVector = new VectorProvider(255, 0, 0);

    public PersistentEffect(EffectGroupInstance parent, ConfigurationSection section, ParticleEffectSettings settings, String directory) {
        this.parent = parent;

        WbsConfigReader.requireNotNull(section, "interval", settings, directory);
        interval = section.getInt("interval");

        effect = WbsParticleEffect.buildParticleEffect(section, settings, directory);

        WbsConfigReader.requireNotNull(section, "particle", settings, directory);
        String particleString = section.getString("particle");
        particle = WbsEnums.particleFromString(particleString);

        if (particle == null) {
            settings.logError("Invalid particle: " + particleString, directory + "/particle");
            throw new InvalidConfigurationException();
        }

        Class<?> clazz = particle.getDataType();
        if (clazz != Void.class) {
            if (clazz == Particle.DustOptions.class) {
                if (section.get("rainbow") != null) {
                    rainbow = section.getBoolean("rainbow");
                } else {
                    rainbow = false;
                }

                if (rainbow) {
                    if (section.get("rainbowSpeed") != null) {
                        rainbowSpeed = new NumProvider(section, "rainbowSpeed", settings, directory + "/rainbowSpeed");
                    } else {
                        rainbowSpeed = new NumProvider(1);
                    }
                } else {
                    ConfigurationSection colourSection = WbsConfigReader.getRequiredSection(section, "colour", settings, directory);
                    rgbVector = new VectorProvider(colourSection, settings, directory + "/colour");
                }

                WbsConfigReader.requireNotNull(section, "size", settings, directory + "/size");
                size = new NumProvider(section, "size", settings, directory + "/size");

                data = new Particle.DustOptions(
                        Color.fromRGB(
                                (int) rgbVector.getX(),
                                (int) rgbVector.getY(),
                                (int) rgbVector.getZ()
                        ), (float) size.val()
                );
                // bool rainbow
                // if not rainbow, rgb vector
                // if rainbow, rainbowSpeed
                // number size (0-2)
            } else if (clazz == BlockData.class) {
                WbsConfigReader.requireNotNull(section, "blockType", settings, directory);
                Material blockType = WbsEnums.materialFromString(section.getString("blockType"));

                if (!blockType.isBlock()) {
                    throw new InvalidConfigurationException();
                }

                data = Bukkit.createBlockData(blockType);
            } else if (clazz == ItemStack.class) {
                WbsConfigReader.requireNotNull(section, "itemType", settings, directory);
                Material blockType = WbsEnums.materialFromString(section.getString("itemType"));

                if (!blockType.isItem()) {
                    throw new InvalidConfigurationException();
                }

                data = Bukkit.createBlockData(blockType);
            }

            effect.setOptions(clazz.cast(data));
        }

        plugin = settings.getPlugin();

        if (section.get("offset") != null) {
            ConfigurationSection offsetSection = WbsConfigReader.getRequiredSection(section, "offset", settings, directory);
            offset = new VectorProvider(offsetSection, settings, directory + "/offset", new Vector(0, 1, 0));
        } else {
            offset = new VectorProvider(0, 1, 0);
        }
    }

    public void writeToConfig(ConfigurationSection section, String path) {
        effect.writeToConfig(section, path);
        section.set(path + ".interval", interval);
    }


    private int runnableId = -1;

    public boolean start() {
        if (runnableId != -1) {
            return false;
        }

        runnableId = new BukkitRunnable() {
            @Override
            public void run() {
                offset.refresh();

                if (data instanceof Particle.DustOptions) {
                    rainbowSpeed.refresh();
                    size.refresh();
                    rgbVector.refresh();

                    if (rainbow) {
                        cycleRainbow();
                    } else {
                        data = new Particle.DustOptions(
                                Color.fromRGB(
                                        (int) rgbVector.getX(),
                                        (int) rgbVector.getY(),
                                        (int) rgbVector.getZ()
                                ), (float) size.val()
                        );
                    }
                    effect.setOptions(data);
                }

                effect.build();
                parent.runEffect(PersistentEffect.this);
            }
        }.runTaskTimer(plugin, 1, interval).getTaskId();

        return true;
    }

    public PersistentEffect stop() {
        if (runnableId != -1) {
            Bukkit.getScheduler().cancelTask(runnableId);
            runnableId = -1;
        }

        return this;
    }

    // for DustOptions data rainbow cycling

    private float age = 0;

    public void cycleRainbow() {
        if (data instanceof Particle.DustOptions) {
            age += 0.002 * interval * rainbowSpeed.val();

            Color colour = WbsColours.fromHSB(age, 1, 1);

            data = new Particle.DustOptions(colour, (float) size.val());
        }
    }

    public WbsParticleEffect getEffect() {
        return effect;
    }
    public Particle getParticle() {
        return particle;
    }

    public VectorProvider getOffset() {
        return offset;
    }
}
