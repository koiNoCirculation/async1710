package org.tgt.async1710;

import net.minecraft.init.Blocks;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.network.NetHandlerPlayServer;

@Mod(modid = Async1710.MODID, version = Async1710.VERSION)
public class Async1710
{
    public static final String MODID = "async1710";
    public static final String VERSION = "1.0";

    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
		// some example code
        Class<NetHandlerPlayServer> netHandlerPlayServerClass = NetHandlerPlayServer.class;
        System.out.println("DIRT BLOCK >> "+Blocks.dirt.getUnlocalizedName());
    }
}
