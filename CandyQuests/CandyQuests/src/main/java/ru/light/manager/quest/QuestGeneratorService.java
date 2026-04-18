package ru.light.manager.quest;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import ru.light.CandyQuests;
import ru.light.model.Quest;
import ru.light.model.enums.QuestType;

import java.util.*;

public class QuestGeneratorService {
    
    private final CandyQuests plugin;
    private final Random random = new Random();
    
    private static final Set<Material> PASS_RARE_ORES = new HashSet<>(Arrays.asList(
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT,
            Material.COAL, Material.REDSTONE, Material.LAPIS_LAZULI, Material.QUARTZ
    ));

    private static final Set<Material> PLACE_EXPENSIVE = new HashSet<>(Arrays.asList(
            Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK, Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK
    ));

    private static final Set<Material> HIGH_USAGE_ITEMS = new HashSet<>(Arrays.asList(
            Material.TORCH, Material.MINECART, Material.ICE
    ));

    public QuestGeneratorService(CandyQuests plugin) {
        this.plugin = plugin;
    }

    public Map<Integer, List<Quest>> generateAllQuests() {
        long seed = System.currentTimeMillis();
        random.setSeed(seed);
        
        Map<Integer, List<Quest>> questsByLevel = new HashMap<>();
        
        for (int level = 1; level <= 3; level++) {
            int questCount = getQuestCountForLevel(level);
            if (questCount > 0) {
                questsByLevel.put(level, generateQuestsForLevel(level, questCount));
            }
        }
        
        return questsByLevel;
    }

    private List<Quest> generateQuestsForLevel(int level, int count) {
        List<Quest> quests = new ArrayList<>();
        if (count <= 0) return quests;

        int[] killPlayerNeeded = getKillPlayerNeeded(level);
        int killPlayerOccurrences = killPlayerNeeded.length;
        
        Set<Integer> playerKillPositions = choosePositions(count, killPlayerOccurrences);
        int playerKillIndex = 0;

        int mobKillQuota = getMobKillQuota(level);
        Set<Integer> mobKillPositions = choosePositionsExclude(count, mobKillQuota, playerKillPositions);

        QuestType lastType = null;
        EntityType lastEntityType = null;
        int sameTypeCount = 0;

        for (int i = 1; i <= count; i++) {
            QuestType type;
            int needed;
            
            if (playerKillPositions.contains(i)) {
                type = QuestType.KILL_PLAYER;
                needed = killPlayerNeeded[playerKillIndex++];
            } else if (mobKillPositions.contains(i)) {
                type = QuestType.KILL_MOB;
                needed = getMobKillNeeded(level, mobKillPositions, i);
            } else {
                type = getRandomNonKillTypes(lastType, sameTypeCount);
                needed = getScaledNeededAmount(type, level, i, count);
            }

            if (type == lastType) {
                sameTypeCount++;
            } else {
                sameTypeCount = 1;
                lastType = type;
            }

            Quest quest = generateQuest(getQuestId(level, i), type, level, lastEntityType, needed);
            if (type == QuestType.KILL_MOB) {
                lastEntityType = quest.getEntityType();
            }
            quests.add(quest);
        }
        
        return quests;
    }

    private Quest generateQuest(int id, QuestType type, int level, EntityType lastEntityType, int needed) {
        Material material = null;
        EntityType entityType = null;
        
        switch (type) {
            case PASS_ITEM:
                material = getRandomPassMaterial();
                break;
            case CRAFT_ITEM:
                material = getRandomCraftMaterial();
                break;
            case PLACE_BLOCK:
                material = getRandomPlaceMaterial();
                break;
            case BREAK_BLOCK:
                material = getRandomBreakMaterial();
                break;
            case KILL_MOB:
                entityType = getRandomMob(lastEntityType);
                if (entityType == null) entityType = EntityType.ZOMBIE;
                material = getMobDropMaterial(entityType);
                break;
            case KILL_PLAYER:
                material = needed >= 50 ? Material.GOLDEN_HOE : Material.PLAYER_HEAD;
                break;
        }

        needed = applyNeededLimits(type, material, needed);
        
        return new Quest(id, type, material, entityType, needed, level);
    }

    private int applyNeededLimits(QuestType type, Material material, int needed) {
        if (type == QuestType.PASS_ITEM && material != null && PASS_RARE_ORES.contains(material)) {
            return Math.min(needed, 150);
        }
        if (type == QuestType.PLACE_BLOCK && material != null && PLACE_EXPENSIVE.contains(material)) {
            return Math.min(needed, Math.max(5, (int) Math.ceil(needed / 5.0)));
        }
        if ((type == QuestType.PASS_ITEM || type == QuestType.CRAFT_ITEM) && material != null && HIGH_USAGE_ITEMS.contains(material)) {
            return Math.min(needed, 250);
        }
        if (type == QuestType.BREAK_BLOCK && material == Material.ICE) {
            return Math.min(needed, 400);
        }
        return needed;
    }

