package goldendelicios.lite2edit;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps modern (post-1.13 "flattened") block state names to the legacy
 * numeric block id + data value pairs used by the classic MCEdit
 * ".schematic" format (pre-1.13). The mapping cannot be perfectly complete
 * (some modern block states have no legacy equivalent), so callers should
 * treat {@link #lookup} returning null as "no exact match" and fall back
 * to a placeholder.
 */
final class LegacyBlocks {

	private static final String[] COLORS = {
		"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
		"gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
	};

	private static final String[] WOOD4 = {"oak", "spruce", "birch", "jungle"};
	private static final String[] WOOD2 = {"acacia", "dark_oak"};

	private static final Map<String, Integer> BASE_ID = new HashMap<>();
	private static final Map<String, Integer> STAIR_ID = new HashMap<>();
	private static final Map<String, int[]> SLAB_INFO = new HashMap<>(); // {familyId, materialIndex}
	private static final Map<String, Integer> STONE_DATA = new HashMap<>(); // name -> data value for legacy id 1
	private static final Map<String, Integer> STONE_BRICK_DATA = new HashMap<>(); // name -> data value for legacy id 98

	static {
		int[][] base = {
			{1, 0}, {3, 0}, {4, 0}, {7, 0}, {9, 0}, {11, 0}, {12, 0}, {13, 0},
			{14, 0}, {15, 0}, {16, 0}, {19, 0}, {20, 0}, {21, 0}, {22, 0}, {23, 0},
			{24, 0}, {25, 0}, {29, 0}, {30, 0}, {33, 0}, {34, 0}, {41, 0}, {42, 0},
			{45, 0}, {46, 0}, {47, 0}, {48, 0}, {49, 0}, {50, 0}, {51, 0}, {52, 0},
			{54, 0}, {55, 0}, {56, 0}, {57, 0}, {58, 0}, {59, 0}, {60, 0}, {61, 0},
			{65, 0}, {66, 0}, {69, 0}, {70, 0}, {71, 0}, {72, 0}, {73, 0}, {76, 0},
			{77, 0}, {78, 0}, {79, 0}, {80, 0}, {81, 0}, {82, 0}, {83, 0}, {84, 0},
			{86, 0}, {87, 0}, {88, 0}, {89, 0}, {90, 0}, {91, 0}, {92, 0}, {93, 0},
			{97, 0}, {98, 0}, {99, 0}, {100, 0}, {101, 0}, {102, 0}, {103, 0}, {104, 0},
			{105, 0}, {106, 0}, {110, 0}, {111, 0}, {112, 0}, {113, 0}, {115, 0}, {116, 0},
			{117, 0}, {118, 0}, {119, 0}, {120, 0}, {121, 0}, {122, 0}, {123, 0}, {127, 0},
			{129, 0}, {130, 0}, {131, 0}, {132, 0}, {133, 0}, {137, 0}, {138, 0}, {139, 0},
			{140, 0}, {141, 0}, {142, 0}, {143, 0}, {145, 0}, {146, 0}, {147, 0}, {148, 0},
			{149, 0}, {151, 0}, {152, 0}, {153, 0}, {154, 0}, {155, 0}, {157, 0}, {158, 0},
			{165, 0}, {166, 0}, {167, 0}, {169, 0}, {170, 0}, {172, 0}, {173, 0}, {174, 0},
			{198, 0}, {199, 0}, {200, 0}, {201, 0}, {202, 0}, {206, 0}, {207, 0}, {208, 0},
			{209, 0}, {210, 0}, {211, 0}, {212, 0}, {213, 0}, {214, 0}, {215, 0}, {216, 0},
			{217, 0}, {218, 0}, {255, 0},
		};
		String[] baseNames = {
			"stone", "dirt", "cobblestone", "bedrock", "water", "lava", "sand", "gravel",
			"gold_ore", "iron_ore", "coal_ore", "sponge", "glass", "lapis_ore", "lapis_block", "dispenser",
			"sandstone", "note_block", "sticky_piston", "cobweb", "piston", "piston_head", "gold_block", "iron_block",
			"bricks", "tnt", "bookshelf", "mossy_cobblestone", "obsidian", "torch", "fire", "spawner",
			"chest", "redstone_wire", "diamond_ore", "diamond_block", "crafting_table", "wheat", "farmland", "furnace",
			"ladder", "rail", "lever", "stone_pressure_plate", "iron_door", "oak_pressure_plate", "redstone_ore", "redstone_torch",
			"stone_button", "snow", "ice", "snow_block", "cactus", "clay", "sugar_cane", "jukebox",
			"pumpkin", "netherrack", "soul_sand", "glowstone", "nether_portal", "jack_o_lantern", "cake", "repeater",
			"infested_stone", "stone_bricks", "brown_mushroom_block", "red_mushroom_block", "iron_bars", "glass_pane", "melon", "pumpkin_stem",
			"melon_stem", "vine", "mycelium", "lily_pad", "nether_bricks", "nether_brick_fence", "nether_wart", "enchanting_table",
			"brewing_stand", "cauldron", "end_portal", "end_portal_frame", "end_stone", "dragon_egg", "redstone_lamp", "cocoa",
			"emerald_ore", "ender_chest", "tripwire_hook", "tripwire", "emerald_block", "command_block", "beacon", "cobblestone_wall",
			"flower_pot", "carrots", "potatoes", "oak_button", "anvil", "trapped_chest", "light_weighted_pressure_plate", "heavy_weighted_pressure_plate",
			"comparator", "daylight_detector", "redstone_block", "nether_quartz_ore", "hopper", "quartz_block", "activator_rail", "dropper",
			"slime_block", "barrier", "iron_trapdoor", "sea_lantern", "hay_block", "terracotta", "coal_block", "packed_ice",
			"end_rod", "chorus_plant", "chorus_flower", "purpur_block", "purpur_pillar", "end_stone_bricks", "beetroots", "grass_path",
			"end_gateway", "repeating_command_block", "chain_command_block", "frosted_ice", "magma_block", "nether_wart_block", "red_nether_bricks", "bone_block",
			"structure_void", "observer", "structure_block",
		};
		for (int i = 0; i < baseNames.length; i++) {
			BASE_ID.put(baseNames[i], base[i][0]);
		}
		BASE_ID.put("air", 0);
		BASE_ID.put("cave_air", 0);
		BASE_ID.put("void_air", 0);
		BASE_ID.put("grass_block", 2);
		BASE_ID.put("dead_bush", 32);
		BASE_ID.put("mossy_cobblestone_wall", 139);
		BASE_ID.put("smooth_stone", 43);
		String[] stoneVariants = {"stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite"};
		for (int i = 0; i < stoneVariants.length; i++) STONE_DATA.put(stoneVariants[i], i);
		String[] stoneBrickVariants = {"stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks"};
		for (int i = 0; i < stoneBrickVariants.length; i++) STONE_BRICK_DATA.put(stoneBrickVariants[i], i);
		BASE_ID.put("dandelion", 37);
		BASE_ID.put("poppy", 38);
		BASE_ID.put("brown_mushroom", 39);
		BASE_ID.put("red_mushroom", 40);
		BASE_ID.put("oak_door", 64);
		BASE_ID.put("oak_trapdoor", 96);
		BASE_ID.put("oak_fence", 85);
		BASE_ID.put("oak_fence_gate", 107);
		BASE_ID.put("prismarine", 168);
		BASE_ID.put("prismarine_bricks", 168);
		BASE_ID.put("dark_prismarine", 168);
		BASE_ID.put("red_sandstone", 179);
		BASE_ID.put("cut_red_sandstone", 179);
		BASE_ID.put("chiseled_red_sandstone", 179);
		BASE_ID.put("smooth_red_sandstone", 179);
		BASE_ID.put("cut_sandstone", 24);
		BASE_ID.put("chiseled_sandstone", 24);
		BASE_ID.put("smooth_sandstone", 24);
		BASE_ID.put("skeleton_skull", 144);
		BASE_ID.put("skeleton_wall_skull", 144);
		BASE_ID.put("wither_skeleton_skull", 144);
		BASE_ID.put("wither_skeleton_wall_skull", 144);
		BASE_ID.put("zombie_head", 144);
		BASE_ID.put("zombie_wall_head", 144);
		BASE_ID.put("creeper_head", 144);
		BASE_ID.put("creeper_wall_head", 144);
		BASE_ID.put("dragon_head", 144);
		BASE_ID.put("dragon_wall_head", 144);
		BASE_ID.put("player_head", 144);
		BASE_ID.put("player_wall_head", 144);

		int[] stairIds = {53, 67, 108, 109, 114, 128, 134, 135, 136, 156, 163, 164, 180, 203};
		String[] stairNames = {
			// "stone" here means the legacy "Cobblestone Stairs" (id 67) -- Minecraft has always
			// named that block's modern id "minecraft:stone_stairs", confusingly.
			"oak", "stone", "brick", "stone_brick", "nether_brick", "sandstone", "spruce", "birch",
			"jungle", "quartz", "acacia", "dark_oak", "red_sandstone", "purpur"
		};
		for (int i = 0; i < stairNames.length; i++) STAIR_ID.put(stairNames[i], stairIds[i]);

		String[] stoneSlabMaterials = {"stone", "sandstone", null, "cobblestone", "brick", "stone_brick", "nether_brick", "quartz"};
		for (int i = 0; i < stoneSlabMaterials.length; i++) {
			if (stoneSlabMaterials[i] != null) SLAB_INFO.put(stoneSlabMaterials[i] + "_slab", new int[] {44, i});
		}
		String[] woodSlabSpecies = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
		for (int i = 0; i < woodSlabSpecies.length; i++) SLAB_INFO.put(woodSlabSpecies[i] + "_slab", new int[] {126, i});
		SLAB_INFO.put("red_sandstone_slab", new int[] {182, 0});
		SLAB_INFO.put("purpur_slab", new int[] {205, 0});

		// wood fences/gates/doors/trapdoors, per-species distinct legacy ids
		int[] fenceIds = {188, 189, 190, 191, 192};
		int[] gateIds = {183, 184, 185, 186, 187};
		int[] doorIds = {193, 194, 195, 196, 197};
		String[] species5 = {"spruce", "birch", "jungle", "dark_oak", "acacia"};
		for (int i = 0; i < species5.length; i++) {
			BASE_ID.put(species5[i] + "_fence", fenceIds[i]);
			BASE_ID.put(species5[i] + "_fence_gate", gateIds[i]);
			BASE_ID.put(species5[i] + "_door", doorIds[i]);
		}
	}

	/** Returns {legacyId, data}, or null if there is no known mapping. */
	static int[] lookup(String fullName) {
		String name = fullName.startsWith("minecraft:") ? fullName.substring(10) : fullName;
		int bracket = name.indexOf('[');
		String base = bracket < 0 ? name : name.substring(0, bracket);
		Map<String, String> props = parseProperties(bracket < 0 ? "" : name.substring(bracket + 1, name.length() - 1));

		Integer stoneData = STONE_DATA.get(base);
		if (stoneData != null) return new int[] {1, stoneData};
		Integer brickData = STONE_BRICK_DATA.get(base);
		if (brickData != null) return new int[] {98, brickData};

		// colour-indexed families: fixed id, data = colour index
		int[] r;
		if ((r = colorFamily(base, "wool", 35)) != null) return r;
		if ((r = colorFamily(base, "carpet", 171)) != null) return r;
		if ((r = colorFamily(base, "stained_glass", 95)) != null) return r;
		if ((r = colorFamily(base, "stained_glass_pane", 160)) != null) return r;
		if ((r = colorFamily(base, "terracotta", 159)) != null) return r; // colored terracotta ("stained clay")
		if ((r = colorFamily(base, "concrete", 251)) != null) return r;
		if ((r = colorFamily(base, "concrete_powder", 252)) != null) return r;

		// colour-indexed families where colour selects the block id instead
		for (int i = 0; i < COLORS.length; i++) {
			if (base.equals(COLORS[i] + "_shulker_box")) return new int[] {219 + i, 0};
			if (base.equals(COLORS[i] + "_glazed_terracotta")) return new int[] {235 + i, facing0to3(props.get("facing"))};
			if (base.equals(COLORS[i] + "_bed")) return new int[] {26, bedData(props)};
		}
		if (base.equals("shulker_box")) return new int[] {219 + indexOf(COLORS, "purple"), 0};

		if (base.equals("grass") || base.equals("short_grass")) return new int[] {31, 1};
		if (base.equals("fern")) return new int[] {31, 2};
		if (base.equals("tall_grass")) return new int[] {175, "upper".equals(props.get("half")) ? 10 : 2};
		if (base.equals("large_fern")) return new int[] {175, "upper".equals(props.get("half")) ? 10 : 3};

		// wood-species families; species[0]==0 -> WOOD4 (oak/spruce/birch/jungle), ==1 -> WOOD2 (acacia/dark_oak)
		int[] species = matchWood(base, "_planks");
		if (species != null) return new int[] {5, species[0] == 0 ? species[1] : 4 + species[1]};
		species = matchWood(base, "_log");
		if (species != null) {
			int axis = axisBits(props.get("axis"));
			return species[0] == 0 ? new int[] {17, species[1] | axis} : new int[] {162, species[1] | axis};
		}
		species = matchWood(base, "_wood"); // bark-on-all-sides log variant, treat like log
		if (species != null) {
			int axis = axisBits(props.get("axis"));
			return species[0] == 0 ? new int[] {17, species[1] | axis} : new int[] {162, species[1] | axis};
		}
		species = matchWood(base, "_leaves");
		if (species != null) return species[0] == 0 ? new int[] {18, species[1] | 4} : new int[] {161, species[1] | 4};
		species = matchWood(base, "_sapling");
		if (species != null) return new int[] {6, species[0] == 0 ? species[1] : 4 + species[1]};

		if (base.endsWith("_stairs")) {
			Integer id = STAIR_ID.get(base.substring(0, base.length() - "_stairs".length()));
			if (id != null) return new int[] {id, stairData(props)};
		}

		int[] slab = SLAB_INFO.get(base);
		if (slab != null) {
			int top = "top".equals(props.get("type")) ? 8 : 0;
			return new int[] {slab[0], slab[1] | top};
		}

		Integer id = BASE_ID.get(base);
		if (id != null) return new int[] {id, 0};
		return null;
	}

	private static int[] colorFamily(String base, String suffix, int baseId) {
		for (int i = 0; i < COLORS.length; i++) {
			if (base.equals(COLORS[i] + "_" + suffix)) return new int[] {baseId, i};
		}
		return null;
	}

	private static int[] matchWood(String base, String suffix) {
		if (!base.endsWith(suffix)) return null;
		String species = base.substring(0, base.length() - suffix.length());
		int idx = indexOf(WOOD4, species);
		if (idx >= 0) return new int[] {0, idx};
		idx = indexOf(WOOD2, species);
		if (idx >= 0) return new int[] {1, idx};
		return null;
	}

	private static int indexOf(String[] arr, String v) {
		for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
		return -1;
	}

	private static int axisBits(String axis) {
		if ("x".equals(axis)) return 4;
		if ("z".equals(axis)) return 8;
		return 0;
	}

	private static int stairData(Map<String, String> props) {
		int data = facing0to3(props.get("facing"));
		if ("top".equals(props.get("half"))) data |= 4;
		return data;
	}

	private static int facing0to3(String facing) {
		if ("west".equals(facing)) return 1;
		if ("south".equals(facing)) return 2;
		if ("north".equals(facing)) return 3;
		return 0; // east / default
	}

	private static int bedData(Map<String, String> props) {
		int data = facing0to3(props.get("facing"));
		if ("head".equals(props.get("part"))) data |= 8;
		return data;
	}

	private static Map<String, String> parseProperties(String body) {
		Map<String, String> props = new HashMap<>();
		if (body.isEmpty()) return props;
		for (String pair : body.split(",")) {
			int eq = pair.indexOf('=');
			if (eq > 0) props.put(pair.substring(0, eq), pair.substring(eq + 1));
		}
		return props;
	}

	private LegacyBlocks() {}
}
