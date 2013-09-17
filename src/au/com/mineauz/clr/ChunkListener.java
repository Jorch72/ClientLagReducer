package au.com.mineauz.clr;

import java.util.HashMap;

import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.util.BlockVector;

public class ChunkListener implements Listener
{
	private HashMap<Chunk, ChunkData> mLoadedChunks;
	
	public ChunkListener()
	{
		mLoadedChunks = new HashMap<Chunk, ChunkData>();
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChunkLoad(ChunkLoadEvent event)
	{
		ChunkData chunk = new ChunkData(event.getChunk());
		scanChunk(chunk);
		mLoadedChunks.put(chunk.chunk, chunk);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChunkUnload(ChunkUnloadEvent event)
	{
		mLoadedChunks.remove(event.getChunk());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onTileAdded(BlockPlaceEvent event)
	{
		if(event.getBlock().getState().getClass().isAssignableFrom(BlockState.class))
			return;
		
		ChunkData chunk = mLoadedChunks.get(event.getBlock().getChunk());
		if(chunk == null)
			return;
		
		chunk.interestingBlocks.add(new BlockVector(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()));
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onTileRemoved(BlockBreakEvent event)
	{
		if(event.getBlock().getState().getClass().isAssignableFrom(BlockState.class))
			return;
		
		ChunkData chunk = mLoadedChunks.get(event.getBlock().getChunk());
		if(chunk == null)
			return;
		
		chunk.interestingBlocks.remove(new BlockVector(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()));
	}
	
	private void scanChunk(ChunkData chunk)
	{
		for(BlockState state : chunk.chunk.getTileEntities())
			chunk.interestingBlocks.add(new BlockVector(state.getX(), state.getY(), state.getZ()));
	}
	
	
}
