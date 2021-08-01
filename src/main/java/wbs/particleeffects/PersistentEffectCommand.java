package wbs.particleeffects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import wbs.particleeffects.instance.AmbientEffectGroupInstance;
import wbs.particleeffects.instance.EffectGroupInstance;
import wbs.utils.util.plugin.WbsMessenger;

import java.util.*;

public class PersistentEffectCommand extends WbsMessenger implements CommandExecutor, TabCompleter {

    private final String PERMISSION = "wbspe.command";

    private final WbsParticleEffects plugin;
    public PersistentEffectCommand(WbsParticleEffects plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        int length = args.length;

        if (length == 0) {
            sendMessage("Usage: &h/" + label + " <toggle|enable|disable|reload>", sender);
            return true;
        }

        List<String> errors;

        if (checkPermission(sender, PERMISSION)) {
            switch (args[0].toLowerCase()) {
                case "toggle":
                case "enable":
                case "disable":
                    if (checkPermission(sender, PERMISSION + ".toggle")) {
                        if (length == 1) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " <instance name>", sender);
                        } else if (length == 2) {
                            EffectGroupInstance instance = plugin.settings.getEffectGroupInstance(args[1]);
                            if (instance == null) {
                                sendMessage("Instance not found: &w" + args[1], sender);
                                return true;
                            }

                            switch (args[0].toLowerCase()) {
                                case "toggle":
                                    if (instance.toggle()) {
                                        sendMessage(instance.getId() + " is now &henabled.", sender);
                                    } else {
                                        sendMessage(instance.getId() + " is now &wdisabled.", sender);
                                    }
                                    break;
                                case "enable":
                                    if (instance.enable()) {
                                        sendMessage(instance.getId() + " is now &henabled.", sender);
                                    } else {
                                        sendMessage("&w" + instance.getId() + " was already enabled.", sender);
                                    }
                                    break;
                                case "disable":
                                    if (instance.disable()) {
                                        sendMessage(instance.getId() + " is now &wdisabled.", sender);
                                    } else {
                                        sendMessage("&w" + instance.getId() + " was already disabled.", sender);
                                    }
                                    break;
                                default:
                                    return true; // Shouldn't be possible to reach this
                            }
                        } else {
                            sendMessage("Too many args. Usage: &h/" + label + " " + args[0] + " <effect group name>", sender);
                        }
                    }
                    return true;

                case "teleport":
                case "tp":
                    if (checkPermission(sender, PERMISSION + ".tp")) {
                        if (length == 1) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " <instance>", sender);
                            return true;
                        }

                        EffectGroupInstance instance = plugin.settings.getEffectGroupInstance(args[1]);
                        if (instance == null) {
                            sendMessage("Instance not found: &w" + args[1], sender);
                            return true;
                        }

