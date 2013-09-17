package au.com.mineauz.clr;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkListener implements Listener
{
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChunkLoad(ChunkLoadEvent event)
	{
		
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChunkUnload(ChunkUnloadEvent event)
	{
		
	}
}
