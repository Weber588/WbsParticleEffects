package wbs.particleeffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import wbs.particleeffects.instance.EffectGroupInstance;
import wbs.utils.exceptions.MissingRequiredKeyException;
import wbs.utils.exceptions.InvalidConfigurationException;
import wbs.utils.util.configuration.WbsConfigReader;
import wbs.utils.util.plugin.WbsSettings;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ParticleEffectSettings extends WbsSettings {

    private final WbsParticleEffects plugin;
    protected ParticleEffectSettings(WbsParticleEffects plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    private final Map<String, PersistentEffectGroup> allGroups = new HashMap<>();
    private final Map<String, File> groupFiles = new HashMap<>();
    private final Map<String, EffectGroupInstance> allInstances = new HashMap<>();

    private final Map<String, EffectGroupInstance> instancesPendingDeletion = new HashMap<>();

    public WbsParticleEffects getPlugin() {
        return plugin;
    }

    @Override
    public void reload() {
        reload(true);
    }

    public void reload(boolean save) {
        saveAllInstances(false);
        disableAllEffects();

        instanceNames.clear();

        allGroups.clear();
        allInstances.clear();
        errors.clear();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        genConfig("effects/DancingEndRods.yml");
        YamlConfiguration config = loadConfigSafely(genConfig("config.yml"));

        loadMessageFormat(config);

        loadEffects();
        startEnabledEffects();

        if (!errors.isEmpty()) {
            plugin.logger.warning(errors.size() + " errors found:");
        } else {
            plugin.logger.info("Reloaded successfully without errors");
        }
    }

    private void startEnabledEffects() {
        for (EffectGroupInstance instance : allInstances.values()) {
            if (instance.isActive()) {
                instance.start();
            }
        }
    }

    private void loadEffects() {
        final File effectsDirectory =  new File(plugin.getDataFolder() + File.separator + "effects");

        File[] effectFiles = effectsDirectory.listFiles();
        if (effectFiles == null) {
            logError("An unexpected error occurred due while loading the effects directory. Please contact your system administrator.", "Internal");
            return;
        }

        int successful = 0;
        int failed = 0;

        for (File file : effectFiles) {
            ConfigurationSection effectGroupConfig = loadConfigSafely(file);
            assert effectGroupConfig != null;
            try {
                String id = file.getName().split("\\.")[0];
                groupFiles.put(id, file);

                PersistentEffectGroup newGroup = new PersistentEffectGroup(id, effectGroupConfig, this, file.getName());
                allGroups.put(newGroup.getId(), newGroup);

                successful++;
            } catch (MissingRequiredKeyException | InvalidConfigurationException e) {
                failed++;
            }
        }

        logger.info("Successfully loaded " + successful + " groups. Failed to load " + failed + " groups.");
    }

    public List<String> allGroupNames() {
        return allGroups.values().stream().map(PersistentEffectGroup::getId).collect(Collectors.toList());
    }

    public Map<String, PersistentEffectGroup> getAllGroups() {
        return new HashMap<>(allGroups);
    }

    public void disableAllEffects() {
        for (EffectGroupInstance instance : allInstances.values()) {
            instance.disable();
        }
    }

    public void enableAllEffects() {
        for (EffectGroupInstance instance : allInstances.values()) {
            instance.enable();
        }
    }

    public PersistentEffectGroup getEffectGroup(String groupId) {
        return allGroups.get(groupId);
    }

    public EffectGroupInstance getEffectGroupInstance(String instanceId) {
        return allInstances.get(instanceId);
    }

    /**
     * Add an instance directly, not from a config.
     * @param instance The instance to add.
     * @return True if the instance was added, false if something went wrong.
     */
    public boolean addInstance(EffectGroupInstance instance) {
        String instanceId = instance.getId();
        File currentGroupFile = groupFiles.get(instance.getType().getId());

        if (instanceNames.containsKey(instanceId)) {
            logError("Duplicate instance name \"" + instance.getId() + "\" in file " +
                            instanceNames.get(instanceId) + ".", "Command");
            return false;
        }
        instanceNames.put(instanceId, currentGroupFile.getName());

        allInstances.put(instance.getId(), instance);
        if (instance.isActive()) {
            instance.start();
        }
        instance.setUpdated(); // Mark for saving since it wasn't created from a config
        return true;
    }

    /**
     * A map of instance names to their group's file name.
     * This will contain invalid instance names in the case of duplicates.
     */
    private final Map<String, String> instanceNames = new HashMap<>();

    /**
     * Add an instance from a config file.
     * @param instance The instance to add
     * @param directory The path to the instance in its config.
     */
    public void addInstance(EffectGroupInstance instance, String directory) {
        String instanceId = instance.getId();
        File currentGroupFile = groupFiles.get(instance.getType().getId());

        if (instanceNames.containsKey(instanceId)) {
            logError("Duplicate instance name \"" + instance.getId() + "\" in file " +
                            instanceNames.get(instanceId) + ".",
                    directory);

            // Put new file name in so on each subsequent duplicate, the previous duplicate's file is shown.
            instanceNames.put(instanceId, currentGroupFile.getName());

            allInstances.get(instanceId).disable();
            allInstances.remove(instanceId);

            throw new InvalidConfigurationException();
        }
        instanceNames.put(instanceId, currentGroupFile.getName());

        allInstances.put(instance.getId(), instance);
        if (instance.isActive()) {
            instance.start();
        }
    }

    public Map<String, EffectGroupInstance> getAllInstances() {
        return new HashMap<>(allInstances);
    }

    public boolean createNewInstance(PersistentEffectGroup group, String id, Location loc, boolean enabled) {
        return createNewInstance(group, id, loc, loc, enabled);
    }

    /**
     * Create a new instance, register it, and save it to the config.
     * @param group The group to create a new instance of
     * @param id The id for the new instance
     * @param loc1 The location for the instance to render, or, if
     *             the effect is ambient, the first corner of the region
     * @param loc2 The second location for the ambient effect, if applicable.
     * @param enabled Whether the instance should be enabled
     * @return True if the instance was created successfully, false otherwise.
     */
    public boolean createNewInstance(PersistentEffectGroup group, String id, Location loc1, Location loc2, boolean enabled) {
        File groupFile = groupFiles.get(group.getId());
        FileConfiguration effectGroupConfig = YamlConfiguration.loadConfiguration(groupFile);
    //    ConfigurationSection effectGroupConfig = loadConfigSafely(groupFile);


        ConfigurationSection effectsConfig = WbsConfigReader.getRequiredSection(effectGroupConfig, "effects", this, groupFile.getName());

        EffectGroupInstance newInstance;
        try {
            newInstance = EffectGroupInstance.buildInstance(group, id, loc1, loc2, enabled);

            newInstance.readEffectsFromConfig(effectsConfig, this, groupFile.getName() + "/effects");

            addInstance(newInstance, "Command");
        } catch (MissingRequiredKeyException | InvalidConfigurationException e) {
            return false;
        }

        ConfigurationSection instancesSection = WbsConfigReader.getRequiredSection(effectGroupConfig, "instances", this, groupFile.getName());


        return saveInstance(effectGroupConfig, groupFile, newInstance);
    }

    private boolean saveInstance(FileConfiguration fileConfig, File file, EffectGroupInstance instance) {
        try {
            fileConfig.save(file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Save all instances that loaded correctly.
     * @param force Whether or not to force save all instances.
     *              If true, even instances that weren't updated will save.
     */
    public void saveAllInstances(boolean force) {
        removePendingInstances();

        Map<PersistentEffectGroup, FileConfiguration> fileConfigs = new HashMap<>();

        // Load configs
        for (PersistentEffectGroup group : allGroups.values()) {
            if (!fileConfigs.containsKey(group)) {
                boolean save = force;
                if (!save) {
                    for (EffectGroupInstance instance : getAllInstances().values()) {
                        if (group.equals(instance.getType())) {
                            if (instance.isUpdated()) {
                                save = true;
                            }
                        }
                    }
                }

                if (save) {
                    FileConfiguration effectGroupConfig = YamlConfiguration.loadConfiguration(groupFiles.get(group.getId()));

                    fileConfigs.put(group, effectGroupConfig);
                }
            }
        }

        // Write to configs
        int instancesSaved = 0;
        int failedToSave = 0;
        for (EffectGroupInstance instance : allInstances.values()) {
            PersistentEffectGroup group = instance.getType();

            FileConfiguration effectGroupConfig = fileConfigs.get(group);
            if (effectGroupConfig == null) {
                continue;
            }

            try {
                WbsConfigReader.requireNotNull(effectGroupConfig, "instances", this, groupFiles.get(group.getId()).getName());
            } catch (MissingRequiredKeyException e) {
                failedToSave++;
                continue;
            }

            ConfigurationSection instancesSection = effectGroupConfig.getConfigurationSection("instances");

            if (instancesSection == null) {
                logError("Instances section was invalid.", groupFiles.get(group.getId()).getName());
                failedToSave++;
                continue;
            }

            instance.writeToConfig(instancesSection);
            instancesSaved++;
        }

        // Save configs

        for (PersistentEffectGroup group : allGroups.values()) {
            FileConfiguration effectGroupConfig = fileConfigs.get(group);
            if (fileConfigs.containsKey(group)) {
                try {
                    effectGroupConfig.save(groupFiles.get(group.getId()));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                fileConfigs.remove(group);
            }
        }

        if (instancesSaved != 0 || failedToSave != 0) {
            plugin.logger.info("Saved " + instancesSaved + " instances. Failed to save " + failedToSave + " instances.");
        }
    }

    private void removePendingInstances() {
        if (instancesPendingDeletion.size() == 0) return;

        int deleted = 0;
        for (EffectGroupInstance instance : instancesPendingDeletion.values()) {
            PersistentEffectGroup group = instance.getType();

            FileConfiguration effectGroupConfig = YamlConfiguration.loadConfiguration(groupFiles.get(group.getId()));
            WbsConfigReader.requireNotNull(effectGroupConfig, "instances", this, groupFiles.get(group.getId()).getName());

            ConfigurationSection instancesSection = effectGroupConfig.getConfigurationSection("instances");
            assert instancesSection != null;

            instancesSection.set(instance.getId(), null);

            try {
                effectGroupConfig.save(groupFiles.get(group.getId()));
                deleted++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int skipped = instancesPendingDeletion.size() - deleted;
        instancesPendingDeletion.clear();

        plugin.logger.info("Deleted " + deleted + " instances. Skipped " + skipped + " instances.");
    }

    public boolean restore(String id) {
        EffectGroupInstance deletedInstance = instancesPendingDeletion.get(id);

        if (deletedInstance == null) return false;

        instancesPendingDeletion.remove(id);
        allInstances.put(deletedInstance.getId(), deletedInstance);

        return true;
    }

    public String getFileFor(String id) {
        File file = groupFiles.get(id);
        if (file == null) {
            return null;
        }
        return file.getName();
    }

    public boolean removeInstance(EffectGroupInstance deleteInstance) {
        if (deleteInstance.isLocked()) return false;
        allInstances.remove(deleteInstance.getId());
        deleteInstance.disable();
        instancesPendingDeletion.put(deleteInstance.getId(), deleteInstance);
        return true;
    }

    public Map<String, EffectGroupInstance> getPendingDeletion() {
        return new HashMap<>(instancesPendingDeletion);
    }

    public EffectGroupInstance getPendingDeletion(String id) {
        return instancesPendingDeletion.get(id);
    }

    public Set<EffectGroupInstance> getEffectsWithType(PersistentEffectGroup type) {
        Set<EffectGroupInstance> matches = new HashSet<>();

        for (EffectGroupInstance instance : allInstances.values()) {
            if (instance.getType().equals(type)) {
                matches.add(instance);
            }
        }

        return matches;
    }
}
