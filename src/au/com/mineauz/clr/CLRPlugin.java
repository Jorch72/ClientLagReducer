package au.com.mineauz.clr;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.plugin.java.JavaPlugin;

public class CLRPlugin extends JavaPlugin
{
	private ChunkListener mChunks;
	private PlayerChunkUpdater mPlayers;
	
	private static CLRPlugin instance;
	
	public void onEnable() 
	{
		instance = this;
		
		mChunks = new ChunkListener();
		mPlayers = new PlayerChunkUpdater();
		
		Bukkit.getPluginManager().registerEvents(mChunks, this);
		Bukkit.getPluginManager().registerEvents(mPlayers, this);
		
		mChunks.updateFromLoadedChunks();
	};
	public static CLRPlugin getInstance()
	{
		return instance;
	}
	
	public ChunkData getChunkData(Chunk chunk)
	{
		return mChunks.getChunk(chunk);
	}
}