    public Material getMobDropMaterial(EntityType entityType) {
        switch (entityType) {
            case CREEPER: return Material.GUNPOWDER;
            case SPIDER:
            case CAVE_SPIDER: return Material.STRING;
            case SKELETON: return Material.BONE;
            case ZOMBIE:
            case ZOMBIE_VILLAGER:
            case HUSK:
            case DROWNED: return Material.ROTTEN_FLESH;
            case ENDERMAN: return Material.ENDER_PEARL;
            case WITCH: return Material.REDSTONE;
            case SLIME: return Material.SLIME_BALL;
            case MAGMA_CUBE: return Material.MAGMA_CREAM;
            case BLAZE: return Material.BLAZE_ROD;
            case GHAST: return Material.GHAST_TEAR;
            case ZOMBIFIED_PIGLIN:
            case PIGLIN:
            case PIGLIN_BRUTE: return Material.GOLD_NUGGET;
            case GUARDIAN:
            case ELDER_GUARDIAN: return Material.PRISMARINE_SHARD;
            case SHULKER: return Material.SHULKER_SHELL;
            case PHANTOM: return Material.PHANTOM_MEMBRANE;
            case VEX: return Material.IRON_SWORD;
            case VINDICATOR:
            case EVOKER:
            case PILLAGER: return Material.IRON_AXE;
            case RAVAGER: return Material.SADDLE;
            case COW:
            case MUSHROOM_COW: return Material.LEATHER;
            case PIG: return Material.PORKCHOP;
            case SHEEP: return Material.WHITE_WOOL;
            case CHICKEN: return Material.FEATHER;
            case HORSE:
            case DONKEY:
            case MULE: return Material.LEATHER;
            case RABBIT: return Material.RABBIT_HIDE;
            default:
                try {
                    return Material.valueOf(entityType.name() + "_SPAWN_EGG");
                } catch (IllegalArgumentException e) {
                    return Material.SPAWNER;
                }
        }
    }

    private int getQuestCountForLevel(int level) {
        int currentLevel = plugin.getConfig().getInt("settings.level", 1);
        return level > currentLevel ? 0 : 40;
    }

    private int[] getKillPlayerNeeded(int level) {
        if (level == 1) return new int[]{2, 3, 50};
        if (level == 2) return new int[]{3, 5, 6, 100};
        return new int[]{3, 5, 7, 10, 150};
    }

    private int getMobKillQuota(int level) {
        if (level == 1) return 2;
        if (level == 2) return 3;
        return 4;
    }

    private int getMobKillNeeded(int level, Set<Integer> mobPositions, int pos) {
        List<Integer> sorted = new ArrayList<>(mobPositions);
        Collections.sort(sorted);
        int index = sorted.indexOf(pos);
        if (index < 0) index = 0;
        
        if (level == 1) {
            int[] arr = {6, 9};
            return arr[Math.min(index, arr.length - 1)];
        } else if (level == 2) {
            int[] arr = {8, 12, 16};
            return arr[Math.min(index, arr.length - 1)];
        } else {
            int[] arr = {10, 14, 18, 22};
            return arr[Math.min(index, arr.length - 1)];
        }
    }

    private Set<Integer> choosePositions(int total, int occurrences) {
        Set<Integer> positions = new HashSet<>();
        if (occurrences <= 0) return positions;
        
        double step = (double) total / (occurrences + 1);
        for (int k = 1; k <= occurrences; k++) {
            int pos = (int) Math.round(step * k);
            if (pos < 1) pos = 1;
            if (pos > total) pos = total;
            while (positions.contains(pos) && pos < total) pos++;
            positions.add(pos);
        }
        return positions;
    }

    private Set<Integer> choosePositionsExclude(int total, int occurrences, Set<Integer> exclude) {
        Set<Integer> positions = new HashSet<>();
        Random r = new Random();
        while (positions.size() < occurrences) {
            int pos = 1 + r.nextInt(total);
            if (!exclude.contains(pos)) {
                positions.add(pos);
            }
        }
        return positions;
    }

