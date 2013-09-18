package au.com.mineauz.clr;

import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.BlockVector;

import com.google.common.collect.TreeMultimap;

import au.com.mineauz.clr.ChunkData.SubChunk;

public class PlayerChunkUpdater implements Listener
{
	public static int maxTiles = 100;
	
	private HashMap<Player, HashSet<ChunkData>> mActiveChunks;
	private HashMap<Player, HashSet<SubChunk>> mActiveSets;
	private HashMap<Player, BlockVector> mLastChunk;
	
	private int showCount = 0;
	private int hideCount = 0;
	
	public PlayerChunkUpdater()
	{
		mActiveChunks = new HashMap<Player, HashSet<ChunkData>>();
		mActiveSets = new HashMap<Player, HashSet<SubChunk>>();
		mLastChunk = new HashMap<Player, BlockVector>();
	}
	
	public boolean isActive(ChunkData chunk, Player player)
	{
		HashSet<ChunkData> chunks = mActiveChunks.get(player);
		
		if(chunks == null)
			return false;
		
		return chunks.contains(chunk);
	}
	private void getClosestChunks(Player player, HashSet<SubChunk> chunks, HashSet<ChunkData> visible)
	{
		chunks.clear();
		visible.clear();
		
		int count = 0;
		Chunk current = player.getLocation().getChunk();

		Location loc = player.getLocation();
		
		World world = player.getWorld();
		TreeMultimap<Integer, SubChunk> ordered = TreeMultimap.create();
		
		for(int x = current.getX() - Bukkit.getViewDistance() / 2; x <= current.getX() + Bukkit.getViewDistance() / 2; ++x)
		{
			for(int z = current.getZ() - Bukkit.getViewDistance() / 2; z <= current.getZ() + Bukkit.getViewDistance() / 2; ++z)
			{
				if(world.isChunkInUse(x, z))
				{
					ChunkData chunk = CLRPlugin.getInstance().getChunkData(world.getChunkAt(x, z));
					visible.add(chunk);
					
					for(int y = 0; y < world.getMaxHeight() >> 4; ++y)
					{
						int dist = (int)(Math.pow(loc.getX() - (x * 16 + 8), 2) + Math.pow(loc.getY() - (y * 16 + 8),2) + Math.pow(loc.getZ() - (z * 16 + 8),2));
						ordered.put(dist, chunk.getSubChunk(y));
					}
				}
			}
		}
		
		
		// Now get only the nearest chunks until the count is met
		for(SubChunk chunk : ordered.values())
		{
			chunks.add(chunk);
			count += chunk.getTileCount();
			
			if(count >= maxTiles)
				break;
		}
	}
	
	private void updateTiles(Player player, SubChunk chunk, boolean show)
	{
		for(BlockVector loc : chunk.getTileEntities())
		{
			if(show)
			{
				showCount++;
				Block actual = player.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
				Misc.sendBlockStateUpdate(player, actual.getState());
			}
			else
			{
				hideCount++;
				player.sendBlockChange(loc.toLocation(player.getWorld()), 0, (byte)0);
			}
		}
	}
	
	private void updateActiveChunkSet(Player player)
	{
		BlockVector last = null;
		Location playerLoc = player.getLocation();
		BlockVector current = new BlockVector(playerLoc.getBlockX() >> 4, playerLoc.getBlockY() >> 4, playerLoc.getBlockZ() >> 4);
		
		if(!mLastChunk.containsKey(player))
		{
			mActiveSets.put(player, new HashSet<SubChunk>());
			mActiveChunks.put(player, new HashSet<ChunkData>());
		}
		else
		{
			last = mLastChunk.get(player);
			if(last.equals(current))
				return;
		}

		mLastChunk.put(player, current);
		
		HashSet<SubChunk> currentSet = mActiveSets.get(player);
		HashSet<SubChunk> old = new HashSet<SubChunk>(currentSet);
		
		HashSet<ChunkData> visibleChunks = mActiveChunks.get(player);
		HashSet<ChunkData> oldChunks = new HashSet<ChunkData>(visibleChunks);
		
		getClosestChunks(player, currentSet, visibleChunks);
		
		showCount = hideCount = 0;
		for(ChunkData loaded : Misc.uniqueToB(oldChunks, visibleChunks))
		{
			for(int y = 0; y < player.getWorld().getMaxHeight() >> 4; ++y)
				updateTiles(player, loaded.getSubChunk(y), false);
		}
		
		for(SubChunk discarded : Misc.uniqueToB(currentSet, old))
			updateTiles(player, discarded, false);
		
		for(SubChunk gained : Misc.uniqueToB(old, currentSet))
			updateTiles(player, gained, true);
		
		System.out.println("Shown: " + showCount + " Hidden: " + hideCount);
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
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerQuit(PlayerQuitEvent event)
	{
		mActiveSets.remove(event.getPlayer());
		mActiveChunks.remove(event.getPlayer());
		mLastChunk.remove(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerQuit(PlayerKickEvent event)
	{
		mActiveSets.remove(event.getPlayer());
		mActiveChunks.remove(event.getPlayer());
		mLastChunk.remove(event.getPlayer());
	}
	
	public void onPlayerChunkLoad(Chunk chunk, Player player)
	{
		
	}

	public void onPlayerChunkUnload(Chunk chunk, Player player)
	{
		
	}
	
	public void onChunkUpdate(Chunk chunk)
	{
		
	}
}

