package com.darktidegames.celeo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LOListener implements Listener
{

	final DarkLand plugin;

	public LOListener(DarkLand plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (!plugin.isPlacing(player.getName()))
			return;
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			return;
		event.setCancelled(true);
		plugin.handlePlacement(player, event.getClickedBlock().getLocation());
	}

}