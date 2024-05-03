package b100.stackablefood;

import java.io.File;

import b100.json.JsonParser;
import b100.json.element.JsonEntry;
import b100.json.element.JsonObject;
import b100.utils.StringUtils;
import net.minecraft.core.Global;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemFood;

public class StackableFoodMod {
	
	public static int healDelay = 20;
	
	private static boolean initialized = false;
	
	public static void init() {
		if(initialized || Global.accessor == null) {
			return;
		}
		
		print("Init");
		
		File minecraftDirectory = Global.accessor.getMinecraftDir();
		File configDirectory = new File(minecraftDirectory, "config");
		
		if(!configDirectory.exists()) {
			configDirectory.mkdirs();
		}
		
		File configFile = new File(configDirectory, "stackablefood.json");
		if(!configFile.exists()) {
			try{
				createDefaultConfig(configFile);
			}catch (Exception e) {
				throw new RuntimeException("Error while creating default config: '" + configFile.getAbsolutePath() + "'!", e);
			}
		}
		
		try {
			loadConfig(configFile);
		}catch (Exception e) {
			throw new RuntimeException("Error while loading config: '" + configFile.getAbsolutePath() + "'!", e);
		}
		
		initialized = true;
	}
	
	public static void createDefaultConfig(File configFile) {
		print("Creating default config: '" + configFile.getAbsolutePath() + "'");
		JsonObject root = new JsonObject();
		
		for(int i=0; i < Item.itemsList.length; i++) {
			Item item = Item.itemsList[i];
			if(item == null) {
				continue;
			}
			
			int newStackSize;
			boolean instantHeal;
			
			if(item == Item.foodAppleGold) {
				newStackSize = 16;
				instantHeal = true;
			}else if(item == Item.foodPorkchopCooked || item == Item.foodPorkchopRaw || item == Item.foodFishRaw || item == Item.foodFishCooked) {
				newStackSize = 1;
				instantHeal = true;
			}else if(item instanceof ItemFood) {
				ItemFood itemFood = (ItemFood) item;
				
				newStackSize = StackableFoodMod.getStackSizeForHealAmount(itemFood.healAmount);
				instantHeal = false;
			}else {
				continue;
			}
			
			JsonObject obj = new JsonObject();
			obj.set("instantHeal", instantHeal);
			obj.set("stackSize", newStackSize);
			root.set(String.valueOf(item.id), obj);
		}
		
		StringUtils.saveStringToFile(configFile, root.toString());
	}
	
	public static void loadConfig(File configFile) {
		JsonObject config = JsonParser.instance.parseFileContent(configFile);
		
		for(int i=0; i < config.entryList().size(); i++) {
			JsonEntry entry = config.entryList().get(i);
			
			int id = Integer.parseInt(entry.name);
			JsonObject object = entry.value.getAsObject();
			
			boolean instantHeal = object.getBoolean("instantHeal");
			int stackSize = object.getInt("stackSize");
			
			Item item = Item.itemsList[id];
			
			if(stackSize < 1 || stackSize > 64) {
				print("Invalid stack size " + stackSize + " for item " + toString(item) + "!");
				continue;
			}
			
			if(item == null) {
				print("Item " + id + " does not exist!");
				continue;
			}
			
			if(item instanceof ItemFood) {
				print("Change stack size of item " + toString(item) + " to " + stackSize);
				
				ItemFood itemFood = (ItemFood) item;
				itemFood.setMaxStackSize(stackSize);
				itemFood.slowHeal = !instantHeal;
			}else {
				print("Item " + toString(item) + " is not a food item!");
				continue;
			}
		}
	}
	
	public static int getStackSizeForHealAmount(int healAmount) {
		if(healAmount >= 8) {
			return 1;
		}
		if(healAmount >= 6) {
			return 2;
		}
		if(healAmount >= 4) {
			return 4;
		}
		if(healAmount >= 2) {
			return 8;
		}
		return 16;
	}
	
	public static void print(String string) {
		System.out.print("[StackableFood] " + string + "\n");
	}
	
	public static String toString(Item item) {
		return item.id + " (" + item.getKey() + ")";
	}

}
