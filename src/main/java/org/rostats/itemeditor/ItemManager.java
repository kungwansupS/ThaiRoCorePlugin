package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    public String getRelativePath(File file) {
        String rootPath = rootDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.equals(rootPath)) return "/";
        if (filePath.startsWith(rootPath)) {
            String rel = filePath.substring(rootPath.length()).replace("\\", "/");
            if (rel.startsWith("/")) return rel;
            return "/" + rel;
        }
        return "/" + file.getName();
    }

    public File getFileFromRelative(String relativePath) {
        if (relativePath == null || relativePath.equals("/") || relativePath.isEmpty()) return rootDir;
        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
        return new File(rootDir, relativePath);
    }

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
            config.set("folder", parent.getName());
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveItem(File file, ItemAttribute attr, ItemStack itemStack) {
        YamlConfiguration config = new YamlConfiguration();

        config.set("material", itemStack.getType().name());
        if (itemStack.hasItemMeta()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta.hasDisplayName()) {
                config.set("name", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                List<String> fullLore = meta.getLore();
                List<String> manualLore = new ArrayList<>();
                String header = "§f§l--- Item Stats ---";

                for (String line : fullLore) {
                    if (line.equals(header)) break;
                    manualLore.add(line);
                }

                if (!manualLore.isEmpty()) {
                    String last = manualLore.get(manualLore.size() - 1);
                    if (last.trim().isEmpty()) {
                        manualLore.remove(manualLore.size() - 1);
                    }
                }

                config.set("lore", manualLore);
            }

            // Save Enchantments
            if (meta.hasEnchants()) {
                ConfigurationSection enchSec = config.createSection("enchantments");
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    enchSec.set(entry.getKey().getName(), entry.getValue());
                }
            }
        }

        if (attr.getCustomModelData() != null && attr.getCustomModelData() != 0) {
            config.set("custom-model-data", attr.getCustomModelData());
        }

        config.set("remove-vanilla", attr.isRemoveVanillaAttribute());
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

        plugin.getItemAttributeManager().applyMetaFromConfig(item, config);

        // Load Enchantments manually here as they are part of Meta/Item logic
        if (config.contains("enchantments")) {
            ItemMeta meta = item.getItemMeta();
            ConfigurationSection enchSec = config.getConfigurationSection("enchantments");
            for (String key : enchSec.getKeys(false)) {
                Enchantment ench = Enchantment.getByName(key);
                if (ench != null) {
                    meta.addEnchant(ench, enchSec.getInt(key), true);
                }
            }
            item.setItemMeta(meta);
        }

        plugin.getItemAttributeManager().applyAttributesToItem(item, attr);

        return item;
    }

    public void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File sub : contents) deleteFile(sub);
            }
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

        File dest = new File(file.getParentFile(), name + "_copy.yml");
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