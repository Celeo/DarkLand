package com.darktidegames.celeo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.darktidegames.celeo.clans.Clan;
import com.darktidegames.celeo.clans.DarkClans;
import com.darktidegames.empyrean.C;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * DarkLand
 * 
 * @author Celeo, epuidokas
 */
public class DarkLand extends JavaPlugin
{

	List<Player> placing = new ArrayList<Player>();
	WorldGuardPlugin wg;
	DarkClans clans;
	Map<String, Integer> unclaimed = new HashMap<String, Integer>();
	List<SubRegionCreator> creators = new ArrayList<SubRegionCreator>();
	int subRegionPriority = 1;

	@Override
	public void onLoad()
	{
		getDataFolder().mkdirs();
		if (!new File(getDataFolder(), "config.yml").exists())
			saveDefaultConfig();
		getLogger().info("Pre-enable setup finished");
	}

	@Override
	public void onEnable()
	{
		getDataFolder().mkdirs();
		Plugin t = getServer().getPluginManager().getPlugin("WorldGuard");
		if (t != null)
			wg = (WorldGuardPlugin) t;
		t = getServer().getPluginManager().getPlugin("DarkClans");
		if (t != null)
			clans = (DarkClans) t;
		else
			getLogger().info("Not connected to DarkClans");
		load();
		getServer().getPluginManager().registerEvents(new LOListener(this), this);
		getServer().getPluginManager().registerEvents(new SubRegionListener(this), this);
		getLogger().info("Enabled");
	}

	@Override
	public void onDisable()
	{
		save();
		getLogger().info("Disabled");
	}

	private void save()
	{
		List<String> ret = new ArrayList<String>();
		for (String key : unclaimed.keySet())
			ret.add(key + "-" + unclaimed.get(key));
		getConfig().set("unclaimed", ret);
		getConfig().set("subRegionPriority", Integer.valueOf(subRegionPriority));
		saveConfig();
	}