                        instance.teleportPlayer((Player) sender);
                        sendMessage("Teleported to &h" + instance.getId(), sender);
                    }
                    return true;

                case "redefine":
                case "define":
                case "outline":
                    if (!(sender instanceof Player)) {
                        sendMessage("This command is only usable by players", sender);
                        return true;
                    }

                    if (checkPermission(sender, PERMISSION + ".highlight")) {
                        if (length == 1) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " <instance>", sender);
                            return true;
                        }

                        EffectGroupInstance instance = plugin.settings.getEffectGroupInstance(args[1]);
                        if (instance == null) {
                            sendMessage("Instance not found: &w" + args[1], sender);
                            return true;
                        }

                        if (!(instance instanceof AmbientEffectGroupInstance)) {
                            sendMessage("&w" + args[1] + "&r is not an ambient instance.", sender);
                            return true;
                        }

                        Player player = (Player) sender;
                        PlayerData data = new PlayerData(player);

                        // TODO: Integrate with world edit for region selection?
                        data.selectingFor = (AmbientEffectGroupInstance) instance;
                        data.pos1 = null;
                        player.getInventory().addItem(new ItemStack(Material.LAPIS_BLOCK));
                        sendMessage("Place a Lapis Block to select the first corner of the area.", sender);
                    }
                    return true;
                case "highlight":
                    if (checkPermission(sender, PERMISSION + ".highlight")) {
                        if (length == 1) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " <instance>", sender);
                            return true;
                        }

                        EffectGroupInstance instance = plugin.settings.getEffectGroupInstance(args[1]);
                        if (instance == null) {
                            sendMessage("Instance not found: &w" + args[1], sender);
                            return true;
                        }

                        if (!(instance instanceof AmbientEffectGroupInstance)) {
                            sendMessage(args[1] + " is not an ambient instance.", sender);
                            return true;
                        }

                        boolean enabled = ((AmbientEffectGroupInstance) instance).highlightFor((Player) sender, plugin);

                        if (enabled) {
                            sendMessage(args[1] + " has been highlighted. Repeat this command to hide it.", sender);
                        } else {
                            sendMessage(args[1] + " is no longer highlighted.", sender);
                        }

                    }
                    return true;
                case "create":
                    if (checkPermission(sender, PERMISSION + ".create")) {
                        if (length == 1) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " <group> <name>", sender);
                            return true;
                        }

                        String groupString = args[1];
                        PersistentEffectGroup group = plugin.settings.getEffectGroup(groupString);
                        if (group == null) {
                            sendMessage("&w" + groupString + " is not a valid group.", sender);
                            return true;
                        }
                        if (length == 2) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " " + args[1] + " <name>", sender);
                            return true;
                        }

                        String name = args[2];

                        boolean ambient = false;
                        if (length >= 4) {
                            ambient = Boolean.parseBoolean(args[3]);
                        }
                        boolean success;

                        Location playerLoc = ((Player) sender).getLocation();

                        if (ambient) {
                            success = plugin.settings.createNewInstance(group, name, playerLoc.clone().subtract(5, 5, 5), playerLoc.clone().add(5, 5, 5), true);
                        } else {
                            success = plugin.settings.createNewInstance(group, name, playerLoc, true);
                        }

                        if (success) {
                            sendMessage("Instance created successfully!", sender);
                        } else {
                            sendMessage("&wInstance could not be created. Do &7/" + label + " errors&w to see why.", sender);
                        }
                    }
                    return true;
                case "remove":
                case "delete":
                    if (checkPermission(sender, PERMISSION + ".delete")) {
                        if (length == 1) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " <instance>", sender);
                            return true;
                        }

                        String deletionString = args[1];
                        EffectGroupInstance deleteInstance = plugin.settings.getEffectGroupInstance(deletionString);
                        if (deleteInstance == null) {
                            sendMessage("&w" + args[1] + " is not a valid instance. Do &7/" + label + " list&w for a list.", sender);
                            return true;
                        }
                        if (deleteInstance.isLocked()) {
                            sendMessage("This effect is locked. To delete it, unlock it in &h" +
                            plugin.settings.getFileFor(deleteInstance.getType().getId()), sender);
                            return true;
                        }

                        plugin.settings.removeInstance(deleteInstance);
                        sendMessage("&h" + deleteInstance.getId() + "&r will be deleted on the next save or restart. To restore it before then, do &h/" + label + " restore " + deletionString + "&r.", sender);
                    }
                    return true;
                case "restore":
                    if (checkPermission(sender, PERMISSION + ".delete")) {
                        if (length == 1) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " <instance>", sender);
                            return true;
                        }

                        String deletionString = args[1];
                        EffectGroupInstance deleteInstance = plugin.settings.getPendingDeletion(deletionString);
                        if (deleteInstance == null) {
                            sendMessage("&w" + args[1] + " has not been marked for deletion.", sender);
                            return true;
                        }
                        plugin.settings.restore(deletionString);
                        sendMessage("&h" + deleteInstance.getId() + "&r has been restored! Do &h/" + label + " toggle&r to enable it.", sender);
                    }
                    return true;
                case "movehere":
                case "move":
                    if (checkPermission(sender, PERMISSION + ".move")) {
                        if (length == 1) {
                            sendMessage("Usage: &h/" + label + " " + args[0] + " <instance>", sender);
                            return true;
                        }

                        EffectGroupInstance moveInstance = plugin.settings.getEffectGroupInstance(args[1]);
                        if (moveInstance == null) {
                            sendMessage("&w" + args[1] + " is not a valid instance. Do &7/" + label + " list&w for a list.", sender);
                            return true;
                        }
                        if (moveInstance.isLocked()) {
                            sendMessage("This effect is locked. To move it, unlock it in &h" +
                                    plugin.settings.getFileFor(moveInstance.getType().getId()), sender);
                        } else {
                            moveInstance.move(((Player)sender).getLocation());
                            sendMessage("&h" + moveInstance.getId() + "&r was moved to your location!", sender);
                        }
                    }
                    return true;
                case "listgroup":
                case "listgroups":
                    if (checkPermission(sender, PERMISSION + ".list")) {
                        String listString = String.join(", ", plugin.settings.getAllGroups().keySet());

                        sendMessage("Groups: &h" + listString, sender);
                    }
                    return true;
                case "list":
                    if (checkPermission(sender, PERMISSION + ".list")) {
                        StringBuilder listString = new StringBuilder();
                        for (EffectGroupInstance instance : plugin.settings.getAllInstances().values()) {
                            if (instance.isActive()) {
                                listString.append("&a").append(instance.getId());
                            } else {
                                listString.append("&c").append(instance.getId());
                            }
                            listString.append(", ");
                        }
                        listString = new StringBuilder(listString.substring(0, listString.length() - 2));

                        sendMessage("Instances: &h" + listString, sender);
                    }
                    return true;
                case "save":
                    if (checkPermission(sender, PERMISSION + ".save")) {
                        plugin.settings.saveAllInstances(false);

                        sendMessage("&hSaved changes!", sender);
                    }
                    return true;
                case "reload":
                    if (checkPermission(sender, PERMISSION + ".reload")) {

                        plugin.settings.reload();

                        errors = plugin.settings.getErrors();
                        if (errors.isEmpty()) {
                            sendMessage("&hReload successful!", sender);
                            return true;
                        } else {
                            sendMessage("&wThere were " + errors.size() + " config errors. Do &7/" + label + " errors&w to see them.", sender);
                        }
                    }
                    return true;
                case "errors":
                    if (checkPermission(sender, PERMISSION + ".reload")) {
                        errors = plugin.settings.getErrors();
                        if (errors.isEmpty()) {
                            sendMessage("&hThere were no errors in the last reload.", sender);
                            return true;
                        }
                        int page = 1;
                        if (args.length > 1) {
                            try {
                                page = Integer.parseInt(args[1]);
                            } catch (NumberFormatException e) {
                                sendMessage("Usage: &h/" + label + " errors [page]", sender);
                                return true;
                            }
                        }
                        page--;
                        int ENTRIES_PER_PAGE = 5;
                        int pages = errors.size() / ENTRIES_PER_PAGE;
                        if (errors.size() % ENTRIES_PER_PAGE != 0) {
                            pages++;
                        }

                        int index = 1;

                        sendMessage("Displaying page " + (page + 1) + "/" + pages + ":", sender);
                        for (String error : errors) {
                            if (index > page * ENTRIES_PER_PAGE && index <= (page + 1) * (ENTRIES_PER_PAGE)) {
                                sendMessage("&6" + index + ") " + error, sender);
                            }
                            index++;
                        }
                    }

                    return true;
                default:
                    sendMessage("Usage: &h/" + label + " <toggle|enable|disable|reload>", sender);
                    return true;
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> choices = new LinkedList<>();

        if (sender.hasPermission(PERMISSION)) {
            switch (args.length) {
                case 1:
                    if (sender.hasPermission(PERMISSION + ".toggle")) {
                        choices.add("toggle");
                        choices.add("enable");
                        choices.add("disable");
                    }
                    if (sender.hasPermission(PERMISSION + ".reload")) {
                        choices.add("reload");
                        choices.add("errors");
                    }
                    if (sender.hasPermission(PERMISSION + ".list")) {
                        choices.add("list");
                        choices.add("listgroups");
                    }
                    if (sender.hasPermission(PERMISSION + ".create")) {
                        choices.add("create");
                    }
                    if (sender.hasPermission(PERMISSION + ".delete")) {
                        choices.add("delete");
                        choices.add("restore");
                    }
                    if (sender.hasPermission(PERMISSION + ".move")) {
                        choices.add("movehere");
                    }
                    if (sender.hasPermission(PERMISSION + ".highlight")) {
                        choices.add("highlight");
                    }
                    if (sender.hasPermission(PERMISSION + ".tp")) {
                        choices.add("tp");
                    }
                    break;
                case 2:
                    switch (args[0].toLowerCase())  {
                        case "toggle":
                        case "enable":
                        case "disable":
                            if (sender.hasPermission(PERMISSION + ".toggle")) {
                                choices.addAll(plugin.settings.getAllInstances().keySet());
                            } else {
                                return choices;
                            }
                            break;
                        case "teleport":
                        case "tp":
                            if (sender.hasPermission(PERMISSION + ".tp")) {
                                choices.addAll(plugin.settings.getAllInstances().keySet());
                            } else {
                                return choices;
                            }
                            break;
                        case "highlight":
                            if (sender.hasPermission(PERMISSION + ".highlight")) {
                                for (EffectGroupInstance instance : plugin.settings.getAllInstances().values()) {
                                    if (instance instanceof AmbientEffectGroupInstance) {
                                        choices.add(instance.getId());
                                    }
                                }
                            } else {
                                return choices;
                            }
                            break;
                        case "list":
                        case "listgroup":
                        case "listgroups":
                        case "reload":
                            return choices;
                        case "create":
                            if (sender.hasPermission(PERMISSION + ".create")) {
                                choices.addAll(plugin.settings.getAllGroups().keySet());
                            }
                            break;
                        case "remove":
                        case "delete":
                            if (sender.hasPermission(PERMISSION + ".delete")) {
                                choices.addAll(plugin.settings.getAllInstances().keySet());
                            }
                            break;
                        case "restore":
                            if (sender.hasPermission(PERMISSION + ".delete")) {
                                choices.addAll(plugin.settings.getPendingDeletion().keySet());
                            }
                            break;
                        case "move":
                        case "movehere":
                            if (sender.hasPermission(PERMISSION + ".move")) {
                                choices.addAll(plugin.settings.getAllInstances().keySet());
                            }
                    }
                    break;
                default:
                    return choices;
            }
        }

        List<String> result = new ArrayList<>();
        for (String add : choices) {
            if (add.toLowerCase().startsWith(args[args.length-1].toLowerCase())) {
                result.add(add);
            }
        }

        return result;
    }
}
