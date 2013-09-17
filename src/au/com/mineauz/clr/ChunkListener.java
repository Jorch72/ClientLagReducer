package au.com.mineauz.clr;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

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
		
		chunk.addTileEntity(event.getBlock());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onTileRemoved(BlockBreakEvent event)
	{
		if(event.getBlock().getState().getClass().isAssignableFrom(BlockState.class))
			return;
		
		ChunkData chunk = mLoadedChunks.get(event.getBlock().getChunk());
		if(chunk == null)
			return;
		
		chunk.removeTileEntity(event.getBlock());
	}
	
	public void updateFromLoadedChunks()
	{
		mLoadedChunks.clear();
		
		for(World world : Bukkit.getWorlds())
		{
			for(Chunk chunk : world.getLoadedChunks())
			{
				ChunkData chunkdata = new ChunkData(chunk);
				mLoadedChunks.put(chunkdata.chunk, chunkdata);
			}
		}
	}
	
	public ChunkData getChunk(Chunk chunk)
	{
		return mLoadedChunks.get(chunk);
	}
	
}
