package au.com.mineauz.clr;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.utils.PacketUtil;

public class BlockSender
{
	private Player mPlayer;
	private HashMap<Chunk, OutputSet> mChunks;
	
	public void begin(Player player)
	{
		mPlayer = player;
	
		mChunks = new HashMap<Chunk, OutputSet>();
	}
	
	public void add(BlockVector location)
	{
		Block block = mPlayer.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		add(location, block.getTypeId(), block.getData());
	}
	public void add(BlockVector location, int material, int data)
	{
		Chunk chunk = mPlayer.getWorld().getChunkAt(location.getBlockX() >> 4, location.getBlockZ() >> 4);
		if(!mChunks.containsKey(chunk))
			mChunks.put(chunk, new OutputSet());
		
		OutputSet output = mChunks.get(chunk);
		
		short locPart = (short)((location.getBlockX() & 0xF) << 12 | (location.getBlockZ() & 0xF) << 8 | (location.getBlockY() & 0xFF));
		short dataPart = (short)((material & 4095) << 4 | (data & 0xF));
		
		try
		{
			output.output.writeShort(locPart);
			output.output.writeShort(dataPart);
			++output.blockCount;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void end()
	{
		for(Entry<Chunk, OutputSet> entry : mChunks.entrySet())
		{
			CommonPacket packet = PacketFields.MULTI_BLOCK_CHANGE.newInstance();
			packet.write(PacketFields.MULTI_BLOCK_CHANGE.chunkX, entry.getKey().getX());
			packet.write(PacketFields.MULTI_BLOCK_CHANGE.chunkZ, entry.getKey().getZ());
			
			packet.write(PacketFields.MULTI_BLOCK_CHANGE.blockCount, entry.getValue().blockCount);
			packet.write("c", entry.getValue().stream.toByteArray());
			//packet.write(PacketFields.MULTI_BLOCK_CHANGE.blockData, entry.getValue().stream.toByteArray());
//			
			PacketUtil.sendPacket(mPlayer, packet);
		}
		
		mPlayer = null;
		mChunks.clear();
		mChunks = null;
	}
	
	private class OutputSet
	{
		public OutputSet()
		{
			blockCount = 0;
			stream = new ByteArrayOutputStream();
			output = new DataOutputStream(stream);
		}
		
		public int blockCount;
		public DataOutputStream output;
		public ByteArrayOutputStream stream;
	}
}
