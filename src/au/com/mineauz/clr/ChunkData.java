package au.com.mineauz.clr;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

public class ChunkData
{
	public final Chunk chunk;
	private SubChunk[] mSubChunks;
	
	public ChunkData(Chunk chunk)
	{
		this.chunk = chunk;
		mSubChunks = new SubChunk[chunk.getWorld().getMaxHeight() >> 4];
		for(int i = 0; i < chunk.getWorld().getMaxHeight() >> 4; ++i)
			mSubChunks[i] = new SubChunk(i);
		
		populate();
	}

	private void populate()
	{
		BlockState[] tiles = chunk.getTileEntities();
		for(SubChunk sub : mSubChunks)
			sub.populateTiles(tiles);
	}
	
	public void addTileEntity(Block block)
	{
		
	}
	
	public void removeTileEntity(Block block)
	{
		
	}
	
	public int getTileCount()
	{
		int count = 0;
		for(SubChunk sub : mSubChunks)
			count += sub.getTileCount();
		
		return count;
	}
	
	public SubChunk getSubChunk(int y)
	{
		return mSubChunks[y];
	}
	
	public class SubChunk implements Comparable<SubChunk>
	{
		private final int y;
		private HashSet<BlockVector> mTiles;
		
		public SubChunk(int y)
		{
			this.y = y;
			mTiles = new HashSet<BlockVector>();
		}
		
		private void populateTiles(BlockState[] tiles)
		{
			mTiles.clear();
			for(BlockState tile : tiles)
			{
				if(tile.getY() >> 4 == y)
					mTiles.add(new BlockVector(tile.getX(), tile.getY(), tile.getZ()));
			}
		}
		
		public int getTileCount()
		{
			return mTiles.size();
		}
		
		public Set<BlockVector> getTileEntities()
		{
			return mTiles;
		}
		
		public int getX()
		{
			return chunk.getX();
		}
		
		public int getY()
		{
			return y;
		}
		
		public int getZ()
		{
			return chunk.getZ();
		}
		
		@Override
		public String toString()
		{
			return "Chunk (" + chunk.getX() + "," + y + "," + chunk.getZ() + ")";
		}

		@Override
		public int compareTo( SubChunk o )
		{
			return 0;
		}
	}
	
	@Override
	public int hashCode()
	{
		return chunk.hashCode();
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof ChunkData))
			return false;
		
		return chunk.equals(((ChunkData)obj).chunk);
	}
	
	@Override
	public String toString()
	{
		return "Chunk (" + chunk.getX() + "," + chunk.getZ() + ")";
	}
}
