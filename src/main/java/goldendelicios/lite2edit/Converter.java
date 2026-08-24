package goldendelicios.lite2edit;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntArrayTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.ShortTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

public class Converter {

	/** Callback used to surface progress/status messages to the caller (CLI or GUI). */
	public interface Logger {
		void log(String message);
	}

	private static final Logger NO_OP_LOGGER = message -> {};

	public static List<File> litematicToWorldEdit(File inputFile, File outputDir) throws IOException {
		return litematicToWorldEdit(inputFile, outputDir, NO_OP_LOGGER);
	}

	public static List<File> litematicToWorldEdit(File inputFile, File outputDir, Logger logger) throws IOException {
		File tempFile = new File("lite2edit_" + Thread.currentThread().getId() + ".tmp");
		logger.log("Reading " + inputFile.getName() + "...");
		DataInputStream inStream = new DataInputStream(new GZIPInputStream(new FileInputStream(inputFile)));
		Tag litematica = CompoundTag.read(inStream).get("");
		inStream.close();
		int dataVersion = litematica.get("MinecraftDataVersion").intValue();

		List<File> files = new ArrayList<>();
		CompoundTag regions = litematica.get("Regions").asCompound();
		logger.log("Found " + regions.size() + " region(s)");
		for (NamedTag regionTag : regions) {
			logger.log("Converting region '" + regionTag.name() + "'...");
			CompoundTag region = regionTag.asCompound();
			ListTag palette = region.get("BlockStatePalette").asList();
			int bitsPerBlock = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));

			// Litematica dimensions can be negative.
			Tag size = region.get("Size");
			int x = size.get("x").intValue();
			int y = size.get("y").intValue();
			int z = size.get("z").intValue();

			// get offset
			Tag position = region.get("Position");
			int offsetx = position.get("x").intValue() + (x < 0 ? x+1 : 0);
			int offsety = position.get("y").intValue() + (y < 0 ? y+1 : 0);
			int offsetz = position.get("z").intValue() + (z < 0 ? z+1 : 0);

