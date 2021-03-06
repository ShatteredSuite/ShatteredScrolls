package com.github.shatteredsuite.scrolls;

import com.github.shatteredsuite.scrolls.commands.BaseCommand;
import com.github.shatteredsuite.scrolls.config.ScrollConfig;
import com.github.shatteredsuite.scrolls.config.ScrollCost;
import com.github.shatteredsuite.scrolls.config.ScrollLocation;
import com.github.shatteredsuite.scrolls.config.ScrollRecipe;
import com.github.shatteredsuite.scrolls.listeners.InteractListener;
import com.github.shatteredsuite.scrolls.recipes.RecipeHandler;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.shatteredsuite.utilities.MaterialUtil;
import com.github.shatteredsuite.utilities.cooldowns.CooldownManager;
import com.github.shatteredsuite.utilities.messages.Messageable;
import com.github.shatteredsuite.utilities.messages.Messages;
import com.github.shatteredsuite.utilities.messages.Messenger;
import com.github.shatteredsuite.utilities.nms.MaterialUtil12;
import com.github.shatteredsuite.utilities.nms.MaterialUtil13;
import com.github.shatteredsuite.utilities.nms.MaterialUtil14;
import com.github.shatteredsuite.utilities.nms.MaterialUtil15;
import com.github.shatteredsuite.utilities.nms.NBTUtil;
import com.github.shatteredsuite.utilities.nms.NBTUtil12;
import com.github.shatteredsuite.utilities.nms.NBTUtil13;
import com.github.shatteredsuite.utilities.nms.NBTUtil14;
import com.github.shatteredsuite.utilities.nms.NBTUtil15;

public class ShatteredScrolls extends JavaPlugin implements Messageable {

    private static ShatteredScrolls instance;
    private Messenger messenger;
    private NBTUtil nbtUtil;
    private MaterialUtil materialUtil;
    private CooldownManager teleportCooldownManager;
    private ScrollConfig config;
    private HashMap<String, ScrollLocation> locations;

    public static ShatteredScrolls getInstance() {
        return instance;
    }

    public NBTUtil getNbtUtil() {
        return nbtUtil;
    }

    public MaterialUtil getMaterialUtil() {
        return materialUtil;
    }

    public void reload() {
        onDisable();
        onEnable();
    }

    @Override
    public void onEnable() {
        instance = this;

        ConfigurationSerialization.registerClass(ScrollConfig.class);
        ConfigurationSerialization.registerClass(ScrollLocation.class);
        ConfigurationSerialization.registerClass(ScrollCost.class);
        ConfigurationSerialization.registerClass(ScrollRecipe.class);

        loadVersionSpecific();
        loadMessages();
        loadConfig();
        loadLocations();

        PluginCommand cmd = getCommand("scrolls");
        if (cmd != null) {
            BaseCommand command = new BaseCommand(this);
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        teleportCooldownManager = new CooldownManager(config.getCooldown());

        PluginCommand command = getCommand("scroll");
        if (command != null) {
            command.setExecutor(new BaseCommand(this));
            command.setTabCompleter(new BaseCommand(this));
        }

        getServer().getPluginManager().registerEvents(new InteractListener(this), this);

        if(config.doesAllowCrafting()) {
            RecipeHandler.addRecipe(this);
        }
        getServer()
            .getScheduler()
            .runTaskTimerAsynchronously(this, this::save, 20 * 60 * 10, 20 * 60 * 10);

        Metrics metrics = new Metrics(this);
    }

    private void save() {
        saveLocations();
    }

    private void loadLocations() {
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        File locationsFile = new File(getDataFolder(), "locations.yml");

        locations = new HashMap<>();
        if (!locationsFile.exists()) {
            return;
        }
        YamlConfiguration locationConfig = YamlConfiguration.loadConfiguration(locationsFile);
        ConfigurationSection section = locationConfig.getConfigurationSection("locations");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            locations.put(id, (ScrollLocation) section.get(id));
        }
    }

    private void loadMessages() {
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        File messagesFile = new File(getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        Messages messages = new Messages(this, YamlConfiguration.loadConfiguration(messagesFile));
        messenger = new Messenger(this, messages);
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        this.config = (ScrollConfig) YamlConfiguration.loadConfiguration(configFile).get("config");
    }

    private void loadVersionSpecific() {
        if (Bukkit.getVersion().contains("1.12.2")) {
            getLogger().info("Loaded compatibility for 1.12.");
            nbtUtil = new NBTUtil12();
            materialUtil = new MaterialUtil12();
        } else if (Bukkit.getVersion().contains("1.13.1") || Bukkit.getVersion()
            .contains("1.13.2")) {
            getLogger().info("Loaded compatibility for 1.13.");
            nbtUtil = new NBTUtil13();
            materialUtil = new MaterialUtil13();
        } else if (Bukkit.getVersion().contains("1.14")) {
            getLogger().info("Loaded compatibility for 1.14.");
            nbtUtil = new NBTUtil14();
            materialUtil = new MaterialUtil14();
        } else if (Bukkit.getVersion().contains("1.15")) {
            getLogger().info("Loaded compatibility for 1.15.");
            nbtUtil = new NBTUtil15();
            materialUtil = new MaterialUtil15();
        } else {
            getLogger().severe("Could not load compatibility for this version. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        saveLocations();
    }

    private void saveLocations() {
        File locationsFile = new File(getDataFolder(), "locations.yml");
        YamlConfiguration locationConfig = YamlConfiguration.loadConfiguration(locationsFile);
        for (ScrollLocation location : locations.values()) {
            locationConfig.set("locations." + location.getId(), location);
        }
        try {
            locationConfig.save(locationsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Messenger getMessenger() {
        return messenger;
    }

    public ScrollConfig config() {
        return this.config;
    }

    public boolean canTeleport(Player player) {
        return teleportCooldownManager.canUse(player.getUniqueId());
    }

    public void doCooldown(Player player) {
        teleportCooldownManager.use(player.getUniqueId());
    }

    public long cooldownLength(Player player) {
        return teleportCooldownManager.timeUntilUse(player.getUniqueId());
    }

    public ScrollLocation getLocation(String destination) {
        return locations.get(destination);
    }

    public boolean hasLocation(String destination) {
        return locations.containsKey(destination);
    }

    public Collection<ScrollLocation> getLocations() {
        return locations.values();
    }

    public Collection<String> getLocationKeys() {
        return locations.keySet();
    }

    public void removeLocation(String location) {
        locations.remove(location);
    }

    public void addLocation(ScrollLocation loc) {
        locations.put(loc.getId(), loc);
    }
}