    private QuestType getRandomNonKillTypes(QuestType lastType, int sameTypeCount) {
        List<QuestType> pool = new ArrayList<>();
        for (QuestType qt : QuestType.values()) {
            if (qt == QuestType.KILL_PLAYER || qt == QuestType.KILL_MOB) continue;
            if (sameTypeCount >= 2 && qt == lastType) continue;
            pool.add(qt);
        }
        if (pool.isEmpty()) {
            for (QuestType qt : QuestType.values()) {
                if (qt != QuestType.KILL_PLAYER && qt != QuestType.KILL_MOB) pool.add(qt);
            }
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private int getScaledNeededAmount(QuestType type, int level, int position, int total) {
        double progress = (double) (position - 1) / (double) (total - 1);
        double curve = Math.pow(progress, 1.15);
        int[] range = getBaseRange(type, level);
        int baseMin = range[0];
        int baseMax = range[1];
        double value = baseMin + (baseMax - baseMin) * curve;
        double variance = 0.04;
        double v = value * (1 + (random.nextDouble() * 2 - 1) * variance);
        int needed = (int) Math.round(v);
        if (needed < baseMin) needed = baseMin;
        if (needed > baseMax) needed = baseMax;
        return needed;
    }

    private int[] getBaseRange(QuestType type, int level) {
        int min, max;
        if (type == QuestType.PASS_ITEM) {
            if (level == 1) { min = 8; max = 180; }
            else if (level == 2) { min = 16; max = 260; }
            else { min = 24; max = 340; }
        } else if (type == QuestType.CRAFT_ITEM) {
            if (level == 1) { min = 3; max = 120; }
            else if (level == 2) { min = 6; max = 200; }
            else { min = 10; max = 280; }
        } else if (type == QuestType.PLACE_BLOCK) {
            if (level == 1) { min = 12; max = 120; }
            else if (level == 2) { min = 20; max = 200; }
            else { min = 30; max = 300; }
        } else if (type == QuestType.BREAK_BLOCK) {
            if (level == 1) { min = 30; max = 260; }
            else if (level == 2) { min = 50; max = 420; }
            else { min = 70; max = 650; }
        } else {
            min = 1; max = 5;
        }
        return new int[]{min, max};
    }

    private Material getRandomPassMaterial() {
        Material[] materials = {
                Material.COBBLESTONE, Material.DIRT, Material.SAND, Material.GRAVEL,
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
                Material.BEEF, Material.PORKCHOP, Material.CHICKEN, Material.MUTTON,
                Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH,
                Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT,
                Material.STRING, Material.GUNPOWDER, Material.BONE, Material.SPIDER_EYE,
                Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD,
                Material.COAL, Material.REDSTONE, Material.LAPIS_LAZULI, Material.QUARTZ
        };
        return materials[random.nextInt(materials.length)];
    }

    private Material getRandomCraftMaterial() {
        Material[] materials = {
                Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_SHOVEL, Material.WOODEN_HOE,
                Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_SHOVEL, Material.STONE_HOE,
                Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL, Material.IRON_HOE,
                Material.CRAFTING_TABLE, Material.FURNACE, Material.CHEST, Material.LADDER,
                Material.TORCH, Material.STICK, Material.BOWL, Material.BUCKET,
                Material.BREAD, Material.MINECART,
                Material.OAK_DOOR, Material.BIRCH_DOOR, Material.SPRUCE_DOOR, Material.JUNGLE_DOOR,
                Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
                Material.FLINT_AND_STEEL, Material.FISHING_ROD
        };
        return materials[random.nextInt(materials.length)];
    }

    private Material getRandomPlaceMaterial() {
        Material[] materials = {
                Material.COBBLESTONE, Material.DIRT, Material.SAND, Material.GRAVEL,
                Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS,
                Material.GLASS, Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
                Material.TORCH, Material.CHEST, Material.FURNACE, Material.CRAFTING_TABLE,
                Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
                Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK, Material.EMERALD_BLOCK
        };
        return materials[random.nextInt(materials.length)];
    }

    private Material getRandomBreakMaterial() {
        Material[] materials = {
                Material.STONE, Material.COBBLESTONE, Material.DIRT, Material.SAND, Material.GRAVEL,
                Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE,
                Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE,
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
                Material.WHEAT, Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES,
                Material.ICE
        };
        return materials[random.nextInt(materials.length)];
    }

    private EntityType getRandomMob(EntityType lastEntityType) {
        EntityType[] mobs = {
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                EntityType.ENDERMAN, EntityType.WITCH, EntityType.MAGMA_CUBE,
                EntityType.SILVERFISH, EntityType.HUSK, EntityType.PILLAGER,
                EntityType.VINDICATOR, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN
        };
        
        if (lastEntityType != null) {
            List<EntityType> available = new ArrayList<>();
            for (EntityType mob : mobs) {
                if (mob != lastEntityType) available.add(mob);
            }
            if (!available.isEmpty()) {
                return available.get(random.nextInt(available.size()));
            }
        }
        return mobs[random.nextInt(mobs.length)];
    }

    private int getQuestId(int level, int questNumber) {
        return level * 100 + questNumber;
    }
}
