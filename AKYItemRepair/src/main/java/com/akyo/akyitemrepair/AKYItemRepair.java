package com.akyo.akyitemrepair;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AKYItemRepair extends JavaPlugin implements CommandExecutor {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private FileConfiguration config;
    private String prefix;
    private final Map<String, Long> permissionCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();
        this.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix", "&c[AKYItemRepair] "));

        for (String key : config.getConfigurationSection("permissions").getKeys(false)) {
            long cooldown = config.getLong("permissions." + key);
            permissionCooldowns.put(key, cooldown);
        }

        this.getCommand("akyrepair").setExecutor(this);
        this.getCommand("arepair").setExecutor(this);
        this.getCommand("repair").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownTime = getCooldownMillis(player);

        if (!player.hasPermission("akyitemrepair.bypass")) {
            if (cooldowns.containsKey(uuid)) {
                long lastUsed = cooldowns.get(uuid);
                long timeLeft = cooldownTime - (now - lastUsed);
                if (timeLeft > 0) {
                    player.sendMessage(prefix + "You must wait " + formatTime(timeLeft) + " before using this again.");
                    return true;
                }
            }
            cooldowns.put(uuid, now);
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(prefix + "You must hold an item to repair.");
            return true;
        }

        item.setDurability((short) 0); // deprecated, but still functional for legacy
        item.setItemMeta(item.getItemMeta());
        player.sendMessage(prefix + "Item has been repaired!");
        return true;
    }

    private long getCooldownMillis(Player player) {
        for (Map.Entry<String, Long> entry : permissionCooldowns.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue() * 1000L;
            }
        }
        return 3600 * 1000L; // Default 1 hour in milliseconds
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return minutes + "m " + seconds + "s";
    }
}
