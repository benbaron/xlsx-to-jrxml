# xlsx-to-jrxml

Generate JRXML (detail-only bands) directly from Excel workbooks using the authoring rules for blue dynamic cells and magenta detail rectangles. The tool can also emit Java bean classes and a field mapping CSV for data binding.

## Highlights
- Solid magenta rectangles (#FF00FF) mark detail bands; the top-left cell supports `[[band ...]]` tags for name/index/height/split/printWhen settings.
- Blue cells (palette 41 or blue-ish RGB), formulas, or `[[field ...]]` tags mark dynamic fields. Inline tags accept `name`, `type`, `pattern`, `align`, and `force=1` overrides.
- Column widths and row heights come from Excel, then the layout is scaled to fill a Letter portrait page (defaults 612×792 with 20pt margins). Font sizes scale with Y and clamp between 6–22pt.
- Non-blue, non-formula text becomes `<staticText>` with the required JRXML element order.
- Each JRXML includes base styles, two parameters (`reportTitle`, `organizationName`), and a three-line title band (title, organization, sheet name).
- Optional `--beans` generation writes one bean per sheet plus `meta/field_mapping.csv` with column/row → field/type/format mappings.
- JRXML + metadata files are written under `out/jrxml`, beans under `out/beans`, and generator shells under `out/generators`.

## Usage

```bash
mvn -q package
java -jar target/xlsx2jrxml-1.0.0-all.jar \
  --excel MyReport.xlsx \
  --out output-dir \
  --beans \
  --beans-package org.scaledger.reports.AccountBeans
```

### Key flags
- `--sheets`: comma-separated list to limit scanning.
- `--pageWidth` / `--pageHeight`: override Letter defaults (points).
- `--margins`: left,right,top,bottom margins (points; default 20,20,20,20).
- `--xsd`: optional JRXML XSD validation (skip with `--skipValidation`).
- `--beans`: enable bean/metadata/mapping CSV generation.
- `--beans-package` / `--beanPackage`: package for generated beans.
- `--beanSuffix`: suffix appended to generated bean class names.
- `--generator-package`: package recorded for generated Jasper generator classes in metadata.
- `--generatorSuffix`: suffix appended to Jasper generator class names in metadata.
- `--reportTypeSuffix`: suffix appended to report type enum names in metadata.
- Output is organized into subdirectories: JRXML + `.properties` in `jrxml/`, beans in `beans/`, generator shells in `generators/`.

## Authoring quick reference
- Blue cells or formulas become dynamic fields; nearest headers/row labels drive auto names unless `[[field name=...]]` overrides.
- Magenta rectangles split sheets into multiple detail bands; content inside renders relative to the rectangle origin. No overlaps allowed.
- Patterns infer from Excel formats (accounting → `$ #,##0.00`, dates → `MMM d, yyyy`, numeric tokens → `#,##0.###`).
- Fonts, widths, and heights follow Excel proportions after scaling to the Letter page.
