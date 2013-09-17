package au.com.mineauz.clr;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.util.BlockVector;

public class ChunkData
{
	public ChunkData(Chunk chunk)
	{
		this.chunk = chunk;
		interestingBlocks = new HashSet<BlockVector>();
	}
	public final Chunk chunk;
	
	public final Set<BlockVector> interestingBlocks;
	
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
}
