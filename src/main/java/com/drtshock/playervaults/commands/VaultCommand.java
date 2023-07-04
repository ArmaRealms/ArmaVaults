/*
 * PlayerVaultsX
 * Copyright (C) 2013 Trent Hensler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.drtshock.playervaults.commands;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.vaultmanagement.VaultManager;
import com.drtshock.playervaults.vaultmanagement.VaultOperations;
import com.drtshock.playervaults.vaultmanagement.VaultViewInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VaultCommand implements CommandExecutor, TabCompleter {
    private final PlayerVaults plugin;

    public VaultCommand(PlayerVaults plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (VaultOperations.isLocked()) {
            this.plugin.getTL().locked().title().send(sender);
            return true;
        }

        if (sender instanceof Player player) {
            if (PlayerVaults.getInstance().getInVault().containsKey(player.getUniqueId().toString())) {
                // don't let them open another vault.
                return true;
            }

            switch (args.length) {
                case 1 -> {
                    if (VaultOperations.openOwnVault(player, args[0], true)) {
                        PlayerVaults.getInstance().getInVault().put(player.getUniqueId().toString(), new VaultViewInfo(player.getUniqueId().toString(), Integer.parseInt(args[0])));
                    } else if (sender.hasPermission("playervaults.admin")) {
                        OfflinePlayer searchPlayer = Bukkit.getOfflinePlayer(args[0]);
                        String target = args[0];
                        if (searchPlayer != null) {
                            target = searchPlayer.getUniqueId().toString();
                        }

                        YamlConfiguration file = VaultManager.getInstance().getPlayerVaultFile(target, false);
                        if (file == null) {
                            this.plugin.getTL().vaultDoesNotExist().title().send(sender);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (String key : file.getKeys(false)) {
                                sb.append(key.replace("vault", "")).append(" ");
                            }

                            this.plugin.getTL().existingVaults().title().with("player", args[0]).with("vault", sb.toString().trim()).send(sender);
                        }
                    }
                }
                case 2 -> {
                    if (!player.hasPermission("playervaults.admin")) {
                        this.plugin.getTL().noPerms().title().send(sender);
                        break;
                    }
                    int number;
                    try {
                        number = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        this.plugin.getTL().mustBeNumber().title().send(sender);
                        return true;
                    }
                    String target = args[0];
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                    if (offlinePlayer != null) {
                        target = offlinePlayer.getUniqueId().toString();
                    }
                    if (VaultOperations.openOtherVault(player, target, args[1])) {
                        PlayerVaults.getInstance().getInVault().put(player.getUniqueId().toString(), new VaultViewInfo(target, number));
                    } else {
                        this.plugin.getTL().noOwnerFound().title().with("player", args[0]).send(sender);
                    }
                }
                default -> this.plugin.getTL().help().title().send(sender);
            }
        } else {
            this.plugin.getTL().playerOnly().title().send(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        final List<String> completions = new ArrayList<>();

        final List<String> perms = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            if (commandSender.hasPermission("playervaults.amount." + i)) {
                perms.add(String.valueOf(i));
            }
        }

        final Iterable<String> commands = new ArrayList<>(perms);
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], commands, completions);
            return completions;
        } else {
            return Collections.emptyList();
        }
    }
}
