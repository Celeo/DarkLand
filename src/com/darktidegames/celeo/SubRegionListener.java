package com.darktidegames.celeo;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldguard.protection.ApplicableRegionSet;

public class SubRegionListener implements Listener
{

	final DarkLand plugin;

	public SubRegionListener(DarkLand plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler
	public void interact(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (player == null)
			return;
		Block block = event.getClickedBlock();
		if (block == null)
			return;
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			return;
		SubRegionCreator src = plugin.getCreator(player);
		if (src != null)
		{
			if (!block.getWorld().getName().equalsIgnoreCase("world"))
			{
				player.sendMessage("§cYou can only place subregions in the main world.");
				plugin.stopCreator(player);
				return;
			}
			ApplicableRegionSet exist = plugin.getWorldGuard().getRegionManager(block.getWorld()).getApplicableRegions(block.getLocation());
			if (exist.size() == 0)
			{
				player.sendMessage("§cYou cannot only create subregions in existing regions!");
				plugin.stopCreator(player);
				return;
			}
			if (!exist.isOwnerOfAll(plugin.getWorldGuard().wrapPlayer(player)))
			{
				player.sendMessage("§cYou are not an owner of all the intersection regions!");
				plugin.stopCreator(player);
				return;
			}
			switch (src.getStep())
			{
			default:
			case 1:
				plugin.incrementStep(src, block.getLocation());
				break;
			case 2:
				plugin.finish(src, block.getLocation());
				break;
			}
		}
	}

}