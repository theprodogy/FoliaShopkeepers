package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.tag.DamageTypeTags;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.bukkit.RegistryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * ItemStack definitions for test cases.
 */
public class TestItemStacks {

	// Note: SPIGOT-7571: Use Json-based display names and lore (e.g. via
	// ItemUtils#setDisplayNameAndLore) instead of ItemMeta#setDisplayName / #setLore.

	public static List<? extends @Nullable ItemStack> createAllItemStacks() {
		return Arrays.asList(
				createItemStackNull(),
				createItemStackAir(),
				createItemStackBasic(),
				createItemStackBasicWithSize(),
				createItemStackBasicTool(),
				createItemStackDisplayName(),
				createItemStackComplete(),
				// TODO Broken in MC 1.20.5+, until late MC 1.21. See SPIGOT-7857
				// Tested again, still broken. See SPIGOT-8049
				// createItemStackBlockData(),
				createItemStackUncommonMeta(),
				createItemStackWritableBook(),
				createItemStackWrittenBook(),
				// TODO Broken in MC 1.20.5+, until late MC 1.21. See SPIGOT-7857
				// Tested again, still broken. See SPIGOT-8049
				// createItemStackTileEntityDisplayName(),
				createItemStackBasicTileEntity(),
				createItemStackPotion()
		);
	}

	public static @Nullable ItemStack createItemStackNull() {
		return null;
	}

	public static ItemStack createItemStackAir() {
		return new ItemStack(Material.AIR);
	}

	public static ItemStack createItemStackBasic() {
		return new ItemStack(Material.STONE);
	}

	public static ItemStack createItemStackBasicWithSize() {
		return new ItemStack(Material.STONE, 10);
	}

