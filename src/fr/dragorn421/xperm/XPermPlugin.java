package fr.dragorn421.xperm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class XPermPlugin extends JavaPlugin implements Listener
{

	private Permission ignorePermission;
	private TreeMap<Integer, List<Permission>> permissions;
	private Map<UUID, PermissionAttachment> attachments;

	@Override
	public void onEnable()
	{
		final PluginManager pm = Bukkit.getPluginManager();
		this.ignorePermission = pm.getPermission("xperm.ignore");
		if(this.ignorePermission == null)
		{
			this.ignorePermission = new Permission("xperm.ignore", PermissionDefault.FALSE);
			pm.addPermission(this.ignorePermission);
		}
		this.loadPermissions();
		this.attachments = new HashMap<>();
		for(final Player p : Bukkit.getOnlinePlayers())
			this.loadPlayerPermissions(p);
		pm.registerEvents(this, this);
		super.getLogger().info(super.getName() + " enabled!");
	}

	@Override
	public void onDisable()
	{
		super.getLogger().info(super.getName() + " disabled!");
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args)
	{
		if(args.length == 0)
			return false;
		switch(args[0].toLowerCase())
		{
		case "reload": {
			this.reload();
			sender.sendMessage("XPerm configuration has been reloaded and applied. Check logs for any error!");
		} break;
		default:
			return false;
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args)
	{
		if(args.length == 1)
			return Arrays.asList("reload");
		return Collections.emptyList();
	}

	@EventHandler
	public void onPlayerLevelChange(final PlayerLevelChangeEvent e)
	{
		final Player p = e.getPlayer();
		if(p.hasPermission(this.ignorePermission))
			return;
		final int oldLevel = e.getOldLevel(), newLevel = e.getNewLevel();
		if(oldLevel == newLevel)
			return;
		final PermissionAttachment pa = this.getAttachment(p);
		boolean changed = false;
		if(oldLevel > newLevel)
		{
			Entry<Integer, List<Permission>> entry = this.permissions.floorEntry(oldLevel);
			while(entry != null && entry.getKey() > newLevel)
			{
				for(final Permission perm : entry.getValue())
				{
					pa.unsetPermission(perm);
					changed = true;
				}
				entry = this.permissions.floorEntry(entry.getKey() - 1);
			}
			// or
/*			for(int level = oldLevel; level > newLevel; level--)
			{
				final List<Permission> permissions = this.permissions.get(level);
				if(permissions != null)
					for(final Permission perm : permissions)
					{
						pa.unsetPermission(perm);
						changed = true;
					}
			}//*/
		}
		else if(newLevel > oldLevel)
		{
			Entry<Integer, List<Permission>> entry = this.permissions.floorEntry(newLevel);
			while(entry != null && entry.getKey() > oldLevel)
			{
				for(final Permission perm : entry.getValue())
				{
					pa.setPermission(perm, true);
					changed = true;
				}
				entry = this.permissions.floorEntry(entry.getKey() - 1);
			}
			// or
/*			for(int level = newLevel; level > oldLevel; level--)
			{
				final List<Permission> permissions = this.permissions.get(level);
				if(permissions != null)
					for(final Permission perm : permissions)
					{
						pa.setPermission(perm, true);
						changed = true;
					}
			}//*/
		}
		if(changed)
			p.recalculatePermissions();
	}

	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e)
	{
		this.loadPlayerPermissions(e.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent e)
	{
		this.removeAttachment(e.getPlayer());
	}

	private void reload()
	{
		this.reloadConfig();
		this.loadPermissions();
		this.clearAttachments();
		for(final Player p : Bukkit.getOnlinePlayers())
			this.loadPlayerPermissions(p);
	}

	private void loadPermissions()
	{
		boolean save = false;
		this.permissions = new TreeMap<>();
		if(!this.getConfig().isConfigurationSection("permissions"))
		{
			final ConfigurationSection cs = this.getConfig().createSection("permissions");
			cs.set("421", Arrays.asList("my.permission.1", "my.permission.2", "and.so.on"));
			cs.set("level-4210", Arrays.asList("my.permission.1.bis", "my.permission.2.bis", "and.so.on.bis"));
			save = true;
		}
		final ConfigurationSection cs = this.getConfig().getConfigurationSection("permissions");
		for(final String key : cs.getKeys(false))
		{
			final int level;
			try {
				final String str;
				if(key.length() > 6 && key.substring(0, 6).equalsIgnoreCase("level-"))
					str = key.substring(6);
				else
					str = key;
				level = Integer.parseInt(str);
			} catch(final NumberFormatException e) {
				this.getLogger().warning("Invalid level " + key + " in configuration: " + e.getMessage());
				continue;
			}
			if(level <= 0)
				this.getLogger().warning("Weird level: " + level);
			List<Permission> levelPerms = this.permissions.get(level);
			if(levelPerms == null)
				levelPerms = new ArrayList<>();
			else
				this.getLogger().warning("Duplicate configuration of level " + level + ", merging both.");
			this.permissions.put(level, levelPerms);
			for(final String permName : cs.getStringList(key))
			{
				Permission perm = Bukkit.getPluginManager().getPermission(permName);
				if(perm == null)
				{
					perm = new Permission(permName, PermissionDefault.FALSE);
					Bukkit.getPluginManager().addPermission(perm);
				}
				if(levelPerms.contains(perm))
					this.getLogger().warning("Duplicate permission for level " + level + ": " + perm.getName());
				else
					levelPerms.add(perm);
			}
		}
		if(save)
			this.saveConfig();
	}

	private void loadPlayerPermissions(final Player player)
	{
		if(!player.hasPermission(this.ignorePermission))
		{
			final PermissionAttachment pa = this.getAttachment(player);
			Entry<Integer, List<Permission>> entry = this.permissions.floorEntry(player.getLevel());
			boolean changed = false;
			while(entry != null)
			{
				for(final Permission perm : entry.getValue())
				{
					pa.setPermission(perm, true);
					changed = true;
				}
				entry = this.permissions.floorEntry(entry.getKey() - 1);
			}
			if(changed)
				player.recalculatePermissions();
		}
	}

	private PermissionAttachment getAttachment(final Player player)
	{
		return this.attachments.computeIfAbsent(player.getUniqueId(), (uuid) -> {
			return player.addAttachment(this);
		});
	}

	private boolean removeAttachment(final Player player)
	{
		final PermissionAttachment pa = this.attachments.remove(player.getUniqueId());
		if(pa != null)
		{
			player.removeAttachment(pa);
			return true;
		}
		return false;
	}

	private void clearAttachments()
	{
		for(final Entry<UUID, PermissionAttachment> e : this.attachments.entrySet())
		{
			final Player p = Bukkit.getPlayer(e.getKey());
			if(p != null)
			{
				p.removeAttachment(e.getValue());
				p.recalculatePermissions();
			}
		}
		this.attachments.clear();
	}

}
