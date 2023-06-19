package net.lerariemann.infinity.dimensions;

import net.lerariemann.infinity.util.CommonIO;
import net.lerariemann.infinity.util.ListReader;
import net.lerariemann.infinity.util.WeighedStructure;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.Files.walk;

public class RandomProvider {
    public Map<String, WeighedStructure<String>> registry;
    public Map<String, WeighedStructure<NbtElement>> blockRegistry;
    public Map<String, NbtList> presetRegistry;
    public Map<String, Double> rootChances;
    public WeighedStructure<String> floralDistribution;
    public String configPath;
    public boolean runtimeGenerationEnabled;

    public RandomProvider(String path) {
        configPath = path;
        registry = new HashMap<>();
        blockRegistry = new HashMap<>();
        presetRegistry = new HashMap<>();
        rootChances = new HashMap<>();
        floralDistribution = new WeighedStructure<>();
        register_all();
    }

    void register_all() {
        read_root_config();
        register_category(registry, configPath + "weighed_lists/misc", CommonIO::commonListReader);
        register_category(blockRegistry, configPath + "weighed_lists/blocks", CommonIO::uncommonListReader);
        register_category(registry, configPath + "weighed_lists/mobs", CommonIO::commonListReader);
        register_category(presetRegistry, configPath + "weighed_lists/multinoisepresets", s -> CommonIO.read(s).getList("elements", NbtElement.STRING_TYPE));
    }

    void read_root_config() {
        NbtCompound rootConfig = CommonIO.read(configPath + "infinity.json");
        runtimeGenerationEnabled = rootConfig.getBoolean("runtimeGenerationEnabled");
        NbtCompound rootchances = rootConfig.getCompound("rootChances");
        for (String s: rootchances.getKeys()) {
            rootChances.put(s, rootchances.getDouble(s));
        }
        NbtCompound flora = rootConfig.getCompound("floralDistribution");
        for (String s: flora.getKeys()) {
            floralDistribution.add(s, flora.getDouble(s));
        }
    }

    double chance(String key) {
        return rootChances.get(key);
    }

    static <B> void register_category(Map<String, B> reg, String path, ListReader<B> reader) {
        try {
            walk(Paths.get(path)).forEach(p -> {
                String fullname = p.toString();
                if (fullname.endsWith(".json")) {
                    LogManager.getLogger().info(fullname);
                    String name = fullname.substring(fullname.lastIndexOf("/") + 1, fullname.length() - 5);
                    if (!Objects.equals(name, "none")) reg.put(name, reader.op(fullname));
                }});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    List<String> mobcategories() {
        return registry.get("mob_categories").keys;
    }

    public static boolean weighedRandom(Random random, int weight0, int weight1) {
        int i = random.nextInt(weight0+weight1);
        return i < weight1;
    }

    public static NbtCompound Block(String block) {
        NbtCompound res = new NbtCompound();
        res.putString("Name", block);
        return res;
    }

    public static NbtCompound blockToProvider(NbtCompound block) {
        NbtCompound res = new NbtCompound();
        res.putString("type", "minecraft:simple_state_provider");
        res.put("state", block);
        return res;
    }

    public String randomName(Random random, String key) {
        switch (key) {
            case "all_blocks", "top_blocks", "blocks_features", "full_blocks", "full_blocks_worldgen" -> {
                NbtElement compound = blockRegistry.get(key).getRandomElement(random);
                if (compound instanceof NbtCompound) return ((NbtCompound)compound).getString("Name");
                else return compound.asString();
            }
            default -> {
                return registry.get(key).getRandomElement(random);
            }
        }
    }

    public NbtCompound randomBlock(Random random, String key) {
        switch (key) {
            case "all_blocks", "top_blocks", "blocks_features", "full_blocks", "full_blocks_worldgen" -> {
                NbtElement compound = blockRegistry.get(key).getRandomElement(random);
                if (compound instanceof NbtCompound) return ((NbtCompound)compound);
                else return Block(compound.asString());
            }
            default -> {
                return Block(registry.get(key).getRandomElement(random));
            }
        }
    }

    public NbtCompound randomBlockProvider (Random random, String key) {
        return blockToProvider(randomBlock(random, key));
    }

    static NbtCompound genBounds(Random random, int lbound, int bound) {
        NbtCompound value = new NbtCompound();
        int a = random.nextInt(lbound, bound);
        int b = random.nextInt(lbound, bound);
        value.putInt("min_inclusive", Math.min(a, b));
        value.putInt("max_inclusive", Math.max(a, b));
        return value;
    }

    public static NbtElement intProvider(Random random, int bound, boolean acceptDistributions) {
        return intProvider(random, 0, bound, acceptDistributions);
    }

    public static NbtElement intProvider(Random random, int lbound, int bound, boolean acceptDistributions) {
        int i = random.nextInt(acceptDistributions ? 6 : 4);
        NbtCompound res = new NbtCompound();
        switch(i) {
            case 0 -> {
                res.putString("type", "constant");
                res.putInt("value", random.nextInt(lbound, bound));
                return res;
            }
            case 1, 2 -> {
                res.putString("type", i==1 ? "uniform" : "biased_to_bottom");
                res.put("value", genBounds(random, lbound, bound));
                return res;
            }
            case 4 -> {
                res.putString("type", "clamped");
                NbtCompound value = genBounds(random, lbound, bound);
                value.put("source", intProvider(random, lbound, bound, false));
                res.put("value", value);
                return res;
            }
            case 3 -> {
                res.putString("type", "clamped_normal");
                NbtCompound value = genBounds(random, lbound, bound);
                value.putDouble("mean", lbound + random.nextDouble()*(bound-lbound));
                value.putDouble("deviation", random.nextExponential());
                res.put("value", value);
                return res;
            }
            case 5 -> {
                res.putString("type", "weighted_list");
                int j = 2 + random.nextInt(0, 5);
                NbtList list = new NbtList();
                for (int k=0; k<j; k++) {
                    NbtCompound element = new NbtCompound();
                    element.put("data", intProvider(random, lbound, bound, false));
                    element.putInt("weight", random.nextInt(100));
                    list.add(element);
                }
                res.put("distribution", list);
                return res;
            }
        }
        return res;
    }
}

