package au.com.mineauz.clr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
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

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;

import au.com.mineauz.clr.ChunkData.SubChunk;

public class PlayerChunkUpdater implements Listener
{
	public static int maxRange = 20;
	public static int signRange = 10;
	public static int updateFreq = 4;
	
	private HashMap<Player, HashSet<SubChunk>> mActiveSets;
	private HashMap<Player, Set<BlockVector>> mShownTiles;
	private HashMap<Player, Set<BlockVector>> mShownSigns;
	private HashMap<Player, BlockVector> mLastChunk;
	
	private BlockSender mSender;
	
	public PlayerChunkUpdater()
	{
		mActiveSets = new HashMap<Player, HashSet<SubChunk>>();
		mLastChunk = new HashMap<Player, BlockVector>();
		mShownTiles = new HashMap<Player, Set<BlockVector>>();
		mShownSigns = new HashMap<Player, Set<BlockVector>>();
		
		mSender = new BlockSender();
	}
	
	private void getClosestChunks(Player player, HashSet<SubChunk> chunks)
	{
		chunks.clear();
		
		Chunk current = player.getLocation().getChunk();
		int currentY = player.getLocation().getBlockY() >> 4;
		
		World world = player.getWorld();
		
		int chunkRange = (maxRange >> 4) + 1;

		for(int x = current.getX() - Bukkit.getViewDistance() / 2; x <= current.getX() + Bukkit.getViewDistance() / 2; ++x)
		{
			for(int z = current.getZ() - Bukkit.getViewDistance() / 2; z <= current.getZ() + Bukkit.getViewDistance() / 2; ++z)
			{
				if(EntityUtil.isNearChunk(player, x, z, CommonUtil.VIEW))
				{
					ChunkData chunk = CLRPlugin.getInstance().getChunkData(world.getChunkAt(x, z));
					
					for(int y = 0; y < world.getMaxHeight() >> 4; ++y)
					{
						if(Math.abs(x - current.getX()) < chunkRange && Math.abs(z - current.getZ()) < chunkRange && Math.abs(y - currentY) < chunkRange) 
							chunks.add(chunk.getSubChunk(y));
					}
				}
			}
		}
	}
	
	
	private void hideTiles(Player player, SubChunk chunk)
	{
		for(BlockVector loc : chunk.getTileEntities())
			mSender.add(loc, 0, 0);
	}
	
	private void updateActiveChunkSet(Player player)
	{
		BlockVector last = null;
		Location playerLoc = player.getLocation();
		BlockVector current = new BlockVector(playerLoc.getBlockX() >> 4, playerLoc.getBlockY() >> 4, playerLoc.getBlockZ() >> 4);
		
		if(!mLastChunk.containsKey(player))
			mActiveSets.put(player, new HashSet<SubChunk>((int)Math.pow(((maxRange >> 4) + 1) * 2, 3), 0.75f));
		else
		{
			last = mLastChunk.get(player);
			if(last.equals(current))
				return;
		}

		mLastChunk.put(player, current);
		
		HashSet<SubChunk> currentSet = mActiveSets.get(player);
		HashSet<SubChunk> old = new HashSet<SubChunk>(currentSet);
		
		getClosestChunks(player, currentSet);
		
		mSender.begin(player);
		
		for(SubChunk discarded : Misc.uniqueToB(currentSet, old))
			hideTiles(player, discarded);
		
		mSender.end();
	}
	
	private void updateVisibleTiles(Player player)
	{
		HashSet<SubChunk> nearestChunks = mActiveSets.get(player);
		if(nearestChunks == null)
			return;
		
		Set<BlockVector> shownTiles = mShownTiles.get(player);
		Set<BlockVector> shownSigns = mShownSigns.get(player);
		if(shownTiles == null)
		{
			shownTiles = new HashSet<BlockVector>();
			shownSigns = new HashSet<BlockVector>();
			mShownTiles.put(player, shownTiles);
			mShownSigns.put(player, shownSigns);
		}
		
		Set<BlockVector> old = new HashSet<BlockVector>(shownTiles);
		Set<BlockVector> oldSigns = new HashSet<BlockVector>(shownSigns);
		
		shownTiles.clear();	
		shownSigns.clear();
		
		int dist = maxRange * maxRange;
		int distSign = signRange * signRange;
		
		Vector pos = player.getLocation().toVector();
		
		for(SubChunk chunk : nearestChunks)
		{
			for(BlockVector block : chunk.getTileEntities())
			{
				if(block.distanceSquared(pos) < dist)
				{
					shownTiles.add(block);
					Block b = player.getWorld().getBlockAt(block.getBlockX(), block.getBlockY(), block.getBlockZ());
					if((b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) && block.distanceSquared(pos) < distSign)
						shownSigns.add(block);
				}
			}
		}
		
		mSender.begin(player);
		for(BlockVector hidden : Misc.uniqueToB(shownTiles, old))
			mSender.add(hidden, 0, 0);
		
		for(BlockVector shown : Misc.uniqueToB(old, shownTiles))
			mSender.add(shown);
			//Misc.sendBlockStateUpdate(player, player.getWorld().getBlockAt(shown.getBlockX(), shown.getBlockY(), shown.getBlockZ()).getState(), false);
		
		for(BlockVector hidden : Misc.uniqueToB(shownSigns, oldSigns))
			Misc.setSignState(player, hidden, new String[] {"","","",""});
		
		for(BlockVector shown : Misc.uniqueToB(oldSigns, shownSigns))
			Misc.sendBlockStateUpdate(player, player.getWorld().getBlockAt(shown.getBlockX(), shown.getBlockY(), shown.getBlockZ()).getState(), true);
		
		mSender.end();
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerMove(PlayerMoveEvent event)
	{
		updateActiveChunkSet(event.getPlayer());
		if((event.getFrom().getBlockX() / updateFreq != event.getTo().getBlockX() / updateFreq) ||
			(event.getFrom().getBlockZ() / updateFreq != event.getTo().getBlockZ() / updateFreq) ||
			(event.getFrom().getBlockY() / updateFreq != event.getTo().getBlockY() / updateFreq))
			updateVisibleTiles(event.getPlayer());
	}
		
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerRespawn(PlayerRespawnEvent event)
	{
		updateActiveChunkSet(event.getPlayer());
		updateVisibleTiles(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		final Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(CLRPlugin.getInstance(), new Runnable()
		{
			@Override
			public void run()
			{
				if(!player.isOnline())
					return;
				
				updateActiveChunkSet(player);
				updateVisibleTiles(player);
			}
		}, 2L);
		
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerQuit(PlayerQuitEvent event)
	{
		mActiveSets.remove(event.getPlayer());
		mLastChunk.remove(event.getPlayer());
		mShownTiles.remove(event.getPlayer());
		mShownSigns.remove(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerQuit(PlayerKickEvent event)
	{
		mActiveSets.remove(event.getPlayer());
		mLastChunk.remove(event.getPlayer());
		mShownTiles.remove(event.getPlayer());
		mShownSigns.remove(event.getPlayer());
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