			// convert blocks
			// use a temporary file to avoid OutOfMemoryErrors for large schematics
			BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(tempFile));
			int numBlocks = Math.abs(x * y * z);
			long bitmask, bits = 0;
			int i = 0, bitCount = 0;
			for (long num : region.get("BlockStates").longArray()) {
				int remainingBits = bitCount + 64;
				if (bitCount != 0) {
					bitmask = (1 << (bitsPerBlock - bitCount)) - 1;
					long newBits = (num & bitmask) << bitCount;
					bits = bits | newBits;
					num = num >>> (bitsPerBlock - bitCount);
					remainingBits -= bitsPerBlock;
					writeBlock(fout, (short) bits);
					i++;
				}

				bitmask = (1 << bitsPerBlock) - 1;
				while (remainingBits >= bitsPerBlock) {
					bits = num & bitmask;
					num = num >>> bitsPerBlock;
					remainingBits -= bitsPerBlock;
					if (i >= numBlocks)
						break;
					writeBlock(fout, (short) bits);
					i++;
				}
				bits = num;
				bitCount = remainingBits;
			}
			fout.flush();
			fout.close();

			i = 0;
			String[] blockPalette = new String[palette.size()];
			for (SpecificTag blockState : palette) {
				String name = blockState.get("Name").stringValue();
				CompoundTag properties = blockState.get("Properties").asCompound();
				if (!properties.isEmpty()) {
					List<String> propertyNames = new ArrayList<>();
					for (NamedTag property : properties) {
						propertyNames.add(property.name() + "=" + property.unpack().stringValue());
					}
					name += "[" + String.join(",", propertyNames) + "]";
				}
				blockPalette[i++] = name;
			}

			//
			// Convert to WorldEdit format now
			//

			// read block data
			byte[] weBlocks = Files.readAllBytes(tempFile.toPath());

			// Convert palette
			CompoundTag wePalette = new CompoundTag();
			for (i = 0; i < blockPalette.length; ++i) {
				wePalette.add(blockPalette[i], new IntTag(i));
			}

			// Copy tile entity data
			List<CompoundTag> weTileEntities = new ArrayList<>();
			List<String> skip = Arrays.asList("x", "y", "z", "id");
			for (SpecificTag tileEntity : region.get("TileEntities").asList()) {
				CompoundTag liteTileEntity = tileEntity.asCompound();
				CompoundTag weTileEntity = new CompoundTag();

				// Litematica uses integer "x", "y", and "z" tags
				// WorldEdit uses one integer array "Pos" tag
				int tx = liteTileEntity.get("x").intValue();
				int ty = liteTileEntity.get("y").intValue();
				int tz = liteTileEntity.get("z").intValue();
				weTileEntity.add("Pos", new IntArrayTag(new int[] {tx, ty, tz}));

				// Litematica uses a lowercase "id"
				// WorldEdit uses a capitalized "Id"
				String tid = liteTileEntity.get("id").stringValue();
				weTileEntity.add("Id", new StringTag(tid));

				for (NamedTag tileEntityTag : liteTileEntity) {
					String name = tileEntityTag.name();
					if (!skip.contains(name))
						weTileEntity.add(tileEntityTag);
				}
				weTileEntities.add(weTileEntity);
			}

			// metadata
			CompoundTag metadata = new CompoundTag();
			metadata.add("WEOffsetX", new IntTag(offsetx));
			metadata.add("WEOffsetY", new IntTag(offsety));
			metadata.add("WEOffsetZ", new IntTag(offsetz));

			CompoundTag worldEdit = new CompoundTag();
			worldEdit.add(new NamedTag("Metadata", metadata));
			worldEdit.add(new NamedTag("Palette", wePalette));
			worldEdit.add(new NamedTag("BlockEntities", new ListTag(Tag.TAG_COMPOUND, weTileEntities)));
			worldEdit.add(new NamedTag("DataVersion", new IntTag(dataVersion)));
			worldEdit.add(new NamedTag("Height", new ShortTag((short) Math.abs(y))));
			worldEdit.add(new NamedTag("Length", new ShortTag((short) Math.abs(z))));
			worldEdit.add(new NamedTag("PaletteMax", new IntTag(wePalette.size())));
			worldEdit.add(new NamedTag("Version", new IntTag(2)));
			worldEdit.add(new NamedTag("Width", new ShortTag((short) Math.abs(x))));
			worldEdit.add(new NamedTag("BlockData", new ByteArrayTag(weBlocks)));
			worldEdit.add(new NamedTag("Offset", new IntArrayTag(new int[3])));

			CompoundTag worldEditRoot = new CompoundTag();
			worldEditRoot.add("Schematic", worldEdit);

			// determine outputFileName
			String outputFileName = inputFile.getName();
			if (outputFileName.contains(".")) {
				outputFileName = outputFileName.substring(0, outputFileName.lastIndexOf('.'));
			}
			if (regions.size() > 1) {
				outputFileName += "-" + regionTag.name();
			}
			outputFileName = outputFileName.replaceAll("[^\\w-]+", "_") + ".schem";

			// make sure directory exists, and write to the provided path
			Files.createDirectories(outputDir.toPath());
			File outputFile = new File(outputDir + "/" + outputFileName);
			writeGzippedNbt(worldEditRoot, outputFile);
			files.add(outputFile);
			logger.log("Wrote " + outputFile.getName());
		}

		tempFile.delete();
		return files;
	}

	/** Writes a gzipped NBT file, adding available-disk-space context if the write fails. */
	private static void writeGzippedNbt(CompoundTag root, File outputFile) throws IOException {
		try (DataOutputStream outStream = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(outputFile)))) {
			root.write(outStream);
		} catch (IOException e) {
			long freeMb = outputFile.getAbsoluteFile().getParentFile().getUsableSpace() / (1024 * 1024);
			throw new IOException(e.getMessage() + " (writing to " + outputFile + "; " + freeMb + " MB free on that drive)", e);
		}
	}

	private static void writeBlock(BufferedOutputStream fout, short block) throws IOException {
		if (block > 127) {
			fout.write((byte) (block | 128));
			fout.write((byte) (block / 128));
		}
		else {
			fout.write((byte) block);
		}
	}

	public static List<File> schemToSchematic(File inputFile, File outputDir) throws IOException {
		return schemToSchematic(inputFile, outputDir, NO_OP_LOGGER);
	}

	/**
	 * Converts a modern WorldEdit Sponge Schematic (.schem, v1/v2/v3) into the
	 * classic MCEdit ".schematic" format (pre-1.13 legacy block ids). Block
	 * states with no legacy equivalent fall back to stone and are reported
	 * through the logger, since the legacy format cannot represent every
	 * modern block.
	 */
	public static List<File> schemToSchematic(File inputFile, File outputDir, Logger logger) throws IOException {
		logger.log("Reading " + inputFile.getName() + "...");
		DataInputStream inStream = new DataInputStream(new GZIPInputStream(new FileInputStream(inputFile)));
		CompoundTag wrapper = CompoundTag.read(inStream).asCompound();
		inStream.close();

		CompoundTag content = null;
		for (NamedTag t : wrapper) {
			content = t.asCompound();
			break;
		}
		if (content != null && content.get("Palette").isError() && !content.get("Schematic").isError()) {
			content = content.get("Schematic").asCompound();
		}
		if (content == null || content.get("Width").isError() || content.get("Palette").isError()) {
			logger.log(inputFile.getName() + " is not a valid schematic file");
			return new ArrayList<>();
		}

		int width = content.get("Width").shortValue() & 0xFFFF;
		int height = content.get("Height").shortValue() & 0xFFFF;
		int length = content.get("Length").shortValue() & 0xFFFF;
		int numBlocks = width * height * length;
		logger.log("Dimensions: " + width + "x" + height + "x" + length + " (" + numBlocks + " blocks)");

		CompoundTag palette = content.get("Palette").asCompound();
		int paletteMax = content.get("PaletteMax").isError() ? palette.size() : content.get("PaletteMax").intValue();
		String[] indexToName = new String[Math.max(paletteMax, palette.size())];
		for (NamedTag entry : palette) {
			int idx = entry.getTag().intValue();
			if (idx >= 0 && idx < indexToName.length) indexToName[idx] = entry.name();
		}

		byte[] blockData = content.get("BlockData").byteArray();
		int[] paletteIndices = decodeVarIntBlocks(blockData, numBlocks);

		// Resolve each distinct palette entry to a legacy id/data pair once, rather than
		// once per block instance (a palette usually has a few hundred entries, while a
		// schematic can have many millions of blocks referencing them).
		Set<String> unmapped = new LinkedHashSet<>();
		byte[] idByPalette = new byte[indexToName.length];
		byte[] dataByPalette = new byte[indexToName.length];
		for (int p = 0; p < indexToName.length; p++) {
			String name = indexToName[p];
			int[] legacy = name == null ? null : LegacyBlocks.lookup(name);
			if (legacy == null) {
				if (name != null) unmapped.add(name);
				legacy = new int[] {1, 0}; // fall back to stone
			}
			idByPalette[p] = (byte) (legacy[0] & 0xFF);
			dataByPalette[p] = (byte) (legacy[1] & 0xF);
		}

		byte[] blocks = new byte[numBlocks];
		byte[] data = new byte[numBlocks];
		for (int i = 0; i < numBlocks; i++) {
			int p = paletteIndices[i];
			blocks[i] = idByPalette[p];
			data[i] = dataByPalette[p];
		}
		if (!unmapped.isEmpty()) {
			logger.log("Warning: " + unmapped.size() + " block type(s) have no legacy equivalent and were replaced with stone:");
			for (String name : unmapped) logger.log("  - " + name);
		}

		// Copy block entity data. Sponge uses "BlockEntities"/"Id"/"Pos"(int array);
		// classic .schematic uses "TileEntities"/"id"/separate x,y,z int tags.
		List<CompoundTag> tileEntities = new ArrayList<>();
		Tag blockEntities = content.get("BlockEntities");
		if (!blockEntities.isError()) {
			List<String> skip = Arrays.asList("Pos", "Id");
			for (SpecificTag blockEntity : blockEntities.asList()) {
				CompoundTag be = blockEntity.asCompound();
				CompoundTag te = new CompoundTag();
				int[] pos = be.get("Pos").intArray();
				if (pos.length == 3) {
					te.add("x", new IntTag(pos[0]));
					te.add("y", new IntTag(pos[1]));
					te.add("z", new IntTag(pos[2]));
				}
				te.add("id", new StringTag(be.get("Id").stringValue()));
				for (NamedTag beTag : be) {
					if (!skip.contains(beTag.name())) te.add(beTag);
				}
				tileEntities.add(te);
			}
			logger.log("Copied " + tileEntities.size() + " tile entity/entities");
		}

		int offsetx = 0, offsety = 0, offsetz = 0;
		Tag metadata = content.get("Metadata");
		if (!metadata.isError()) {
			if (!metadata.get("WEOffsetX").isError()) offsetx = metadata.get("WEOffsetX").intValue();
			if (!metadata.get("WEOffsetY").isError()) offsety = metadata.get("WEOffsetY").intValue();
			if (!metadata.get("WEOffsetZ").isError()) offsetz = metadata.get("WEOffsetZ").intValue();
		}

		CompoundTag schematic = new CompoundTag();
		schematic.add("Width", new ShortTag((short) width));
		schematic.add("Height", new ShortTag((short) height));
		schematic.add("Length", new ShortTag((short) length));
		schematic.add("Materials", new StringTag("Alpha"));
		schematic.add("Blocks", new ByteArrayTag(blocks));
		schematic.add("Data", new ByteArrayTag(data));
		schematic.add("Entities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>()));
		schematic.add("TileEntities", new ListTag(Tag.TAG_COMPOUND, tileEntities));
		schematic.add("WEOffsetX", new IntTag(offsetx));
		schematic.add("WEOffsetY", new IntTag(offsety));
		schematic.add("WEOffsetZ", new IntTag(offsetz));

		CompoundTag root = new CompoundTag();
		root.add("Schematic", schematic);

		String outputFileName = inputFile.getName();
		if (outputFileName.contains(".")) {
			outputFileName = outputFileName.substring(0, outputFileName.lastIndexOf('.'));
		}
		outputFileName = outputFileName.replaceAll("[^\\w-]+", "_") + ".schematic";

		Files.createDirectories(outputDir.toPath());
		File outputFile = new File(outputDir + "/" + outputFileName);
		writeGzippedNbt(root, outputFile);
		logger.log("Wrote " + outputFile.getName());

		List<File> files = new ArrayList<>();
		files.add(outputFile);
		return files;
	}

	/** Decodes `count` Sponge-format (LEB128) VarInts from `data`. */
	private static int[] decodeVarIntBlocks(byte[] data, int count) {
		int[] result = new int[count];
		int pos = 0;
		for (int i = 0; i < count && pos < data.length; i++) {
			int value = 0, shift = 0, b;
			do {
				b = data[pos++];
				value |= (b & 0x7F) << shift;
				shift += 7;
			} while ((b & 0x80) != 0 && pos < data.length);
			result[i] = value;
		}
		return result;
	}

}
