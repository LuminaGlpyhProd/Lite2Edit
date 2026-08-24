# Lite2Edit
Java application: converts Litematics and WorldEdit schematics between each other and across Minecraft versions.

Fork of [GoldenDelicios/Lite2Edit](https://github.com/GoldenDelicios/Lite2Edit) adding a `.schem` → `.schematic` converter, cross-version conversion (1.13.2 through the newest tracked release, plus legacy 1.12.2), and a rebuilt GUI.

## GUI
Run the jar file to open the GUI application.

Pick a target Minecraft version from the dropdown, then click "Browse files..." and select one or more `.litematic` and/or `.schem` files — the source type is detected automatically per file. A live log shows progress, and any block with no equivalent in the target version is substituted (closest same-family block, or stone as a last resort) and reported rather than silently dropped.

## CLI
Run the jar file with the `--convert` argument and add all files you want converted after that. Add `--target <version>` to pick a version other than the default (`1.12.2`, the classic pre-flattening `.schematic` format).

Example:
```
java -jar Lite2Edit.jar --convert test.litematic ../anothertestfile.schem
java -jar Lite2Edit.jar --convert --target 1.20.1 test.schem
```

Both relative and full file paths work. Run with no arguments to see the full list of supported `--target` versions.

## Building
Run maven install:
```
mvn install -f pom.xml
```
The correct output file is in the folder "target" and doesn't start with "original-".
The other file doesn't have dependencies in the jar file and won't work.

## Notes on version conversion
- `1.12.2` uses numeric block ids (the format predates Minecraft's 1.13 "flattening"), so blocks added after 1.12.2 have no way to be represented there and fall back to stone.
- Other versions are matched by block existence (and a small table of known renames) against block lists sourced from [PrismarineJS/minecraft-data](https://github.com/PrismarineJS/minecraft-data); a block added after your target version, or removed before it, gets a same-family substitute if one exists, otherwise stone.
- Block *properties* (facing, waterlogged, etc.) are passed through as-is and aren't re-validated per version, so a small amount of fidelity loss is possible there.