	public static ItemStack createItemStackBasicTool() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		return itemStack;
	}

	public static ItemStack createItemStackDisplayName() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		ItemUtils.setDisplayNameAndLore(itemStack, "{\"text\":\"Custom Name\",\"color\":\"red\"}", null);
		return itemStack;
	}

	public static ItemStack createItemStackComplete() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		ItemUtils.setDisplayNameAndLore(
				itemStack,
				"{\"text\":\"Custom Name\",\"color\":\"red\"}",
				Arrays.asList("{\"text\":\"lore1\",\"color\":\"green\"}", "lore2")
		);
		ItemUtils.setItemName(itemStack, "{\"text\":\"Custom item name\",\"color\":\"red\"}");

		ItemMeta itemMeta = Unsafe.assertNonNull(itemStack.getItemMeta());
		itemMeta.setMaxStackSize(65);
		itemMeta.setRarity(ItemRarity.EPIC);
		itemMeta.setHideTooltip(true);

		var customModelData = itemMeta.getCustomModelDataComponent();
		var customModelDataFloats = new ArrayList<Float>();
		customModelDataFloats.add(1.0f);
		customModelData.setFloats(Unsafe.castNonNull(customModelDataFloats));
		itemMeta.setCustomModelDataComponent(customModelData);

		itemMeta.setDamageResistant(DamageTypeTags.IS_EXPLOSION);
		itemMeta.setUnbreakable(true);
		((Damageable) itemMeta).setDamage(2);
		((Damageable) itemMeta).setMaxDamage(10);
		((Repairable) itemMeta).setRepairCost(3);

		ToolComponent tool = itemMeta.getTool();
		tool.setDefaultMiningSpeed(1.5f);
		tool.setDamagePerBlock(2);
		tool.addRule(Material.STONE, 0.5f, true);
		itemMeta.setTool(tool);

		itemMeta.setEnchantmentGlintOverride(true);
		itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
		itemMeta.addEnchant(Enchantment.SHARPNESS, 2, true);
		itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED,
				new AttributeModifier(
						NamespacedKeyUtils.create("some_plugin", "attack-speed-bonus"),
						2,
						Operation.ADD_NUMBER,
						EquipmentSlotGroup.HAND
				)
		);
		itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED,
				new AttributeModifier(
						NamespacedKeyUtils.create("some_plugin", "attack-speed-bonus-2"),
						0.5,
						Operation.MULTIPLY_SCALAR_1,
						EquipmentSlotGroup.OFFHAND
				)
		);
		itemMeta.addAttributeModifier(Attribute.MAX_HEALTH,
				new AttributeModifier(
						NamespacedKeyUtils.create("some_plugin", "max-health-bonus"),
						2,
						Operation.ADD_NUMBER,
						EquipmentSlotGroup.HAND
				)
		);
		itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

		FoodComponent food = itemMeta.getFood();
		food.setNutrition(2);
		food.setSaturation(2.5f);
		food.setCanAlwaysEat(true);
		itemMeta.setFood(food);

		// TODO Not available on Paper
		/*var consumable = itemMeta.getConsumable();
		consumable.setAnimation(Animation.EAT);
		consumable.setConsumeParticles(true);
		consumable.setConsumeSeconds(5.5f);
		consumable.setSound(Sound.ENTITY_PLAYER_BURP);
		// TODO Not sure how to create consumable effects via the API.
		itemMeta.setConsumable(consumable);*/

		var equippable = itemMeta.getEquippable();
		equippable.setSlot(EquipmentSlot.HEAD);
		// Note: SPIGOT-8104: 1.21.6: Affects the shearing sound during config deserialization. But:
		// Does not affect our custom serialization format.
		equippable.setEquipSound(Sound.ITEM_ARMOR_EQUIP_CHAIN);
		equippable.setModel(RegistryUtils.getKeyOrThrow(Material.DIAMOND_HELMET));
		equippable.setCameraOverlay(RegistryUtils.getKeyOrThrow(Material.CARVED_PUMPKIN));
		equippable.setAllowedEntities(EntityType.PLAYER);
		equippable.setDispensable(false);
		equippable.setSwappable(false);
		equippable.setDamageOnHurt(false);
		equippable.setEquipOnInteract(true);
		// TODO Added in 1.21.6
		// equippable.setCanBeSheared(true);
		// equippable.setShearingSound(Sound.ENTITY_SHEEP_SHEAR);
		itemMeta.setEquippable(equippable);

		var useCooldown = itemMeta.getUseCooldown();
		useCooldown.setCooldownSeconds(1.5f);
		useCooldown.setCooldownGroup(NamespacedKeyUtils.create("plugin", "cooldown"));
		itemMeta.setUseCooldown(useCooldown);

		itemMeta.setUseRemainder(new ItemStack(Material.BONE));

		itemMeta.setEnchantable(15);
		itemMeta.setTooltipStyle(NamespacedKeyUtils.create("plugin", "tooltip-style"));
		itemMeta.setItemModel(NamespacedKeyUtils.create("plugin", "item-model"));
		itemMeta.setGlider(true);

		// Note: This data ends up getting stored in an arbitrary order internally.
		PersistentDataContainer customTags = itemMeta.getPersistentDataContainer();
		customTags.set(
				NamespacedKeyUtils.create("some_plugin", "some-key"),
				PersistentDataType.STRING,
				"some value"
		);
		PersistentDataContainer customContainer = customTags.getAdapterContext().newPersistentDataContainer();
		customContainer.set(
				NamespacedKeyUtils.create("inner_plugin", "inner-key"),
				PersistentDataType.FLOAT,
				0.3F
		);
		customTags.set(
				NamespacedKeyUtils.create("some_plugin", "some-other-key"),
				PersistentDataType.TAG_CONTAINER,
				customContainer
		);
		// TODO MC 1.21.11:
		// - damage type / damage type key
		// - Use effects
		// - Swing animation
		// - Attack range
		// - Piercing weapon
		// - Kinetic weapon
		// - Minimum attack charge
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackBlockData() {
		ItemStack itemStack = new ItemStack(Material.CAMPFIRE);
		BlockDataMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		Campfire blockData = (Campfire) Material.CAMPFIRE.createBlockData();
		blockData.setLit(false);
		itemMeta.setBlockData(blockData);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackUncommonMeta() {
		ItemStack itemStack = new ItemStack(Material.LEATHER_CHESTPLATE);
		LeatherArmorMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		itemMeta.setColor(Color.BLUE);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackWritableBook() {
		ItemStack itemStack = new ItemStack(Material.WRITABLE_BOOK);
		WritableBookMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		itemMeta.setPages(
				"Page 1\nWith empty lines\n\nAnd literal newline \\n and different kinds of quotes like ' and \"!",
				"Page2\n  With multiple lines and whitespace\nAnd §ccolors &a!"
		);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackWrittenBook() {
		ItemStack itemStack = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		itemMeta.setTitle("Finding Diamonds");
		itemMeta.setAuthor("D. Whining Rod");
		itemMeta.setGeneration(Generation.COPY_OF_ORIGINAL);
		itemMeta.setPages(
				"Page 1\nWith empty lines\n\nAnd literal newline \\n and different kinds of quotes like ' and \"!",
				"Page2\n  With multiple lines and whitespace\nAnd §ccolors &a!"
		);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackBasicTileEntity() {
		ItemStack itemStack = new ItemStack(Material.CHEST);
		return itemStack;
	}

	public static ItemStack createItemStackTileEntityDisplayName() {
		ItemStack itemStack = new ItemStack(Material.CHEST);
		ItemUtils.setDisplayNameAndLore(itemStack, "{\"text\":\"Custom Name\",\"color\":\"red\"}", null);
		return itemStack;
	}

	public static ItemStack createItemStackPotion() {
		ItemStack itemStack = new ItemStack(Material.POTION);
		PotionMeta potionMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		potionMeta.setBasePotionType(PotionType.HEALING);
		potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2, 1, true, true, true), true);
		potionMeta.setColor(Color.RED);
		potionMeta.setCustomPotionName("MyPotion");
		// TODO SPIGOT-8103: Additional "unhandled" data, breaks the item comparison.
		// potionMeta.setDurationScale(1.5f);
		itemStack.setItemMeta(potionMeta);
		return itemStack;
	}

	private TestItemStacks() {
	}
}
