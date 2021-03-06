package com.github.shatteredsuite.scrolls.commands;

import com.github.shatteredsuite.scrolls.config.ScrollLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import com.github.shatteredsuite.scrolls.ShatteredScrolls;
import com.github.shatteredsuite.utilities.commands.WrappedCommand;

public class LocationRemoveCommand extends WrappedCommand {

    private final ShatteredScrolls instance;
    private final LocationCommand parent;

    public LocationRemoveCommand(ShatteredScrolls instance, LocationCommand parent) {
        super(
            instance, parent, "remove", "shatteredscrolls.location.remove",
            "location-remove-cmd-help");
        this.instance = instance;
        this.parent = parent;
        addAlias("rem");
        addAlias("r");
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        if (!showHelpOrNoPerms(sender, label, args)) {
            return true;
        }

        if (args.length < 1) {
            HashMap<String, String> msgArgs = new HashMap<>();
            msgArgs.put("label", label);
            instance.getMessenger().sendErrorMessage(sender, "location-remove-cmd-help", msgArgs);
        }

        if (args.length > 1) {
            HashMap<String, String> msgArgs = new HashMap<>();
            msgArgs.put("label", label);
            msgArgs.put("argc", String.valueOf(args.length));
            msgArgs.put("args", Arrays.stream(args).reduce((a, i) -> a + ", " + i).orElse(""));
            instance.getMessenger().sendErrorMessage(sender, "too-many-args", msgArgs);
        }

        ScrollLocation scrollLoc = instance.getLocation(parent.parent.getLocation(args, 0, sender));

        HashMap<String, String> msgArgs = parent.parent.buildArgs(scrollLoc);
        instance.removeLocation(args[0]);

        instance.getMessenger().sendMessage(sender, "location-removed", msgArgs);
        return true;
    }

    @Override
    public List<String> onTabComplete(
        CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length <= 1) {
            StringUtil.copyPartialMatches(args[0], instance.getLocationKeys(), completions);
            Collections.sort(completions);
            return completions;
        } else {
            return Collections.emptyList();
        }
    }
}
