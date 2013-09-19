package au.com.mineauz.clr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.v1_6_R2.Packet;
import net.minecraft.server.v1_6_R2.Packet130UpdateSign;

import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.PacketUtil;

public class Misc
{
	private static void sendPacket(Player player, Packet packet)
	{
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}
	
	public static void sendBlockChange(Player player, BlockVector block, int typeId, int data)
	{
		CommonPacket packet = new CommonPacket(PacketType.BLOCK_CHANGE);
		packet.write(PacketFields.BLOCK_CHANGE.typeId, typeId);
		packet.write(PacketFields.BLOCK_CHANGE.x, block.getBlockX());
		packet.write(PacketFields.BLOCK_CHANGE.y, block.getBlockY());
		packet.write(PacketFields.BLOCK_CHANGE.z, block.getBlockZ());
		packet.write(PacketFields.BLOCK_CHANGE.data, data);
		
		PacketUtil.sendPacket(player, packet);
	}
	
	public static void setSignState(Player player, BlockVector location, String[] lines)
	{
		Packet130UpdateSign packet = new Packet130UpdateSign(location.getBlockX(), location.getBlockY(), location.getBlockZ(), lines);
		sendPacket(player, packet);
	}
	
	public static void sendBlockStateUpdate(Player player, BlockState state, boolean signs)
	{
		player.sendBlockChange(state.getLocation(), state.getTypeId(), state.getRawData());
		
		if (state instanceof Sign && signs)
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
	
	public static <T> Set<T> limitedCopy(Collection<T> a, int max)
	{
		HashSet<T> set = new HashSet<T>();
		int count = 0;
		for(T item : a)
		{
			if(count > max)
				break;
			
			set.add(item);
			++count;
		}
		
		return set;
	}
}
