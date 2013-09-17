package au.com.mineauz.clr;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.BlockVector;

public class PlayerChunkUpdater implements Listener
{
	private HashMap<Player, HashSet<ChunkData>> mActiveSets;
	private HashMap<Player, Chunk> mLastChunk;
	
	public PlayerChunkUpdater()
	{
		mActiveSets = new HashMap<Player, HashSet<ChunkData>>();
		mLastChunk = new HashMap<Player, Chunk>();
	}
	
	private void updateTiles(Player player, ChunkData chunk, boolean show)
	{
		for(BlockVector loc : chunk.interestingBlocks)
		{
			if(show)
			{
				Block actual = player.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
				Misc.sendBlockStateUpdate(player, actual.getState());
			}
			else
			{
				player.sendBlockChange(loc.toLocation(player.getWorld()), 0, (byte)0);
			}
		}
	}
	
	private boolean getShowStatus(ChunkData chunk, Chunk playersChunk)
	{
		return (Math.abs(chunk.chunk.getX() - playersChunk.getX()) + Math.abs(chunk.chunk.getZ() - playersChunk.getZ()) < 2);
	}
	
	private void updateActiveChunkSet(Player player)
	{
		Chunk last = null;
		if(!mLastChunk.containsKey(player))
			mActiveSets.put(player, new HashSet<ChunkData>());
		else
		{
			last = mLastChunk.get(player);
			if(last.equals(player.getLocation().getChunk()))
				return;
		}

		Chunk current = player.getLocation().getChunk();
		mLastChunk.put(player, current);
		
		HashSet<ChunkData> currentSet = mActiveSets.get(player);
		HashSet<ChunkData> old = new HashSet<ChunkData>(currentSet);
		HashSet<ChunkData> toShow = new HashSet<ChunkData>();
		HashSet<ChunkData> toHide = new HashSet<ChunkData>();
		
		currentSet.clear();
		
		World world = player.getWorld();
		
		for(int x = current.getX() - Bukkit.getViewDistance() / 2; x <= current.getX() + Bukkit.getViewDistance() / 2; ++x)
		{
			for(int z = current.getZ() - Bukkit.getViewDistance() / 2; z <= current.getZ() + Bukkit.getViewDistance() / 2; ++z)
			{
				if(world.isChunkInUse(x, z))
				{
					ChunkData chunk = CLRPlugin.getInstance().getChunkData(world.getChunkAt(x, z));
					
					if(chunk == null)
						continue;
					
					if(!old.contains(chunk))
					{
						if(getShowStatus(chunk, current))
							toShow.add(chunk);
						else
							toHide.add(chunk);
					}
					else
					{
						boolean showNew = getShowStatus(chunk, current);
						boolean showOld = (last != null ? getShowStatus(chunk, last) : false);
						
						if(showNew && !showOld)
							toShow.add(chunk);
						else if(showOld && !showNew)
							toHide.add(chunk);
					}
					
					currentSet.add(chunk);
				}
			}
		}
		
		for(ChunkData chunk : toShow)
			updateTiles(player, chunk, true);
		
		for(ChunkData chunk : toHide)
			updateTiles(player, chunk, false);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerMove(PlayerMoveEvent event)
	{
		updateActiveChunkSet(event.getPlayer());
	}
		
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerRespawn(PlayerRespawnEvent event)
	{
		updateActiveChunkSet(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		updateActiveChunkSet(event.getPlayer());
	}
	
	
}