	private void load()
	{
		reloadConfig();
		subRegionPriority = getConfig().getInt("subRegionPriority", 1);
		for (String str : getConfig().getStringList("unclaimed"))
			unclaimed.put(str.split("-")[0], Integer.valueOf(str.split("-")[1]));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!(sender instanceof Player))
		{
			if (args != null && args.length >= 2
					&& args[0].equalsIgnoreCase("increase"))
			{
				int count = 1;
				if (args.length == 3)
					count = C.i(args[2]);
				unclaimed.put(args[1], Integer.valueOf(getUnclaimedLandOwners(args[1])
						+ count));
				getLogger().info("Unclaimed plots for " + args[1]
						+ " increased by " + count + " to "
						+ getUnclaimedLandOwners(args[1])
						+ " by server command");
			}
			return true;
		}
		Player player = (Player) sender;
		if (args == null || args.length == 0)
		{
			player.sendMessage("§7You have §6"
					+ getUnclaimedLandOwners(player.getName())
					+ " §7unclaimed land plots");
			return false;
		}
		if (args[0].equalsIgnoreCase("claim"))
		{
			if (isPlacing(player.getName()))
			{
				player.sendMessage("§7You were already placing your land. §cIt has been cancelled. §a/darkland claim §7to start the process again.");
				removePlacing(player.getName());
			}
			else
			{
				placing.add(player);
				player.sendMessage("§7Now you need to §aright-click §7the §acenterpoint §7for your new land plot. §b/darkland cancel §7to cancel");
			}
			return true;
		}
		else if (args[0].equalsIgnoreCase("cancel"))
		{
			removePlacing(player.getName());
			player.sendMessage("§7The placement of your land has been §ccancelled");
			return true;
		}
		else if (args[0].equalsIgnoreCase("-unclaimed"))
		{
			if (!noPerms(player, "darkland.mod"))
				return true;
			if (args.length < 2)
			{
				player.sendMessage("§c/darkland -unclaimed [who]");
				return true;
			}
			else if (args.length == 2)
			{
				player.sendMessage("§7Unclaimed land plots for §6" + args[1]
						+ "§7: §6" + getUnclaimedLandOwners(args[1]));
			}
			else if (args.length == 3)
			{
				if (!noPerms(player, "darkland.admin"))
					return true;
				unclaimed.put(args[1], Integer.valueOf(C.i(args[2])));
				player.sendMessage("§7Unclaimed plots for §6" + args[1]
						+ " §7set to §6" + args[2]);
			}
			else
				player.sendMessage("§/darkland -unclaimed [who] (optional # to set)");
			return true;
		}
		else if (args[0].equalsIgnoreCase("-reload"))
		{
			if (!player.hasPermission("darkland.admin"))
				return true;
			load();
			player.sendMessage("§aReloaded from configuration");
		}
		else if (args[0].equalsIgnoreCase("sr")
				|| args[0].equalsIgnoreCase("subregion"))
		{
			if (args.length < 2)
			{
				player.sendMessage("§e/darkland sr [start|cancel] <name of new subregion>");
				return true;
			}
			if (args[1].equalsIgnoreCase("start") && args.length == 3)
			{
				creators.add(new SubRegionCreator(player, args[2]));
				player.sendMessage("§bClick the first corner of the new sub region, §9"
						+ args[2]);
			}
			else if (args[1].equalsIgnoreCase("cancel"))
				player.sendMessage(creators.remove(getCreator(player)) ? "§aSub region creation cancelled" : "§cYou did not have any pending sub region creation placements");
			else
				player.sendMessage("§e/darkland sr [start|cancel] <name of new subregion>");
			return true;
		}
		return false;
	}

	private static boolean noPerms(Player player, String node)
	{
		if (!player.hasPermission(node))
		{
			player.sendMessage("§cYou cannot use that command");
			return false;
		}
		return true;
	}

	public boolean isPlacing(String name)
	{
		for (Player player : placing)
			if (player.getName().equals(name))
				return true;
		return false;
	}

	public void removePlacing(String name)
	{
		Iterator<Player> i = placing.iterator();
		while (i.hasNext())
			if (i.next().getName().equals(name))
				i.remove();
	}

	public int getUnclaimedLandOwners(String name)
	{
		if (unclaimed.containsKey(name))
			return unclaimed.get(name).intValue();
		unclaimed.put(name, Integer.valueOf(0));
		return unclaimed.get(name).intValue();
	}

	/**
	 * @author epuidokas (modified)
	 */
	public void handlePlacement(Player player, Location location)
	{
		// check if they have spare lot placements
		removePlacing(player.getName());
		if (getUnclaimedLandOwners(player.getName()) == 0)
		{
			player.sendMessage("§cYou do not have any plots to place!");
			return;
		}
		
		if (!location.getWorld().getName().equalsIgnoreCase("world"))
		{
			player.sendMessage("§cYou can only place subregions in the main world.");
			return;
		}

		// check for other regions in the area
		if (!isValidRegionPlacement(location, 35, player))
		{
			player.sendMessage("§cThat is an invalid placement location!");
			return;
		}

		// place that plot!
		try
		{
			World world = player.getWorld();
			RegionManager regionManager = wg.getRegionManager(world);
			BukkitPlayer wgPlayer = new BukkitPlayer(wg, player);
			ProtectedRegion actual = newRegion(location, Integer.valueOf(25), getNextRegionIdFor(player.getName()));
			DefaultDomain owners = new DefaultDomain();
			owners.addPlayer(wgPlayer);
			actual.setOwners(owners);
			actual.setFlag(DefaultFlag.CHEST_ACCESS, DefaultFlag.CHEST_ACCESS.parseInput(wg, player, "allow"));
			actual.setFlag(DefaultFlag.PVP, DefaultFlag.PVP.parseInput(wg, player, "allow"));
			actual.setFlag(DefaultFlag.MOB_SPAWNING, DefaultFlag.MOB_SPAWNING.parseInput(wg, player, "deny"));
			actual.setFlag(DefaultFlag.CREEPER_EXPLOSION, DefaultFlag.CREEPER_EXPLOSION.parseInput(wg, player, "deny"));
			actual.setFlag(DefaultFlag.OTHER_EXPLOSION, DefaultFlag.OTHER_EXPLOSION.parseInput(wg, player, "deny"));
			actual.setFlag(DefaultFlag.TNT, DefaultFlag.TNT.parseInput(wg, player, "deny"));
			regionManager.addRegion(actual);
			regionManager.save();
		}
		catch (Exception e)
		{
			player.sendMessage("§cAn error occurred! Your landowner has not been placed, nor have you been charged for it.");
			e.printStackTrace();
			return;
		}
		// decrease their plot placement amount
		int currentCount = getUnclaimedLandOwners(player.getName());
		unclaimed.put(player.getName(), Integer.valueOf(currentCount - 1));
		player.sendMessage("§aLand placed successfully! Enjoy!");
	}

	private String getNextRegionIdFor(String player)
	{
		player = player.toLowerCase();
		RegionManager rm = wg.getRegionManager(getServer().getWorld("world"));
		List<ProtectedRegion> regions = new ArrayList<ProtectedRegion>(rm.getRegions().values());
		int highest = 0;
		for (ProtectedRegion r : regions)
		{
			if (r.getId().startsWith(player) && r.getId().contains("_"))
			{
				if (C.i(r.getId().split("_")[1]) > highest)
				{
					highest = C.i(r.getId().split("_")[1]);
				}
			}
		}
		if (highest == 0)
		{
			return player + "_1";
		}
		return player + "_" + (highest + 1);
	}

	/**
	 * @author epuidokas (modified)
	 */
	private boolean isValidRegionPlacement(Location loc, int size, Player player)
	{
		try
		{
			/*
			 * Other player regions
			 */
			ProtectedRegion region = newRegion(loc, Integer.valueOf(size));
			RegionManager regionmanager = wg.getRegionManager(loc.getWorld());
			List<ProtectedRegion> intersectingRegions = region.getIntersectingRegions(new ArrayList<ProtectedRegion>(regionmanager.getRegions().values()));
			for (ProtectedRegion intersectingRegion : intersectingRegions)
			{
				if (!intersectingRegion.isMember(player.getName())
						&& (intersectingRegion.getPriority() >= 0))
				{
					player.sendMessage("§cYou are too close to the '"
							+ intersectingRegion.getId()
							+ "' region, which you are not a member of.");
					return false;
				}
			}
			/*
			 * Faction land
			 */
			double x = loc.getX();
			double z = loc.getZ();
			if (x > 575 || x < -575 || z > 575 || z < -575)
			{
				player.sendMessage("§cYou are too close to faction land. Cannot place past 575 in any direction.");
				return false;
			}
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * @author epuidokas (modified)
	 */
	private static ProtectedRegion newRegion(Location loc, Integer size, String name)
	{
		Double x = Double.valueOf(loc.getX());
		Double y = Double.valueOf(loc.getY());
		Double z = Double.valueOf(loc.getZ());
		Integer halfSize = Integer.valueOf((size.intValue() - 1) / 2);
		BlockVector p1 = new BlockVector(x.doubleValue() - halfSize.intValue(), y.doubleValue() - 24, z.doubleValue()
				- halfSize.intValue());
		BlockVector p2 = new BlockVector(x.doubleValue() + halfSize.intValue(), y.doubleValue() + 24, z.doubleValue()
				+ halfSize.intValue());
		return new ProtectedCuboidRegion(name, p1, p2);
	}

	private static ProtectedRegion newRegion(Location loc, Integer size)
	{
		return newRegion(loc, size, "temp_" + new Random().nextInt(10000));
	}

	public WorldGuardPlugin getWorldGuard()
	{
		return wg;
	}

	public boolean isCreatingSubRegion(Player player)
	{
		for (SubRegionCreator sbc : creators)
			if (sbc.player.getName().equals(player.getName()))
				return true;
		return false;
	}

	public SubRegionCreator getCreator(Player player)
	{
		for (SubRegionCreator sbc : creators)
			if (sbc.player.getName().equals(player.getName()))
				return sbc;
		return null;
	}

	public void stopCreator(Player player)
	{
		creators.remove(getCreator(player));
	}

	public void incrementStep(SubRegionCreator sbc, Location location)
	{
		Player player = sbc.player;
		creators.remove(sbc);
		creators.add(new SubRegionCreator(player, sbc.regionName).setCorner(1, location));
		player.sendMessage("§bNow click the opposite corner of the new region");
	}

	/**
	 * Subregions
	 * 
	 * @param sbc
	 *            SubRegionCreator
	 * @param location
	 *            Location
	 */
	public void finish(SubRegionCreator sbc, Location location)
	{
		Player player = sbc.player;
		creators.remove(sbc);
		sbc.setCorner(2, location);

		World world = player.getWorld();
		RegionManager regionManager = wg.getRegionManager(world);
		BukkitPlayer wgPlayer = new BukkitPlayer(wg, player);
		ProtectedRegion actual = new ProtectedCuboidRegion(sbc.regionName, BukkitUtil.toVector(world.getBlockAt(sbc.corner1)), BukkitUtil.toVector(world.getBlockAt(sbc.corner2)));
		try
		{
			actual.setFlag(DefaultFlag.CHEST_ACCESS, DefaultFlag.CHEST_ACCESS.parseInput(wg, player, "allow"));
		}
		catch (InvalidFlagFormat e1)
		{
			e1.printStackTrace();
		}
		DefaultDomain owners = new DefaultDomain();
		owners.addPlayer(wgPlayer);

		if (clans != null)
		{
			if (clans.getClanFor(player.getName()) != null)
			{
				Clan clan = clans.getClanFor(player.getName());
				if (clans.isInClanLand(player))
				{
					player.sendMessage("§7Adding all officers and leader from your clan, §a"
							+ clan.getName() + " §7to the owners list");
					for (String off : clan.getOfficers())
					{
						owners.addPlayer(off);
					}
					owners.addPlayer(clan.getLeader());
				}
			}
		}
		else
			player.sendMessage("Not connected to clans");

		actual.setOwners(owners);
		actual.setPriority(subRegionPriority);

		try
		{
			regionManager.addRegion(actual);
			regionManager.save();
		}
		catch (ProtectionDatabaseException e)
		{
			e.printStackTrace();
		}

		player.sendMessage("§aSub region §9" + sbc.regionName
				+ " §acreated! You can now add members to it.");
	}

}