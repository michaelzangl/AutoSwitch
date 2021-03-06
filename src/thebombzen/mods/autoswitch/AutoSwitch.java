package thebombzen.mods.autoswitch;

import java.util.Arrays;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import org.lwjgl.input.Keyboard;

import thebombzen.mods.autoswitch.configuration.Configuration;
import thebombzen.mods.autoswitch.configuration.ToolSelectionMode;
import thebombzen.mods.thebombzenapi.ThebombzenAPI;
import thebombzen.mods.thebombzenapi.ThebombzenAPIBaseMod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The main AutoSwitch mod
 * 
 * @author thebombzen
 */
@SideOnly(Side.CLIENT)
@Mod(modid = "autoswitch", name = "AutoSwitch", version = "4.4.0pre7", dependencies = "required-after:thebombzenapi", guiFactory = "thebombzen.mods.autoswitch.configuration.ConfigGuiFactory")
public class AutoSwitch extends ThebombzenAPIBaseMod {

	public static final int STAGE_H0 = 0;
	public static final int STAGE_SWITCHED = 1;
	public static final int STAGE_CANCELED = 2;
	public static final Minecraft mc = Minecraft.getMinecraft();

	private int entityAttackStage = STAGE_H0;

	private EntityLivingBase entitySwitchedOn = null;

	private Configuration configuration;

	private boolean prevMouseDown = false;
	private boolean prevPulse = false;
	private int prevtool = 0;
	private boolean pulseOn = false;
	private boolean switchback = false;
	private boolean treefellerOn = false;

	@Instance(value = "autoswitch")
	public static AutoSwitch instance;
	
	@SubscribeEvent
	public void clientChat(ClientChatReceivedEvent event){
		String text = event.message.getUnformattedText();
		if (text.equals("**YOU READY YOUR AXE**")){
			treefellerOn = true;
		} else if (text.matches("\\*\\*YOU READY YOUR [A-Z]+\\*\\*")) {
			treefellerOn = false;
		} else if (text.equals("**YOU LOWER YOUR AXE**")){
			treefellerOn = false;
		} else if (text.equals("**Tree Feller has worn off**")){
			treefellerOn = false;
		} else if (text.equals("YOUR AXE SPLINTERS INTO DOZENS OF PIECES!")){
			treefellerOn = false;
		} else if (text.matches("^You are too tired to use that ability again. \\(\\d+s\\)$")){
			treefellerOn = false;
		}
		//debug("treefellerOn: %b", treefellerOn);
	}
	
	@SubscribeEvent(priority=EventPriority.LOW)
	public void clientTick(ClientTickEvent event) {

		if (!event.phase.equals(Phase.START)) {
			return;
		}
		
		if (mc.theWorld == null) {
			return;
		}
		
		if (ThebombzenAPI.hasWorldChanged()){
			treefellerOn = false;
		}

		if (entityAttackStage == STAGE_CANCELED) {
			mc.thePlayer.swingItem();
			mc.playerController.attackEntity(mc.thePlayer, entitySwitchedOn);
			entityAttackStage = STAGE_H0;
			entitySwitchedOn = null;
			return;
		}

		pulseOn = Keyboard.isKeyDown(configuration.getKeyCodeProperty(Configuration.PULSE_KEY));
		// func_151463_i() == getKeyCode()
		boolean mouseDown = mc.gameSettings.keyBindAttack.getIsKeyPressed();
		if (!mouseDown && prevMouseDown || mouseDown && pulseOn ^ prevPulse) {
			switchBack();
		}
		if (mouseDown && !prevMouseDown || mouseDown && pulseOn ^ prevPulse) {
			prevtool = mc.thePlayer.inventory.currentItem;
		}
		if (mouseDown) {
			if (mc.objectMouseOver != null
					&& mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
				potentiallySwitchTools(mc.theWorld, mc.objectMouseOver.blockX,
						mc.objectMouseOver.blockY, mc.objectMouseOver.blockZ);
			} else if (mc.objectMouseOver != null
					&& mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY
					&& mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
				potentiallySwitchWeapons((EntityLivingBase) mc.objectMouseOver.entityHit);
			}
		}
		prevMouseDown = mouseDown;
		prevPulse = pulseOn;
	}
	
