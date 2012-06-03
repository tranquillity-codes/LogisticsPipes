package net.minecraft.src.buildcraft.krapht.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.NetworkManager;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraft.src.buildcraft.krapht.logic.LogicCrafting;
import net.minecraft.src.buildcraft.transport.PipeLogicDiamond;
import net.minecraft.src.buildcraft.transport.TileGenericPipe;
import net.minecraft.src.forge.IPacketHandler;

public class PacketHandler implements IPacketHandler {

	@Override
	public void onPacketData(NetworkManager network, String channel, byte[] bytes) {
		DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
		try
		{
			NetServerHandler net = (NetServerHandler)network.getNetHandler();
			int packetID = data.read();
			switch (packetID) {

			case NetworkConstants.CRAFTING_PIPE_NEXT_SATELLITE:
				PacketCoordinates packet = new PacketCoordinates();
				// DEBUG
				System.out.println("[BCLP] debug: PacketCoordinates CRAFTING_PIPE_NEXT_SATELLITE ");
				System.out.print(bytes);
				// DEBUG
				packet.readData(data);
				onCraftingPipeNextSatellite(net.getPlayerEntity(), packet);
				break;
				
			case NetworkConstants.CRAFTING_PIPE_PREV_SATELLITE:
				PacketCoordinates packetA = new PacketCoordinates();
				// DEBUG
				System.out.println("[BCLP] debug: PacketCoordinates CRAFTING_PIPE_PREV_SATELLITE ");
				System.out.print(bytes);
				// DEBUG
				packetA.readData(data);
				onCraftingPipePrevSatellite(net.getPlayerEntity(), packetA);
				break;
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void onCraftingPipeNextSatellite(EntityPlayerMP player, PacketCoordinates packet) {
		TileGenericPipe pipe = getPipe(player.worldObj, packet.posX, packet.posY, packet.posZ);
		if(pipe == null)
			return;
		
		if(!(pipe.pipe.logic instanceof LogicCrafting))
			return;
		
		((LogicCrafting) pipe.pipe.logic).setNextSatellite(player);
	}

	private void onCraftingPipePrevSatellite(EntityPlayerMP player, PacketCoordinates packet) {
		TileGenericPipe pipe = getPipe(player.worldObj, packet.posX, packet.posY, packet.posZ);
		if(pipe == null)
			return;
		
		if(!(pipe.pipe.logic instanceof LogicCrafting))
			return;
		
		((LogicCrafting) pipe.pipe.logic).setPrevSatellite(player);
	}

	// BuildCraft method
	/**
	 * Retrieves pipe at specified coordinates if any.
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private TileGenericPipe getPipe(World world, int x, int y, int z) {
		if(!world.blockExists(x, y, z))
			return null;
		
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if(!(tile instanceof TileGenericPipe))
			return null;

		return (TileGenericPipe)tile;
	}
	// BuildCraft method end
}
