package au.com.mineauz.clr;

import java.util.Arrays;
import java.util.zip.Deflater;

import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.events.PacketReceiveEvent;
import com.bergerkiller.bukkit.common.events.PacketSendEvent;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.protocol.PacketListener;

public class TerrainPacketMonitor implements PacketListener
{
	private PlayerChunkUpdater mChunkUpdater;
	
	public TerrainPacketMonitor(PlayerChunkUpdater updater)
	{
		mChunkUpdater = updater;
	}
	
	@Override
	public void onPacketReceive( PacketReceiveEvent paramPacketReceiveEvent ) {}
	
	
	private void removeTileEntities(RawChunk raw, Chunk chunk)
	{
		for(BlockState tile : chunk.getTileEntities())
			raw.setBlockId(tile.getX() & 0xF, tile.getY(), tile.getZ() & 0xF, 0);
	}
	
	@Override
	public void onPacketSend( PacketSendEvent event )
	{
		onMonitorPacketSend(event.getPacket(), event.getPlayer());
	}
	
	public void onMonitorPacketSend( CommonPacket packet, Player player )
	{
		switch(packet.getType())
		{
		case MAP_CHUNK:
		{
			int x = packet.read(PacketFields.MAP_CHUNK.x);
			int z = packet.read(PacketFields.MAP_CHUNK.z);
			
			Chunk chunk = player.getWorld().getChunkAt(x,z);
			removeTileEntities(new RawChunk(packet.read(PacketFields.MAP_CHUNK.inflatedBuffer), packet.read(PacketFields.MAP_CHUNK.chunkDataBitMap), packet.read(PacketFields.MAP_CHUNK.chunkBiomeBitMap)), chunk);
			
			Deflater deflater = new Deflater(-1);
			deflater.setInput(packet.read(PacketFields.MAP_CHUNK.inflatedBuffer));
			deflater.finish();
			byte[] buffer = new byte[packet.read(PacketFields.MAP_CHUNK.inflatedBuffer).length];
			deflater.deflate(buffer);
			packet.write(PacketFields.MAP_CHUNK.buffer, buffer);
			packet.write(PacketFields.MAP_CHUNK.size, buffer.length);
			deflater.end();
			break;
		}
		case MAP_CHUNK_BULK:
		{
			byte[][] buffers = packet.read(PacketFields.MAP_CHUNK_BULK.inflatedBuffers);
			int[] x = packet.read(PacketFields.MAP_CHUNK_BULK.bulk_x);
			int[] z = packet.read(PacketFields.MAP_CHUNK_BULK.bulk_z);
			//PacketFields.MAP_CHUNK_BULK.bulk_chunkDataBitMap
			for(int i = 0; i < x.length; ++i)
			{
				Chunk chunk = player.getWorld().getChunkAt(x[i],z[i]);
				removeTileEntities(new RawChunk(buffers[i], packet.read(PacketFields.MAP_CHUNK_BULK.bulk_chunkDataBitMap)[i], packet.read(PacketFields.MAP_CHUNK_BULK.bulk_chunkBiomeBitMap)[i]), chunk);
			}
			
			byte[] buildBuffer = new byte[0];
			int start = 0;
			for(int i = 0; i < x.length; ++i)
			{
				if(buildBuffer.length < start + buffers[i].length)
				{
					buildBuffer = Arrays.copyOf(buildBuffer, start + buffers[i].length);
				}
				
				System.arraycopy(buffers[i], 0, buildBuffer, start, buffers[i].length);
				start += buffers[i].length;
			}
			packet.write(PacketFields.MAP_CHUNK_BULK.buildBuffer, buildBuffer);
			
			//Arrays.fill(packet.read(PacketFields.MAP_CHUNK_BULK.deflatedData), (byte)0);
			break;
		}
		case BLOCK_CHANGE:
			break;
		case MULTI_BLOCK_CHANGE:
			break;
		default:
			break;
		}
	}

}
