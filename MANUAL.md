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
