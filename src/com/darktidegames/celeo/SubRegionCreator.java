package com.darktidegames.celeo;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SubRegionCreator
{

	public final Player player;
	public final String regionName;
	public Location corner1;
	public Location corner2;

	public SubRegionCreator(Player player, String regionName)
	{
		this.player = player;
		this.regionName = regionName;
		corner1 = null;
		corner2 = null;
	}

	public int getStep()
	{
		if (corner1 == null)
			return 1;
		if (corner2 == null)
			return 2;
		return 1;
	}

	public SubRegionCreator setCorner(int i, Location location)
	{
		switch (i)
		{
		default:
		case 1:
			corner1 = location;
			break;
		case 2:
			corner2 = location;
			break;
		}
		return this;
	}

}