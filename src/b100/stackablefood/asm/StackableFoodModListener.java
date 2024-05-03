package b100.stackablefood.asm;

import b100.stackablefood.StackableFoodMod;
import b100.stackablefood.asm.utils.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.item.ItemFood;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;

public class StackableFoodModListener {
	
	public static Minecraft mc;
	
	public static void onStatListInit(boolean blocksInitialized, boolean itemsInitialized) {
		if(blocksInitialized && itemsInitialized) {
			StackableFoodMod.init();
		}
	}
	
	public static void onItemFoodRightClick(ItemFood itemFood, ItemStack itemStack, World world, EntityPlayer player, CallbackInfo callbackInfo) {
		if(itemFood.slowHeal) {
			if(player.health >= 20) {
				callbackInfo.setCancelled(true);
				callbackInfo.setReturnValue(itemStack);
				
				return;
			}
			
			player.remainingRegen = Math.max(player.remainingRegen, itemFood.healAmount);
			player.regenCooldown = 0;
			
			itemStack.stackSize--;
			
			callbackInfo.setCancelled(true);
			callbackInfo.setReturnValue(itemStack);
		}
	}
	
	public static void onTickPlayer(EntityPlayer player) {
		if(player.remainingRegen > 0) {
			if(player.regenCooldown > 0) {
				player.regenCooldown--;
			}else {
				player.regenCooldown = StackableFoodMod.healDelay;
				player.remainingRegen--;
				if(player.health < 20) {
					if(!player.world.isClientSide) {
						player.health++;
					}
				}
			}
		}else {
			if(player.regenCooldown > 0) {
				player.regenCooldown--;
			}
		}
	}
	
	public static int getHealthbarIconOffset(int i) {
		if(mc == null) {
			mc = Minecraft.getMinecraft(Minecraft.class);
			if(mc == null) {
				throw new NullPointerException("Minecraft instance is null!");
			}
		}
		if(mc.thePlayer == null) {
			return 0;
		}
		if(mc.thePlayer.remainingRegen == 0) {
			return 0;
		}
		if(10 - i == mc.thePlayer.regenCooldown) {
			return 2;
		}
		return 0;
	}

}
