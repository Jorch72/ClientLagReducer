package au.com.mineauz.clr;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.PacketUtil;

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
		
		PacketUtil.addPacketListener(this, new TerrainPacketMonitor(mPlayers), PacketType.BLOCK_CHANGE, PacketType.MAP_CHUNK, PacketType.MAP_CHUNK_BULK, PacketType.MULTI_BLOCK_CHANGE, PacketType.UPDATE_SIGN, PacketType.TILE_ENTITY_DATA);
	};
	public static CLRPlugin getInstance()
	{
		return instance;
	}
	
	public ChunkData getChunkData(Chunk chunk)
	{
		return mChunks.getChunk(chunk);
	}
	
	public boolean isActiveChunk(ChunkData chunk, Player player)
	{
		return mPlayers.isActive(chunk, player);
	}
}
