package goldendelicios.lite2edit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads, from bundled resources, which blocks exist in each supported Minecraft
 * milestone version (1.13.2 through the newest tracked release), plus the NBT
 * "DataVersion" integer for each. Block name lists were extracted from the
 * community-maintained PrismarineJS/minecraft-data project, which generates them
 * from Minecraft's own data generator reports; DataVersion integers are from the
 * Minecraft Wiki. 1.12.2 (pre-flattening, numeric block ids) is handled separately
 * by {@link LegacyBlocks} and is not part of this table.
 */
final class VersionBlocks {
	private static final List<String> LABELS = new ArrayList<>();
	private static final Map<String, Integer> DATA_VERSION = new HashMap<>();
	private static final Map<String, Set<String>> BLOCKS = new HashMap<>();

	static {
		try (InputStream in = VersionBlocks.class.getResourceAsStream("/blockversions/manifest.tsv")) {
			if (in == null) throw new IOException("missing blockversions/manifest.tsv resource");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				String[] parts = line.split("\t");
				String label = parts[0];
				int dataVersion = Integer.parseInt(parts[1]);
				String fileName = parts[2];
				LABELS.add(label);
				DATA_VERSION.put(label, dataVersion);
				BLOCKS.put(label, loadBlockSet(fileName));
			}
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static Set<String> loadBlockSet(String fileName) throws IOException {
		Set<String> set = new HashSet<>();
		try (InputStream in = VersionBlocks.class.getResourceAsStream("/blockversions/" + fileName)) {
			if (in == null) throw new IOException("missing blockversions/" + fileName + " resource");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().isEmpty()) set.add(line.trim());
			}
		}
		return set;
	}

	/** Milestone version labels, oldest to newest. Does not include "1.12.2". */
	static List<String> labels() {
		return Collections.unmodifiableList(LABELS);
	}

	static int dataVersion(String label) {
		Integer v = DATA_VERSION.get(label);
		if (v == null) throw new IllegalArgumentException("Unknown version label: " + label);
		return v;
	}

	static Set<String> blockSet(String label) {
		Set<String> set = BLOCKS.get(label);
		if (set == null) throw new IllegalArgumentException("Unknown version label: " + label);
		return set;
	}

	private VersionBlocks() {}
}