	private void debug(String string) {
		debug("%s", string);
	}

	private void debug(String format, Object... args) {
		if (configuration.getBooleanProperty(Configuration.DEBUG)) {
			forceDebug(format, args);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public String getDownloadLocationURLString() {
		return "http://is.gd/ThebombzensMods#AutoSwitch";
	}

	@Override
	public String getLongName() {
		return "AutoSwitch";
	}

	@Override
	public String getLongVersionString() {
		return "AutoSwitch, version 4.4.0pre7, Minecraft 1.7.2";
	}

	@Override
	public int getNumToggleKeys() {
		return 1;
	}

	@Override
	public String getShortName() {
		return "AS";
	}

	@Override
	protected String getToggleMessageString(int index, boolean enabled) {
		if (enabled) {
			return "AutoSwitch is now enabled.";
		} else {
			return "AutoSwitch is now disabled.";
		}
	}

	@Override
	protected String getVersionFileURLString() {
		//return "https://dl.dropboxusercontent.com/u/51080973/Mods/AutoSwitch/ASVersion.txt";
		return "";
	}

	@Override
	public void init1(FMLPreInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
		configuration = new Configuration(this);
		FMLCommonHandler.instance().findContainerFor(this).getMetadata().authorList = Arrays.asList("Thebombzen");
	}

	public boolean isToolBetter(ItemStack newItemStack, ItemStack oldItemStack,
			World world, int x, int y, int z) {

		Block block = world.getBlock(x, y, z);

		if (block == null || block.isAir(world, x, y, z)) {
			return false;
		}

		int metadata = world.getBlockMetadata(x, y, z);

		// int newItemID = newItemStack == null ? 0 : newItemStack.itemID;
		// int olditemID = oldItemStack == null ? 0 : oldItemStack.itemID;

		// long time1 = System.nanoTime();

		float newStr = Tests.getDigSpeed(newItemStack, block, metadata);
		float oldStr = Tests.getDigSpeed(oldItemStack, block, metadata);
		float newBlockStr = Tests
				.getBlockStrength(newItemStack, world, x, y, z);
		float oldBlockStr = Tests
				.getBlockStrength(oldItemStack, world, x, y, z);

		if (newBlockStr == 0.0F && oldBlockStr == 0.0F) {
			debug("Not switching because block is unbreakable by either item.");
			return false;
		}

		int newHarvest = Tests.getHarvestLevel(newItemStack, block,
				metadata);
		int oldHarvest = Tests.getHarvestLevel(oldItemStack, block,
				metadata);
		
		debug("newBlockStr: %f, oldBlockStr %f", newBlockStr, oldBlockStr);
		debug("newHarvest: %d, oldHarvest: %d", newHarvest, oldHarvest);
		debug("newStrength: %f, oldStrength: %f", newStr, oldStr);

		float newEff = Tests.getEff(newStr, newItemStack);
		float oldEff = Tests.getEff(oldStr, oldItemStack);
		debug("newEff: %f, oldEff: %f", newEff, oldEff);
		
		ToolSelectionMode toolSelectionMode = configuration.getToolSelectionMode(block, metadata);
		debug("Tool Selection Mode: %s", toolSelectionMode.toString());
		
		int newStandard = Tests.getToolStandardness(newItemStack, world,
					x, y, z);
		int oldStandard = Tests.getToolStandardness(oldItemStack, world,
					x, y, z);
		
		
		debug("newStandard: %d, oldStandard: %d", newStandard, oldStandard);

		boolean newDamageable = Tests.isItemStackDamageableOnBlock(
				newItemStack, world, x, y, z);
		boolean oldDamageable = Tests.isItemStackDamageableOnBlock(
				oldItemStack, world, x, y, z);
		debug("newDamageable: %b, oldDamageable: %b", newDamageable,
				oldDamageable);
		
		int blockStrComparison = Float.compare(newBlockStr, oldBlockStr);
		
		if (toolSelectionMode.isStandard() || (Math.abs(newStandard) >= 4 && Math.abs(oldStandard) < 4) || (Math.abs(newStandard) < 4 && Math.abs(oldStandard) >= 4)) {
			if (newStandard > oldStandard) {
				debug("Switching because new item is more standard than old.");
				return true;
			} else if (oldStandard > newStandard) {
				debug("Not switching because old item is more standard than new.");
				return false;
			} else {
				if (newStandard <= 0 && oldStandard <= 0) {
					if (newDamageable && !oldDamageable) {
						debug("Not switching because new tool is damageable and old isn't, and neither are standard.");
						return false;
					} else if (oldDamageable && !newDamageable) {
						debug("Switching because old tool is damageable and new isn't, and neither are standard.");
						return true;
					}
				}
			}
		} else {
			if (toolSelectionMode.isFast()) {
				if (blockStrComparison > 0) {
					debug("Switching because new tool is stronger.");
					return true;
				} else if (blockStrComparison < 0) {
					debug("Not switching because old tool is stronger.");
					return false;
				}
			} else {
				if (blockStrComparison < 0) {
					debug("Switching because new item is worse than old item and SLOW STANDARD is on.");
					return true;
				} else if (blockStrComparison > 0) {
					debug("Not switching because new item is better than old item and SLOW STANDARD is on.");
					return false;
				}
			}
		}

		boolean silkWorks = Tests.doesSilkTouchWorkOnBlock(world, x, y, z);
		boolean newHasSilk = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.silkTouch.effectId, newItemStack) > 0;
		boolean oldHasSilk = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.silkTouch.effectId, oldItemStack) > 0;
		
		if (configuration.shouldIgnoreSilkTouch(block, metadata)){
			debug("Ignoring Silk Touch.");
		} else {
			debug("silkWorks: %b, newHasSilk: %b, oldHasSilk: %b", silkWorks,
					newHasSilk, oldHasSilk);
			if (newHasSilk && !oldHasSilk) {
				if (silkWorks) {
					debug("Switching because new has silk touch and old doesn't, and new works.");
					return true;
				} else {
					if (oldStandard > 0) {
						debug("Not switching because new has silk touch and old doesn't, and old replaces new.");
						return false;
					} else if (newStandard <= 0) {
						debug("Not switching because new has silk touch and old doesn't, and new is weak.");
						return false;
					}
				}
			} else if (oldHasSilk && !newHasSilk) {
				if (silkWorks) {
					debug("Not switching because old has silk touch and new doesn't, and old works.");
					return false;
				} else {
					if (newStandard > 0) {
						debug("Switching because old has silk touch and new doesn't, and new replaces old.");
						return true;
					} else if (oldStandard <= 0) {
						debug("Switching because old has silk touch and new doesn't, and old is weak.");
						return true;
					}
				}
			}
		}
	

		boolean fortuneWorks = Tests.doesFortuneWorkOnBlock(world, x, y, z);
		int newFortuneLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.fortune.effectId, newItemStack);
		int oldFortuneLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.fortune.effectId, oldItemStack);


		if (configuration.shouldIgnoreFortune(block, metadata)){
			debug("Ignoring Fortune.");
		} else {
			debug("fortuneWorks: %b, newFortuneLevel: %d, oldFortuneLevel: %d",
					fortuneWorks, newFortuneLevel, oldFortuneLevel);
			if (newFortuneLevel > oldFortuneLevel) {
				if (fortuneWorks) {
					debug("Switching because new fortune is more than old, and new works.");
					return true;
				} else {
					if (oldStandard > 0) {
						debug("Not switching because new fortune is more than old, and old replaces new.");
						return false;
					} else if (newStandard <= 0) {
						debug("Not switching because new fortune is more than old, and new is weak.");
						return false;
					}
				}
			} else if (oldFortuneLevel > newFortuneLevel) {
				if (fortuneWorks) {
					debug("Not switching because old fortune is more than new, and old works.");
					return false;
				} else {
					if (newStandard > 0) {
						debug("Switching because old fortune is more than new, and new replaces old.");
						return true;
					} else if (oldStandard <= 0) {
						debug("Switching because old fortune is more than new, and old is weak.");
						return true;
					}
				}
			}
		}

		if (toolSelectionMode.isFast()) {
			if (blockStrComparison > 0) {
				debug("Switching because new tool is stronger.");
				return true;
			} else if (blockStrComparison < 0) {
				debug("Not switching because old tool is stronger.");
				return false;
			}
		} else {
			if (blockStrComparison < 0) {
				debug("Switching because new item is worse than old item and SLOW STANDARD is on.");
				return true;
			} else if (blockStrComparison > 0) {
				debug("Not switching because new item is better than old item and SLOW STANDARD is on.");
				return false;
			}
		}

		Set<Enchantment> bothItemsEnchantments = Tests
				.getNonstandardNondamageEnchantmentsOnBothStacks(newItemStack,
						oldItemStack);

		for (Enchantment enchantment : bothItemsEnchantments) {
			int oldLevel = EnchantmentHelper.getEnchantmentLevel(
					enchantment.effectId, oldItemStack);
			int newLevel = EnchantmentHelper.getEnchantmentLevel(
					enchantment.effectId, newItemStack);
			if (newLevel > oldLevel) {
				debug("Switching because new %s level, %d, is more than old, %d.",
						enchantment.getName(), newLevel, oldLevel);
				return true;
			} else if (newLevel < oldLevel) {
				debug("Switching because old %s level, %d, is more than new, %d.",
						enchantment.getName(), oldLevel, newLevel);
				return false;
			}
		}

		if (newDamageable && !oldDamageable) {
			debug("Not switching because new tool is damageable and old isn't.");
			return false;
		} else if (oldDamageable && !newDamageable) {
			debug("Switching because old tool is damageable and new isn't.");
			return true;
		}

		if (newDamageable && oldDamageable) {
			if (newFortuneLevel > oldFortuneLevel) {
				debug("Not switching because new fortune is bad and items are damageable.");
				return false;
			} else if (oldFortuneLevel > newFortuneLevel) {
				debug("Switching because old fortune is bad and items are damageable.");
				return true;
			}

			int newUnbreakingLevel = EnchantmentHelper.getEnchantmentLevel(
					Enchantment.unbreaking.effectId, newItemStack);
			int oldUnbreakingLevel = EnchantmentHelper.getEnchantmentLevel(
					Enchantment.unbreaking.effectId, oldItemStack);

			if (newUnbreakingLevel > oldUnbreakingLevel) {
				debug("Switching because new unbreaking is more than old unbreaking.");
				return true;
			} else if (oldUnbreakingLevel > newUnbreakingLevel) {
				debug("Not switching because old unbreaking is more than new unbreaking.");
				return false;
			}
		}

		debug("Not switching because tools are equal.");
		return false;

	}

	public boolean isTreefellerOn(){
		return treefellerOn;
	}

	public boolean isWeaponBetter(ItemStack newItemStack,
			ItemStack oldItemStack, EntityLivingBase entityover) {

		boolean isPlayer = entityover instanceof EntityPlayer;

		double oldDamage = Tests.getItemStackDamage(oldItemStack);
		double newDamage = Tests.getItemStackDamage(newItemStack);

		oldDamage += Tests.getEnchantmentModifierLiving(oldItemStack,
				entityover);
		newDamage += Tests.getEnchantmentModifierLiving(newItemStack,
				entityover);

		debug("Old damage is %f, new damage is %f.", oldDamage, newDamage);

		if (isPlayer) {
			if (newDamage > oldDamage) {
				debug("Switching because new damage is more.");
				return true;
			} else if (newDamage < oldDamage) {
				debug("Not switching because old damage is more.");
				return false;
			}
		} else {

			int oldHits;
			int newHits;

			if (oldDamage == 0) {
				oldHits = Integer.MAX_VALUE;
			} else {
				oldHits = MathHelper.ceiling_double_int(entityover.getMaxHealth()
						/ oldDamage);
			}

			if (newDamage == 0) {
				newHits = Integer.MAX_VALUE;
			} else {
				newHits = MathHelper.ceiling_double_int(entityover.getMaxHealth()
						/ newDamage);
			}

			debug("Old hits are %d, new hits are %d", oldHits, newHits);

			if (newHits < oldHits) {
				debug("Switching because new hits are fewer.");
				return true;
			} else if (newHits > oldHits) {
				debug("Not switching because old hits are fewer.");
				return false;
			}

		}

		int newLootingLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.looting.effectId, newItemStack);
		int newFireAspectLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.fireAspect.effectId, newItemStack);
		int newKnockbackLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.knockback.effectId, newItemStack);
		int newUnbreakingLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.unbreaking.effectId, newItemStack);

		int oldLootingLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.looting.effectId, oldItemStack);
		int oldFireAspectLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.fireAspect.effectId, oldItemStack);
		int oldKnockbackLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.knockback.effectId, oldItemStack);
		int oldUnbreakingLevel = EnchantmentHelper.getEnchantmentLevel(
				Enchantment.unbreaking.effectId, oldItemStack);

		if (!isPlayer) {
			if (newLootingLevel > oldLootingLevel) {
				debug("Switching because new looting, %d, is more than old, %d.",
						newLootingLevel, oldLootingLevel);
				return true;
			} else if (oldLootingLevel > newLootingLevel) {
				debug("Not switching because old looting, %d, is more than new, %d.",
						oldLootingLevel, newLootingLevel);
				return false;
			}
		}

		if (newFireAspectLevel > oldFireAspectLevel) {
			debug("Switching because new fire aspect, %d, is more than old, %d.",
					newFireAspectLevel, oldFireAspectLevel);
			return true;
		} else if (oldFireAspectLevel > newFireAspectLevel) {
			debug("Not switching because old fire aspect, %d, is more than new, %d.",
					oldFireAspectLevel, newFireAspectLevel);
			return false;
		}

		if (newKnockbackLevel > oldKnockbackLevel) {
			debug("Switching because new knockback, %d, is more than old, %d.",
					newKnockbackLevel, oldKnockbackLevel);
			return true;
		} else if (oldKnockbackLevel > newKnockbackLevel) {
			debug("Not switching because old knockback, %d, is more than new, %d.",
					oldKnockbackLevel, newKnockbackLevel);
			return false;
		}

		Set<Enchantment> bothItemsEnchantments = Tests
				.getNonstandardNondamageEnchantmentsOnBothStacks(newItemStack,
						oldItemStack);

		for (Enchantment enchantment : bothItemsEnchantments) {
			int oldLevel = EnchantmentHelper.getEnchantmentLevel(
					enchantment.effectId, oldItemStack);
			int newLevel = EnchantmentHelper.getEnchantmentLevel(
					enchantment.effectId, newItemStack);
			if (newLevel > oldLevel) {
				debug("Switching because new %s level, %d, is more than old, %d.",
						enchantment.getName(), newLevel, oldLevel);
				return true;
			} else if (newLevel < oldLevel) {
				debug("Switching because old %s level, %d, is more than new, %d.",
						enchantment.getName(), oldLevel, newLevel);
				return false;
			}
		}

		if (Tests.isSword(newItemStack) && !Tests.isSword(oldItemStack)) {
			debug("Switching because new weapon is sword and old isn't.");
			return true;
		}
		if (Tests.isSword(oldItemStack) && !Tests.isSword(newItemStack)) {
			debug("Not switching because old weapon is sword and new isn't.");
			return false;
		}
		
		if (newDamage > oldDamage) {
			debug("Switching because new damage is more and all else is equal.");
			return true;
		} else if (newDamage < oldDamage) {
			debug("Not switching because old damage is more and all else is equal.");
			return false;
		}

		boolean newDamageable = Tests.isItemStackDamageable(newItemStack);
		boolean oldDamageable = Tests.isItemStackDamageable(oldItemStack);
		debug("newDamageable: %b, oldDamageable: %b", newDamageable,
				oldDamageable);

		if (newDamageable && !oldDamageable) {
			debug("Not switching because new weapon is damageable and old isn't.");
			return false;
		}

		if (oldDamageable && !newDamageable) {
			debug("Switching because new weapon is not damageable and old is.");
			return true;
		}

		if (newDamageable && oldDamageable
				&& newUnbreakingLevel > oldUnbreakingLevel) {
			debug("Switching because new unbreaking, %d, is more than old, %d.",
					newUnbreakingLevel, oldUnbreakingLevel);
			return true;
		} else if (newDamageable && oldDamageable
				&& oldUnbreakingLevel > newUnbreakingLevel) {
			debug("Not switching because old unbreaking, %d, is more than new, %d.",
					oldUnbreakingLevel, newUnbreakingLevel);
			return false;
		}

		if (newItemStack == null && oldItemStack != null) {
			debug("Switching because new tool is fist and old is useless.");
			return true;
		} else if (oldItemStack == null && newItemStack != null) {
			debug("Not switching because old tool is fist and new is useless.");
			return false;
		}

		debug("Not switching because weapons are equal.");
		return false;
	}

	@SubscribeEvent
	public void onEntityAttack(AttackEntityEvent event) {
		if (!event.entity.worldObj.isRemote) {
			return;
		}
		if (entityAttackStage == STAGE_SWITCHED
				&& entitySwitchedOn == event.target) {
			entityAttackStage = STAGE_CANCELED;
			event.setCanceled(true);
		} else if (entityAttackStage != STAGE_CANCELED) {
			entitySwitchedOn = null;
			entityAttackStage = STAGE_H0;
		}
	}

	public boolean potentiallySwitchTools(World world, int x, int y, int z) {
		if (pulseOn == isToggleEnabled(0)
				|| mc.thePlayer.capabilities.isCreativeMode
				&& !configuration
						.getBooleanProperty(Configuration.USE_IN_CREATIVE)
				|| mc.currentScreen != null || !configuration.getSingleMultiProperty(Configuration.BLOCKS)) {
			return false;
		}
		debug("====================================================");
		debug(getLongVersionString());
		debug("Switching on block, x = %d, y = %d, z = %d", x, y, z);
		try {
			switchToBestTool(mc.theWorld, x, y, z);
			return true;
		} catch (Throwable e) {
			throwException("Error switching tools", e, false);
			return false;
		}
	}

	public boolean potentiallySwitchWeapons(EntityLivingBase entity) {
		// System.out.println("Here!");
		if (pulseOn == isToggleEnabled(Configuration.DEFAULT_ENABLED.getDefaultToggleIndex())
				|| mc.thePlayer.capabilities.isCreativeMode
				&& !configuration
						.getBooleanProperty(Configuration.USE_IN_CREATIVE)
				|| mc.currentScreen != null || !configuration.getSingleMultiProperty(Configuration.MOBS)) {
			return false;
		}
		debug("====================================================");
		debug(getLongVersionString());
		debug("Switching on an entity, %s", entity.toString());
		try {
			entitySwitchedOn = entity;
			entityAttackStage = STAGE_SWITCHED;
			switchToBestWeapon(mc.thePlayer, entity);
			return true;
		} catch (Throwable e) {
			throwException("Error switching weapons", e, false);
			return false;
		}
	}

	private void switchBack() {
		if (switchback) {
			mc.thePlayer.inventory.currentItem = prevtool;
			switchback = false;
			debug("Switching tools back to %d", prevtool);
		}
	}

	private void switchToBestTool(World world, int x, int y, int z) {

		Block block = world.getBlock(x, y, z);
		UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);

		String name = String.format("%s:%s", id.modId, id.name);

		debug("Testing vs block %s", name);
		String[] names = new String[9];
		for (int i = 0; i < 9; i++) {
			if (mc.thePlayer.inventory.mainInventory[i] == null) {
				names[i] = "null";
			} else {
				UniqueIdentifier itemID = GameRegistry
						.findUniqueIdentifierFor(mc.thePlayer.inventory.mainInventory[i]
								.getItem());
				names[i] = String.format("%s:%s", itemID.modId, itemID.name);
			}
			debug("Hotbar slot %d contains item %s", i, names[i]);
		}

		int currentBest = prevtool;

		debug("Block hardness is %f", Tests.getBlockHardness(world, x, y, z));

		for (int i = 0; i < 9; i++) {

			if (i == currentBest) {
				continue;
			}

			debug("Checking if tool %d, which is %s, is better than %d, which is %s",
					i, names[i], currentBest, names[currentBest]);
			if (isToolBetter(mc.thePlayer.inventory.mainInventory[i],
					mc.thePlayer.inventory.mainInventory[currentBest], world,
					x, y, z)) {
				debug("Changing possible best tool.");
				currentBest = i;
			}
		}
		debug("Current best is %d, which is %s", currentBest,
				names[currentBest]);
		switchToolsToN(currentBest);
		if (configuration.getSingleMultiProperty(Configuration.SWITCHBACK_BLOCKS)){
			switchback = true;
		} else {
			prevtool = currentBest;
		}
	}

	private void switchToBestWeapon(EntityPlayer entityplayer,
			EntityLivingBase entityover) {

		ItemStack[] inventory = entityplayer.inventory.mainInventory;

		String[] names = new String[9];
		for (int i = 0; i < 9; i++) {
			if (mc.thePlayer.inventory.mainInventory[i] == null) {
				names[i] = "null";
			} else {
				UniqueIdentifier itemID = GameRegistry
						.findUniqueIdentifierFor(mc.thePlayer.inventory.mainInventory[i]
								.getItem());
				names[i] = String.format("%s:%s", itemID.modId, itemID.name);
			}
			debug("Hotbar slot %d contains item %s", i, names[i]);
		}

		int currentBest = prevtool;

		debug("Current item is %d", entityplayer.inventory.currentItem);
		debug("Setting possible best weapon to %d, which is %s", currentBest,
				names[currentBest]);

		for (int i = 0; i < 9; i++) {
			debug("Checking if weapon %d, which is %s, is better than %d, which is %s",
					i, names[i], currentBest, names[currentBest]);
			if (isWeaponBetter(inventory[i], inventory[currentBest], entityover)) {
				debug("Changing possible best weapon because weapon is better.");
				currentBest = i;
			}
		}
		switchToolsToN(currentBest);
		if (configuration.getSingleMultiProperty(Configuration.SWITCHBACK_MOBS)){
			switchback = true;
		} else {
			prevtool = currentBest;
		}
	}

	private void switchToolsToN(int n) {
		EntityPlayer entityplayer = mc.thePlayer;
		entityplayer.inventory.currentItem = n;
		String name;
		if (entityplayer.inventory.mainInventory[n] == null) {
			name = "Nothing";
		} else {
			UniqueIdentifier id = GameRegistry
					.findUniqueIdentifierFor(entityplayer.inventory.mainInventory[n]
							.getItem());
			name = id.modId + ":" + id.name;
		}
		debug("Switching tools to %d, which is %s", n, name);
	}

}
