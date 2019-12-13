/**
 * 
 */
package com.someguyssoftware.treasure2.command;

import java.util.Random;

import com.someguyssoftware.gottschcore.positional.Coords;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.config.TreasureConfig;
import com.someguyssoftware.treasure2.worldgen.WitherTreeWorldGenerator;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * 
 * @author Mark Gottschling on Mar 25, 2018
 *
 */
public class SpawnWitherTreeCommand extends CommandBase {

	@Override
	public String getName() {
		return "t2-withertree";
	}

	@Override
	public String getUsage(ICommandSender var1) {
		return "/t2-withertree <x> <y> <z>: spawns a Treasure! wither tree at location (x,y,z)";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) {
		EntityPlayer player = (EntityPlayer) commandSender.getCommandSenderEntity();
		try {

			int x, y, z = 0;
			x = Integer.parseInt(args[0]);
			y = Integer.parseInt(args[1]);
			z = Integer.parseInt(args[2]);
			
			if (player != null) {
    			World world = commandSender.getEntityWorld();
    			Treasure.logger.debug("Starting to build Treasure! wither tree ...");

    			Random random = new Random();
    			//BlockPos pos = new BlockPos(x, y, z);
    			// TODO get this to work
    			WitherTreeWorldGenerator gen = new WitherTreeWorldGenerator();
    			gen.generate(world, random, new Coords(x, y, z), TreasureConfig.WITHER_TREE);
    		}
		}
		catch(Exception e) {
			player.sendMessage(new TextComponentString("Error:  " + e.getMessage()));
			Treasure.logger.error("Error generating Treasure! well:", e);
			e.printStackTrace();
		}
	}
}
