package au.com.mineauz.clr;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.v1_6_R2.Packet;
import net.minecraft.server.v1_6_R2.Packet130UpdateSign;

import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Misc
{
	private static void sendPacket(Player player, Packet packet)
	{
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}
	public static void sendBlockStateUpdate(Player player, BlockState state)
	{
		player.sendBlockChange(state.getLocation(), state.getTypeId(), state.getRawData());
		
		if (state instanceof Sign)
		{
			Packet130UpdateSign packet = new Packet130UpdateSign(state.getX(), state.getY(), state.getZ(), ((Sign)state).getLines());
			sendPacket(player, packet);
		}
		
	}
	
	public static <T> Set<T> uniqueToB(Set<T> a, Set<T> b)
	{
		HashSet<T> clone = new HashSet<T>(b);
		clone.removeAll(a);
		
		return clone;
	}
}
