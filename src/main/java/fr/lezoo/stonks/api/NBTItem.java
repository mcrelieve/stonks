package fr.lezoo.stonks.api;

import fr.lezoo.stonks.Stonks;
import fr.lezoo.stonks.version.ItemTag;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class NBTItem {
	private final ItemStack item;

	public NBTItem(ItemStack item) {
		this.item = item;
	}

	public ItemStack getItem() {
		return item;
	}

	public abstract String getString(String path);

	public abstract boolean hasTag(String path);

	public abstract boolean getBoolean(String path);

	public abstract double getDouble(String path);

	public abstract int getInteger(String path);

	public abstract NBTItem addTag(List<ItemTag> tags);

	public abstract NBTItem removeTag(String... paths);

	public abstract Set<String> getTags();

	public abstract ItemStack toItem();

	public NBTItem addTag(ItemTag... tags) {
		return addTag(Arrays.asList(tags));
	}

	public static NBTItem get(ItemStack item) {
		return Stonks.plugin.versionWrapper.getNBTItem(item);
	}
}
