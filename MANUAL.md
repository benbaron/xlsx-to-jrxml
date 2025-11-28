# XLSX to JRXML Conversion Manual

## Overview
`jrxml-excel-generator` converts Excel workbooks into JasperReports JRXML files and can optionally emit Java bean classes, metadata properties, and a field-mapping CSV. It interprets specific formatting conventions in the workbook to build detail bands and dynamic fields tailored for reporting.

## Prerequisites
- Java 17 or later (matches the Maven toolchain used by the project).
- Maven for building and testing.
- Source Excel files in `.xlsx` or `.xlsm` format that follow the authoring rules below.

## Building
From the project root:

```bash
mvn -q package
```

The command compiles sources, runs tests, and produces the runnable JAR at `target/xlsx2jrxml-1.0.0-all.jar`.

## Running the converter
Invoke the CLI with the required Excel path and output directory:

```bash
java -jar target/xlsx2jrxml-1.0.0-all.jar \
  --excel SCAFinancialReportv6_LARGE_NOLONGERLOCKED.xlsm \
  --out output
```

### Key options
- `--sheets`: Comma-separated list limiting which sheets are converted (defaults to all sheets).
- `--beans`: Generate Java beans and a field-mapping CSV alongside JRXML output.
- `--beans-package` / `--beanPackage`: Package for generated beans (default `com.acme.jrgen.beans`).
- `--beanSuffix`: Suffix added to each bean class name (default `Bean`).
- `--generator-package`: Package for generated Jasper generator classes in metadata (default `com.acme.jrgen.generator`).
- `--generatorSuffix`: Suffix for generated Jasper generator class names (default `JasperGenerator`).
- `--reportTypeSuffix`: Suffix for generated report type enums in metadata (default `_JASPER`).
- `--pageWidth` / `--pageHeight`: Override JR page size in points (defaults 612×792 for Letter).
- `--margins`: `left,right,top,bottom` margins in points (default `20,20,20,20`).
- `--xsd`: Path to a JRXML XSD schema for validation.
- `--skipValidation`: Skip XSD validation when present.

### Output
For each sheet converted, the tool writes:
- `{SheetName}.jrxml`: A JRXML file containing base styles, a title band (report title, organization name, sheet name), and one or more detail bands.
- When `--beans` is set:
  - `{SheetName}{beanSuffix}.java`: Java bean representing the sheet’s dynamic fields.
  - `{SheetName}.properties`: Metadata describing the bean and generator details.
  - `meta/field_mapping.csv`: Consolidated mapping of sheet cells to field names, Java types, and original Excel formats.

### Validation
If `--xsd` is provided and `--skipValidation` is not set, each generated JRXML is validated against the supplied schema. Validation errors are printed to stderr per file; passing files log a success message.

## Excel authoring rules
Follow these conventions so the converter can infer layout and data bindings:
- **Detail bands**: Draw solid magenta rectangles (`#FF00FF`). The rectangle’s top-left cell can include `[[band ...]]` tags to set name, index, height, split type, or `printWhen` conditions. Do not overlap rectangles.
- **Dynamic fields**: Use blue-ish cells (palette index 41 or similar), formulas, or inline `[[field ...]]` tags to mark dynamic content. Inline tags allow `name`, `type`, `pattern`, `align`, and `force=1` overrides. Neighboring headers and row labels help auto-name fields.
- **Static text**: Any non-blue, non-formula text becomes `<staticText>` in JRXML; the generator orders elements as required by JasperReports.
- **Sizing and fonts**: Column widths and row heights come from Excel and are scaled to fill a Letter portrait page; font sizes scale with Y and clamp between 6–22 pt.
- **Patterns**: Excel number/date formats influence JR patterns automatically (e.g., accounting → `$ #,##0.00`, dates → `MMM d, yyyy`).

### Cell formatting examples
These patterns demonstrate how common Excel formats translate into JRXML output. Use blue cells (or formulas) to mark the dynamic value and keep any inline `[[field ...]]` tag in the same cell.

- **Currency with grouping**: Format the cell as `Accounting` or custom `$ #,##0.00`. The JRXML field will carry `type="java.math.BigDecimal"` with `pattern="$ #,##0.00"` and right alignment inherited from the cell.
- **Percentages**: Format as `0.0%` (or `0%`). The converter emits a numeric field with `pattern="0.0%"` so rendered values show the percent symbol and decimal precision.
- **Dates**: Use a date format such as `mmm d, yyyy` or `yyyy-MM-dd`. The JRXML field uses `java.util.Date` with the matching pattern (e.g., `pattern="MMM d, yyyy"`).
- **Plain numbers**: Apply a custom format like `#,##0` or `#,##0.00###` to control grouping and decimal places. The same pattern flows through to the JRXML field definition.
- **Text with left/center/right alignment**: Set the horizontal alignment on the cell. Static text inherits that alignment; dynamic fields default based on type but you can override with `[[field name=Total align=center]]`.
- **Zero-padding codes**: Custom format `0000` (for IDs) keeps leading zeros in the JRXML by copying the pattern.
- **Explicit overrides**: When Excel formatting is insufficient, supply an inline tag such as `[[field name=Status type=java.lang.String pattern=@]]` to freeze the type and pattern regardless of cell format.

## Typical workflow
1. Author the workbook following the rules above (magenta bands, blue dynamic cells, optional inline tags).
2. Build the project with `mvn -q package`.
3. Run the converter with your Excel file and target output directory.
4. Review generated JRXML (and beans/metadata if enabled).
5. Optionally re-run with `--xsd` to validate JRXML against a schema.

## Troubleshooting
- **No output files**: Confirm the Excel path is correct and accessible; the tool must be pointed at `.xlsx` or `.xlsm` files.
- **Validation failures**: Inspect stderr for per-file XSD errors; correct the workbook layout or adjust JR settings, then re-run.
- **Unexpected field names or types**: Use inline `[[field ...]]` tags to override auto-detected names, types, or patterns for specific cells.
- **Layout sizing issues**: Adjust Excel column widths/row heights; the converter scales them to fit the target page dimensions and margins.
