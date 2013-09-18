package au.com.mineauz.clr;

public class RawChunk
{
	private byte[] mRaw;
	private int mExistsMap;
	private int mExtMap;
	
	public RawChunk(byte[] raw, int existsMap, int extMap)
	{
		mRaw = raw;
		mExistsMap = existsMap;
		mExtMap = extMap;
	}
	
	public int getSectionCount()
	{
		return Integer.bitCount(mExistsMap);
	}
	
	private boolean doesSegmentExist(int segment)
	{
		return (mExistsMap & (1 << segment)) != 0;
	}
	private int getActualSegment(int segment)
	{
		int index = 0;
		for(int i = 0; i < segment; ++i)
		{
			if(doesSegmentExist(segment))
				++index;
		}
		return index;
	}
	
	public int getBlockId(int x, int y, int z)
	{
		if(!doesSegmentExist(y >> 4))
			return 0;
		
		int seg = getActualSegment(y >> 4);
		
		int id = mRaw[seg*4096 + (y&0xF)*256 + z * 16 + x];
		
		// TODO: Should probably do ext block ids
		
		return id;
	}
	
	public void setBlockId(int x, int y, int z, int id)
	{
		if(!doesSegmentExist(y >> 4))
			return;
		
		int seg = getActualSegment(y >> 4);
		
		mRaw[seg*4096 + (y&0xF)*256 + z * 16 + x] = (byte)id;
	}
	
	
}
