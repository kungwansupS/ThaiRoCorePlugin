package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.rostats.ThaiRoCorePlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ItemManager {

    private final ThaiRoCorePlugin plugin;
    private final File rootDir;

    public ItemManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.rootDir = new File(plugin.getDataFolder(), "items");
        if (!rootDir.exists()) rootDir.mkdirs();
    }

    public File getRootDir() {
        return rootDir;
    }

    // List contents (folders and .yml files)
    public List<File> listContents(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return new ArrayList<>();
        return Arrays.stream(files)
                .sorted((f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareTo(f2.getName());
                })
                .collect(Collectors.toList());
    }

    public void createFolder(File parent, String name) {
        File newFolder = new File(parent, name);
        if (!newFolder.exists()) newFolder.mkdirs();
    }

    public void createItem(File parent, String fileName, Material material) {
        File file = new File(parent, fileName.endsWith(".yml") ? fileName : fileName + ".yml");
        if (file.exists()) return;

        try {
            file.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("material", material.name());
            config.set("folder", parent.getName()); // Meta info
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveItem(File file, ItemAttribute attr, ItemStack itemStack) {
        YamlConfiguration config = new YamlConfiguration();

        // Basic Info
        config.set("material", itemStack.getType().name());
        if (itemStack.hasItemMeta()) {
            if (itemStack.getItemMeta().hasDisplayName()) {
                config.set("name", itemStack.getItemMeta().getDisplayName()); // Stored with color codes
            }
            if (itemStack.getItemMeta().hasLore()) {
                config.set("lore", itemStack.getItemMeta().getLore());
            }
        }

        config.set("remove-vanilla", attr.isRemoveVanillaAttribute());

        // Attributes Map
        attr.saveToConfig(config.createSection("attributes"));

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ItemAttribute loadAttribute(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return ItemAttribute.fromConfig(config);
    }

    public ItemStack loadItemStack(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String matName = config.getString("material", "STONE");
        Material mat = Material.getMaterial(matName);
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemAttribute attr = loadAttribute(file);

        // Apply Name & Lore if exists in file
        plugin.getItemAttributeManager().applyMetaFromConfig(item, config);

        // Apply attributes to PDC
        plugin.getItemAttributeManager().applyAttributesToItem(item, attr);

        return item;
    }

    public void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File sub : file.listFiles()) deleteFile(sub);
        }
        file.delete();
    }

    public void renameFile(File file, String newName) {
        String finalName = file.isDirectory() ? newName : (newName.endsWith(".yml") ? newName : newName + ".yml");
        File dest = new File(file.getParentFile(), finalName);
        file.renameTo(dest);
    }

    public void duplicateItem(File file) {
        if (file.isDirectory()) return;
        String name = file.getName().replace(".yml", "");
        String newName = name + "_copy.yml";
        File dest = new File(file.getParentFile(), newName);
        int i = 1;
        while(dest.exists()) {
            dest = new File(file.getParentFile(), name + "_copy_" + i + ".yml");
            i++;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try {
            config.save(dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}