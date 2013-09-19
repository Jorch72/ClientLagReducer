package au.com.mineauz.clr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
import org.bukkit.util.Vector;

import au.com.mineauz.clr.ChunkData.SubChunk;

public class PlayerChunkUpdater implements Listener
{
	public static int maxRange = 20;
	
	private HashMap<Player, HashSet<ChunkData>> mActiveChunks;
	private HashMap<Player, HashSet<SubChunk>> mActiveSets;
	private HashMap<Player, Set<BlockVector>> mShownTiles;
	private HashMap<Player, BlockVector> mLastChunk;
	
	public PlayerChunkUpdater()
	{
		mActiveChunks = new HashMap<Player, HashSet<ChunkData>>();
		mActiveSets = new HashMap<Player, HashSet<SubChunk>>();
		mLastChunk = new HashMap<Player, BlockVector>();
		mShownTiles = new HashMap<Player, Set<BlockVector>>();
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
		
		Chunk current = player.getLocation().getChunk();
		int currentY = player.getLocation().getBlockY() >> 4;
		
		World world = player.getWorld();
		
		int chunkRange = (maxRange >> 4) + 1;

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
						if(Math.abs(x - current.getX()) < chunkRange && Math.abs(z - current.getZ()) < chunkRange && Math.abs(y - currentY) < chunkRange) 
							chunks.add(chunk.getSubChunk(y));
					}
				}
			}
		}
	}
	
	
	private void updateTiles(Player player, SubChunk chunk, boolean show)
	{
		for(BlockVector loc : chunk.getTileEntities())
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
		
		for(ChunkData loaded : Misc.uniqueToB(oldChunks, visibleChunks))
		{
			for(int y = 0; y < player.getWorld().getMaxHeight() >> 4; ++y)
				updateTiles(player, loaded.getSubChunk(y), false);
		}
		
		for(SubChunk discarded : Misc.uniqueToB(currentSet, old))
			updateTiles(player, discarded, false);
		
	}
	
	private void updateVisibleTiles(Player player)
	{
		HashSet<SubChunk> nearestChunks = mActiveSets.get(player);
		if(nearestChunks == null)
			return;
		
		Set<BlockVector> shownTiles = mShownTiles.get(player);
		if(shownTiles == null)
		{
			shownTiles = new HashSet<BlockVector>();
			mShownTiles.put(player, shownTiles);
		}
		
		Set<BlockVector> old = new HashSet<BlockVector>(shownTiles);
		
		shownTiles.clear();	
		
		int dist = maxRange * maxRange;
		Vector pos = player.getLocation().toVector();
		
		for(SubChunk chunk : nearestChunks)
		{
			for(BlockVector block : chunk.getTileEntities())
			{
				if(block.distanceSquared(pos) < dist)
					shownTiles.add(block);
			}
		}
		
		for(BlockVector hidden : Misc.uniqueToB(shownTiles, old))
			Misc.sendBlockChange(player, hidden, 0, 0);
		
		for(BlockVector shown : Misc.uniqueToB(old, shownTiles))
			Misc.sendBlockStateUpdate(player, player.getWorld().getBlockAt(shown.getBlockX(), shown.getBlockY(), shown.getBlockZ()).getState());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerMove(PlayerMoveEvent event)
	{
		updateActiveChunkSet(event.getPlayer());
		updateVisibleTiles(event.getPlayer());
	}
		
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerRespawn(PlayerRespawnEvent event)
	{
		updateActiveChunkSet(event.getPlayer());
		//updateVisibleTiles(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		updateActiveChunkSet(event.getPlayer());
		//updateVisibleTiles(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerQuit(PlayerQuitEvent event)
	{
		mActiveSets.remove(event.getPlayer());
		mActiveChunks.remove(event.getPlayer());
		mLastChunk.remove(event.getPlayer());
		mShownTiles.remove(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerQuit(PlayerKickEvent event)
	{
		mActiveSets.remove(event.getPlayer());
		mActiveChunks.remove(event.getPlayer());
		mLastChunk.remove(event.getPlayer());
		mShownTiles.remove(event.getPlayer());
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

