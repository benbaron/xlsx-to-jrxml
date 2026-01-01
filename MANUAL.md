Here’s a “v1 user manual” for the current state of the converter you and I have built.

I’ll cover:

1. What the program does
2. How to build and run it (Windows-oriented, but works everywhere)
3. How to format the Excel (.xlsx/.xlsm)
4. What files get generated and how to use them

---

## 1. What this program does

Given an Excel workbook, this tool generates, for each selected sheet:

* A **JRXML template** using your **semantic style** (`<element kind="...">`).
* A **Java bean class** that matches all dynamic fields in that sheet.
* A **metadata `.properties` file** describing the report bundle.
* A **field mapping `.csv`** listing cell → field → type → Excel format.

It’s meant to support your SCA/nonprofit reporting workflow:

* Layout is controlled in Excel (grid, positions, dynamic vs static).
* The converter turns that into Jasper-friendly JRXML + beans.
* You can then wire your own data layer and Jasper runner around it.

---

## 2. Building and running the program

### 2.1. Prerequisites

* **JDK 17+** installed and on your PATH.
* **Maven 3+** installed.
* Your project tree is the Maven module we sketched:

```text
xlsx-2-jrxml-2/
  pom.xml
  src/
    main/
      java/
        com/
          acme/
            jrgen/
              Main.java
              ExcelScanner.java
              JRXMLBuilder.java
              FragmentLibrary.java
              BeanGenerator.java
              DataFiller.java
              MetadataGenerator.java
              FieldMappingGenerator.java
              SheetModel.java
              CellItem.java
              FieldInfo.java
              BandSpec.java
              Section.java
              XsdValidator.java
```

### 2.2. Build with Maven

From the project root:

```powershell
cd xlsx-2-jrxml-2
mvn clean package
```

This will:

* Compile the Java sources.
* Build a **fat JAR** with dependencies via `maven-assembly-plugin`.

The JAR will be here:

```text
target/xlsx-2-jrxml-2-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

### 2.3. Basic command-line usage

From the project root (Windows PowerShell):

```powershell
java -jar target\xlsx-2-jrxml-2-1.0.0-SNAPSHOT-jar-with-dependencies.jar `
  --excel "C:\path\to\your\report.xlsx" `
  --out "C:\path\to\outdir"
```

On *nix/macOS the `\` becomes `\` line continuation or just put on one line:

```bash
java -jar target/xlsx-2-jrxml-2-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  --excel /path/to/report.xlsx \
  --out /path/to/outdir
```

### 2.4. All CLI options

**Required:**

* `--excel <path>`
  Path to `.xlsx` or `.xlsm` file.

* `--out <dir>`
  Output directory (will be created if missing).

**Sheet selection:**

* `--sheets Sheet1,Sheet2`
  Comma-separated list of sheet names to process.
  If omitted, **all sheets** are processed.

**Page layout & grid mapping:**

* `--pageWidth <int>` (default `595`)

* `--pageHeight <int>` (default `842`)

* `--margins left,right,top,bottom` (default `20,20,20,20`)

* `--cellSize width,height` (default `80,20`)

Cell coordinates map to JRXML as:

* `x = columnIndex * cellWidth`
* `y = rowIndex * cellHeight`

So with `--cellSize 80,20`:

* Column 0 ⇒ x = 0, col 1 ⇒ x = 80, etc.
* Row 0 ⇒ y = 0, row 1 ⇒ y = 20, etc.

**Validation (optional):**

* `--xsd <path>`
  Path to your JRXML XSD. If provided and `--skipValidation` is **not** set, each generated JRXML will be validated.

* `--skipValidation`
  If present, skip XSD validation even if `--xsd` is provided.

**Bean + metadata naming:**

* `--beanPackage <package>`
  Package for generated bean classes, and for `beanClass` in metadata.
  Default: `com.acme.jrgen.beans`
  Typical for your project: `nonprofitbookkeeping.reports.datasource`

* `--beanSuffix <suffix>`
  Suffix for bean simple class name.
  Default: `Bean`
  If sheet is `PRIMARY_ACCOUNT_2a` and suffix=`RowBean`, class name becomes `PRIMARY_ACCOUNT_2aRowBean`.

* `--generatorPackage <package>`
  Base package for `generatorClass` in metadata.
  Default: `nonprofitbookkeeping.reports.jasper`

* `--generatorSuffix <suffix>`
  Suffix for generator class simple name in metadata.
  Default: `JasperGenerator`
  For sheet `PRIMARY_ACCOUNT_2a` → `PRIMARY_ACCOUNT_2aJasperGenerator`.

* `--reportTypeSuffix <suffix>`
  Suffix appended to the reportType enum-style name.
  Default: `_JASPER`
  E.g. sheet `AccountSummary` ⇒ `ACCOUNT_SUMMARY_JASPER`.

---

## 3. How to format the Excel file

This is the most important part. The converter uses:

* **Colors** to classify cells as static/dynamic/ignored/markers.
* **Annotations** `[[...]]` in cell text or comments to override defaults.
* Optional **magenta bands** to tweak detail band behavior (height/split/printWhen).

### 3.1. Supported files & sheets

* File types: `.xlsx` or `.xlsm`
* Each **sheet** becomes one **report bundle**:

  * `SheetName.jrxml`
  * `SheetName<BeanSuffix>.java`
  * `SheetName.properties`
  * `SheetName_fieldmap.csv`

### 3.2. Static vs dynamic vs ignored vs band markers

The scanner looks at each cell (except magenta) and decides:

1. **Magenta band marker** (NOT data)

   * Fill: solid foreground, color **magenta** (#FF00FF).
   * These cells are **ignored as content**.
   * They are used only to derive an optional **BandSpec** for the detail band (height, splitType, printWhen).

2. **Ignored cells (green)**

   * Fill: solid foreground, color light green (two common ARGBs recognized).
   * These are completely ignored by the converter.

3. **Dynamic cells**

   * Any cell with a **formula** is always dynamic.
   * Any cell with a **non-white solid fill** (e.g. blue) is treated as dynamic.
   * Any cell with a `[[field ...]]` tag and `force=1` is forced dynamic even if it would otherwise be static/ignored.

4. **Static cells**

   * White/empty fill or no fill, containing non-blank text.
   * Not forced dynamic by annotations and not a formula.

### 3.3. Field annotations: `[[field ...]]`

You can put a `[[field ...]]` tag in either:

* The **cell comment**, or
* The **cell text** (it will be stripped from visible output).

Grammar (flexible order, all keys optional except the `field` keyword itself):

```text
[[field name=DepositL1Amount type=Double pattern="$ #,##0.00" align=Right force=1 section=pageFooter]]
```

Supported keys:

* `name=<identifier>`

  * Desired field name.
  * Must be unique per sheet; duplicates are auto-disambiguated.
  * If omitted, a name is auto-generated from nearby labels.

* `type=String|Double|Integer|Long|BigDecimal|Date|Boolean`

  * Maps to Java types:

    * `String` → `java.lang.String`
    * `Double` → `java.lang.Double`
    * `Integer` → `java.lang.Integer`
    * `Long` → `java.lang.Long`
    * `BigDecimal` → `java.math.BigDecimal`
    * `Date` → `java.util.Date`
    * `Boolean` → `java.lang.Boolean`
  * If omitted, type is inferred from the cell’s content (numeric/date/boolean/string).

* `pattern="<JR pattern>"`

  * JR numeric/date pattern.
  * **Note:** at the moment, this pattern is stored in the **FieldInfo** and bean mapping, but the JRXML output from `FragmentLibrary` does not yet add `pattern="..."` attributes. (We can wire that later.)

* `align=Left|Center|Right|Justified`

  * As with `pattern`, stored in the model/FieldInfo; JRXML currently doesn’t emit alignment attributes yet.

* `force=1` or `force=true`

  * Forces the cell to be treated as **dynamic** even if normally static/ignored.

* `section=<name>`

  * Routes this cell to a specific JR section (see 3.4).
  * Examples: `detail`, `pageHeader`, `pageFooter`, `title`, `background`, etc.

Anything not in `[[...]]` remains as normal visible text (after stripping).

### 3.4. Section annotations: `[[section ...]]`

By default, **every cell** (static or dynamic) is assigned to the **DETAIL** section.

You can override this per-cell:

* In the cell text or comment, add:

```text
[[section pageHeader]]
```

or

```text
[[section name=pageFooter]]
```

or

```text
[[section section=summary]]
```

Semantics:

* This works for **any cell**, static or dynamic.
* If a cell also has a `[[field ...]]` and that includes `section=...`, that wins.
* Supported section values (case-insensitive, with these aliases):

  * `detail`
  * `background`
  * `title`
  * `pageHeader`, `page_header`
  * `columnHeader`, `column_header`
  * `columnFooter`, `column_footer`
  * `pageFooter`, `page_footer`
  * `summary`

So for example:

* A static report title in the title band:

  ```text
  Cell A1: [[section title]]Quarterly Balance Sheet
  ```

* A dynamic “Printed on:” field in the page footer:

  ```text
  Cell A60: [[field name=PrintedOn type=String section=pageFooter]]
  ```

### 3.5. Detail bands and magenta rectangles: `[[band ...]]`

**Magenta cells** (solid fill magenta #FF00FF) do **not** become content. They are markers for a **BandSpec** attached to the detail band.

Put a `[[band ...]]` tag on the **top-left cell** of a magenta rectangle:

```text
[[band name=Deposits idx=1 height=160 split=Prevent printWhen=$V{PAGE_NUMBER}==1]]
```

Supported keys:

* `name=<string>`

  * Logical label (for your sanity/debugging; not used in JRXML itself right now).

* `idx=<int>`

  * Reserved for multiple bands ordering. The current implementation uses only a **single detail band**, so `idx` is effectively informational.

* `height=<int>`

  * Overrides the detail band’s height in JR units (points).
  * If omitted, band height is the vertical extent of all DETAIL cells (see 3.6).

* `split=Prevent|Stretch|Immediate`

  * Mapped to JR’s `splitType` attribute on the detail band.

* `printWhen=<JR expression>`

  * Becomes `<printWhenExpression><![CDATA[...]]></printWhenExpression>` inside the `<band>`.

**Important current behavior:**

* Rows covered by magenta rectangles **do NOT** decide which cells are “detail vs header” anymore.
* They **only feed BandSpec**:

  * Optional `height`,
  * Optional `splitType`,
  * Optional `printWhenExpression`.

All detail cells themselves are defined by their `section` (which defaults to DETAIL unless overridden via `[[section ...]]` or `section=` on `[[field ...]]`).

### 3.6. Layout & coordinates

For each non-ignored, non-magenta cell:

* `x = columnIndex * cellWidth`
* `y = rowIndex * cellHeight`
* `width = cellWidth`
* `height = cellHeight`

Where `cellWidth` and `cellHeight` come from `--cellSize`.

Section heights are computed as:

* For each section, take **max(y + height)** over all its cells.
* If a section has no cells: it’s omitted (except `<detail>`, which always exists).
* For the detail band:

  * Compute `minY` and `maxBottom` over all DETAIL cells.
  * Default height = `maxBottom - minY`, overridden by `[[band height=...]]` if present.
  * All DETAIL cell `y` coordinates are normalized to `relY = y - minY` inside the band.

---

## 4. What gets generated

Assume you run:

```powershell
java -jar target\xlsx-2-jrxml-2-1.0.0-SNAPSHOT-jar-with-dependencies.jar `
  --excel "C:\Reports\SCAFinancialReport.xlsx" `
  --out "C:\Reports\jrxml-out" `
  --beanPackage "nonprofitbookkeeping.reports.datasource" `
  --beanSuffix "RowBean" `
  --generatorPackage "nonprofitbookkeeping.reports.jasper" `
  --generatorSuffix "JasperGenerator" `
  --reportTypeSuffix "_JASPER"
```

For a sheet named `PRIMARY_ACCOUNT_2a`, you get:

1. `PRIMARY_ACCOUNT_2a.jrxml`
2. `PRIMARY_ACCOUNT_2aRowBean.java`
3. `PRIMARY_ACCOUNT_2a.properties`
4. `PRIMARY_ACCOUNT_2a_fieldmap.csv`

### 4.1. JRXML (`SheetName.jrxml`)

Structure (semantic, simplified):

```xml
<jasperReport name="PRIMARY_ACCOUNT_2a" ...>
  <!-- fields -->
  <field name="someField" class="java.lang.Double"/>
  ...

  <!-- optional sections -->
  <background height="...">
    <element kind="staticText" ...>...</element>
    ...
  </background>

  <title height="...">...</title>
  <pageHeader height="...">...</pageHeader>
  <columnHeader height="...">...</columnHeader>

  <detail>
    <band height="..." splitType="...">
      <printWhenExpression><![CDATA[...]]></printWhenExpression>
      <element kind="staticText" ...>...</element>
      <element kind="textField" ...>...</element>
    </band>
  </detail>

  <columnFooter height="...">...</columnFooter>
  <pageFooter height="...">...</pageFooter>
  <summary height="...">...</summary>
</jasperReport>
```

* **Fields**: One per dynamic cell, type from:

  * `type=` in `[[field ...]]`, or
  * Inference from Excel cell type (numeric/date/boolean/string).
* **Semantic elements**:

  * Static:

    ```xml
    <element kind="staticText" x="..." y="..." width="..." height="...">
      <text><![CDATA[Some Label]]></text>
    </element>
    ```
  * Dynamic:

    ```xml
    <element kind="textField" x="..." y="..." width="..." height="...">
      <expression><![CDATA[$F{fieldName}]]></expression>
    </element>
    ```

### 4.2. Bean class (`SheetName<BeanSuffix>.java`)

For `--beanPackage nonprofitbookkeeping.reports.datasource` and `--beanSuffix RowBean`, we get e.g.:

```java
package nonprofitbookkeeping.reports.datasource;

/** Generated bean for sheet PRIMARY_ACCOUNT_2a */
public class PRIMARY_ACCOUNT_2aRowBean
{
    private java.lang.String someField;
    private java.lang.Double amountField;
    // ...

    public String getSomeField() { return someField; }
    public void setSomeField(String v) { this.someField = v; }

    public Double getAmountField() { return amountField; }
    public void setAmountField(Double v) { this.amountField = v; }

    public PRIMARY_ACCOUNT_2aRowBean() {}
}
```

Use these in your own data layer / Jasper data source.

You can optionally use `DataFiller` (already in the project) to populate them from `Map<String, Object>` if you like reflection-based filling.

### 4.3. Metadata (`SheetName.properties`)

For sheet `AccountSummary` and CLI options:

* `--beanPackage nonprofitbookkeeping.reports.datasource`
* `--beanSuffix RowBean`
* `--generatorPackage nonprofitbookkeeping.reports.jasper`
* `--generatorSuffix JasperGenerator`
* `--reportTypeSuffix _JASPER`

You’ll get something like:

```properties
# Generated metadata for Jasper report bundle
displayName=Account Summary
generatorClass=nonprofitbookkeeping.reports.jasper.AccountSummaryJasperGenerator
reportType=ACCOUNT_SUMMARY_JASPER
template=AccountSummary.jrxml
beanClass=nonprofitbookkeeping.reports.datasource.AccountSummaryRowBean
description=Account Summary. Data bean: AccountSummaryRowBean.
```

Notes:

* `displayName` is derived from sheet name:

  * Insert spaces at camel-case boundaries and underscores, e.g. `PRIMARY_ACCOUNT_2a` → `Primary Account 2a`.
* `reportType` is an enum-style name derived from the sheet name (uppercase with underscores) plus the suffix (default `_JASPER`).

### 4.4. Field mapping (`SheetName_fieldmap.csv`)

Each row:

```csv
"SheetName","CellRef","FieldName","JavaType","ExcelFormat"
```

Example:

```csv
"PRIMARY_ACCOUNT_2a","D12","DepositL1Amount","java.lang.Double","$ #,##0.00"
"PRIMARY_ACCOUNT_2a","E25","ChecksTotal","java.math.BigDecimal","#,##0.00_);[Red](#,##0.00)"
```

* `CellRef` is in Excel A1 style: `A1`, `D12`, etc.
* `JavaType` is the fully qualified type used in the bean.
* `ExcelFormat` is the raw Excel `CellStyle.getDataFormatString()`, if any, else `""`.

You can use this CSV to:

* Cross-check bindings between Excel and JRXML.
* Drive future automatic formatting rules in your Jasper runner if desired.

---

## 5. Recommended Excel workflow (summary)

Here’s a concrete checklist for designing a new report sheet:

1. **Lay out the page**:

   * Arrange labels and numbers exactly as you want in Excel.
   * Use white background for fixed labels/headings.

2. **Mark dynamic vs ignored vs bands**:

   * Use **blue (or any non-white color)** for **dynamic data cells**.
   * Use **light green** for **ignored** helper cells.
   * Use **magenta (#FF00FF)** rectangles to define optional detail band hints (`[[band ...]]` on top-left).

3. **Add field annotations** for key dynamic cells:

   ```text
   [[field name=OperatingIncome type=BigDecimal pattern="#,##0.00" align=Right]]
   ```

4. **Route cells to sections (optional)**:

   * Title lines: `[[section title]]`
   * Column headings: `[[section columnHeader]]`
   * Page footer fields: `[[section pageFooter]]`
   * If you do nothing, everything goes to the **detail** section.

5. **Run the converter** with appropriate packages/suffixes.

6. **Integrate**:

   * Put generated `.jrxml` in your Jasper template folder.
   * Compile/compile-at-runtime JRXML in your app.
   * Use the generated `*RowBean` classes in your data layer as Jasper data source rows.
   * Use the `.properties` metadata to plug into your reporting registry/enum.

---

Here we go — concrete example + runtime usage.

I’ll do it in two big chunks:

1. Tiny Excel example → **exact** JRXML + bean + metadata + fieldmap.
2. A **developer usage** snippet showing `DataFiller` + Jasper to render it.

---

## 1. Tiny Excel example and what the generator produces

### 1.1. Excel layout (in words)

Assumptions:

* Sheet name: `AccountSummary`
* CLI options:

```text
--beanPackage nonprofitbookkeeping.reports.datasource
--beanSuffix RowBean
--generatorPackage nonprofitbookkeeping.reports.jasper
--generatorSuffix JasperGenerator
--reportTypeSuffix _JASPER
--cellSize 300,20        (for easy math; but any size works)
```

Color rules:

* **Blue** fill  → dynamic (we’ll use it on amount cells).
* **White** text → static (labels, titles).
* **No magenta** in this example (we’ll let band height be auto).

Sections via annotations:

* `[[section title]]` → goes to `<title>`.
* `[[section columnHeader]]` → goes to `<columnHeader>`.
* Default (no section) → `<detail>`.

Field annotations via `[[field ...]]` — either in the cell comment or in the text. I’ll assume **comments** on numeric cells.

#### Rows / cells

Row numbers are normal Excel row numbers; columns letters are normal Excel columns.

**Row 1**

* **A1**:
  Text:
  `[[section title]]Account Summary`
  (static, title band)

**Row 2**

* **A2**:
  Text:
  `[[section title]]Demo Nonprofit`
  (static, also in title band)

**Row 4** – column headers

* **A4**:
  Text:
  `[[section columnHeader]]Account`
* **B4**:
  Text:
  `[[section columnHeader]]Amount`

Both are static text in the columnHeader section.

**Row 5** – first data row

* **A5**:
  Text: `Cash` (plain white cell, static label; section defaults to DETAIL).
* **B5**:

  * Cell fill: **blue** (non-white solid fill → dynamic).
  * Numeric value: `100`
  * Comment text:

    ```text
    [[field name=CashAmount type=BigDecimal pattern="#,##0.00" align=Right section=detail]]
    ```

**Row 6** – second data row

* **A6**:
  Text: `Receivables` (static label).
* **B6**:

  * Cell fill: **blue**
  * Numeric value: `250`
  * Comment text:

    ```text
    [[field name=ReceivablesAmount type=BigDecimal pattern="#,##0.00" align=Right]]
    ```

    (no `section=` → default DETAIL)

**Row 7** – total row

* **A7**:
  Text: `Total Assets` (static label).
* **B7**:

  * Formula: `=SUM(B5:B6)` (formulas are always treated as dynamic).
  * (Optionally) comment:

    ```text
    [[field name=TotalAssets type=BigDecimal pattern="#,##0.00" align=Right]]
    ```

No `[[band ...]]` magenta rectangles in this simple example, so `BandSpec` is null and band height is computed from the detail rows.

> ⚠️ Note on field names:
> For `name=CashAmount`, `name=ReceivablesAmount`, `name=TotalAssets`, the generator **sanitizes & lowercases** those into:
>
> * `cashamount`
> * `receivablesamount`
> * `total_assets`
>   (per `sanitizeToJavaIdentifier`: lowercase + non-alphanumeric → `_`.)


### 1.2. Derived fields & sections

From that sheet, the model sees:

**Dynamic fields**


| Field hint name     | Sanitized field name | Java type              | From cell | 
|---------------------|----------------------|------------------------|-----------|
| `CashAmount`        | `cashamount`         | `java.math.BigDecimal` | B5        |
| `ReceivablesAmount` | `receivablesamount`  | `java.math.BigDecimal` | B6        |
| `TotalAssets`       | `total_assets`       | `java.math.BigDecimal` | B7        |


**Sections**

* `TITLE`: A1 (`Account Summary`), A2 (`Demo Nonprofit`)
* `COLUMN_HEADER`: A4 (`Account`), B4 (`Amount`)
* `DETAIL`: A5/B5, A6/B6, A7/B7

No background/pageHeader/footer/summary in this example.

---

### 1.3. Example generated JRXML (`AccountSummary.jrxml`)

This matches the **semantic** format we’ve been using (`<element kind="...">`), and the current `JRXMLBuilder` + `FragmentLibrary` style.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jasperReport
    name="AccountSummary"
    language="java"
    pageWidth="595"
    pageHeight="842"
    columnWidth="555"
    leftMargin="20"
    rightMargin="20"
    topMargin="20"
    bottomMargin="20">

  <!-- Fields inferred from dynamic cells -->
  <field name="cashamount" class="java.math.BigDecimal"/>
  <field name="receivablesamount" class="java.math.BigDecimal"/>
  <field name="total_assets" class="java.math.BigDecimal"/>

  <!-- Title section for A1, A2 -->
  <title height="40">
    <element kind="staticText" x="0" y="0" width="555" height="20">
      <text><![CDATA[Account Summary]]></text>
    </element>
    <element kind="staticText" x="0" y="20" width="555" height="20">
      <text><![CDATA[Demo Nonprofit]]></text>
    </element>
  </title>

  <!-- Column header for A4, B4 -->
  <columnHeader height="20">
    <element kind="staticText" x="0" y="0" width="300" height="20">
      <text><![CDATA[Account]]></text>
    </element>
    <element kind="staticText" x="300" y="0" width="255" height="20">
      <text><![CDATA[Amount]]></text>
    </element>
  </columnHeader>

  <!-- Detail band: 3 logical rows; band height based on cellH=20 -->
  <detail>
    <band height="60">
      <!-- Row 5: Cash -->
      <element kind="staticText" x="0" y="0" width="300" height="20">
        <text><![CDATA[Cash]]></text>
      </element>
      <element kind="textField" x="300" y="0" width="255" height="20">
        <expression><![CDATA[$F{cashamount}]]></expression>
      </element>

      <!-- Row 6: Receivables -->
      <element kind="staticText" x="0" y="20" width="300" height="20">
        <text><![CDATA[Receivables]]></text>
      </element>
      <element kind="textField" x="300" y="20" width="255" height="20">
        <expression><![CDATA[$F{receivablesamount}]]></expression>
      </element>

      <!-- Row 7: Total Assets -->
      <element kind="staticText" x="0" y="40" width="300" height="20">
        <text><![CDATA[Total Assets]]></text>
      </element>
      <element kind="textField" x="300" y="40" width="255" height="20">
        <expression><![CDATA[$F{total_assets}]]></expression>
      </element>
    </band>
  </detail>

</jasperReport>
```

Coordinates (x,y,width,height) are derived from `(col*cellWidth, row*cellHeight, cellWidth, cellHeight)` and then normalized for detail; above I just show the clean 0/20/40 row offsets that fall out of that.

---

### 1.4. Generated bean class (`AccountSummaryRowBean.java`)

With:

```text
--beanPackage nonprofitbookkeeping.reports.datasource
--beanSuffix RowBean
```

you get something like:

```java
package nonprofitbookkeeping.reports.datasource;

import java.math.BigDecimal;

/**
 * Generated bean for sheet AccountSummary.
 *
 * One instance of this bean holds all dynamic fields for the sheet.
 */
public class AccountSummaryRowBean {

    private BigDecimal cashamount;
    private BigDecimal receivablesamount;
    private BigDecimal total_assets;

    public AccountSummaryRowBean() {
    }

    public BigDecimal getCashamount() {
        return cashamount;
    }

    public void setCashamount(BigDecimal cashamount) {
        this.cashamount = cashamount;
    }

    public BigDecimal getReceivablesamount() {
        return receivablesamount;
    }

    public void setReceivablesamount(BigDecimal receivablesamount) {
        this.receivablesamount = receivablesamount;
    }

    public BigDecimal getTotal_assets() {
        return total_assets;
    }

    public void setTotal_assets(BigDecimal total_assets) {
        this.total_assets = total_assets;
    }
}
```

Note:

* Field names **must match** the `<field name="...">` in JRXML:

  * `cashamount`
  * `receivablesamount`
  * `total_assets`
* Jasper will call `getCashamount()`, `getReceivablesamount()`, `getTotal_assets()`.

---

### 1.5. Generated metadata (`AccountSummary.properties`)

With:

```text

--generatorPackage nonprofitbookkeeping.reports.jasper
--generatorSuffix JasperGenerator
--reportTypeSuffix _JASPER
```

the metadata generator emits:

```properties
# Generated metadata for Jasper report bundle
displayName=Account Summary
generatorClass=nonprofitbookkeeping.reports.jasper.AccountSummaryJasperGenerator
reportType=ACCOUNT_SUMMARY_JASPER
template=AccountSummary.jrxml
beanClass=nonprofitbookkeeping.reports.datasource.AccountSummaryRowBean
description=Account Summary. Data bean: AccountSummaryRowBean.
```

You can tweak `displayName` / `description` logic in the generator if you want something smarter, but structurally it matches what you asked for.

---

### 1.6. Generated field mapping CSV (`AccountSummary_fieldmap.csv`)

`FieldMappingGenerator` is doing:

```java
"SheetName","CellRef","FieldName","JavaType","ExcelFormat"
```

For our sheet, where we didn’t set any special Excel formats, you’d get:

```csv
"AccountSummary","B5","cashamount","java.math.BigDecimal",""
"AccountSummary","B6","receivablesamount","java.math.BigDecimal",""
"AccountSummary","B7","total_assets","java.math.BigDecimal",""
```

If you had a data format like `$ #,##0.00` on B5, you’d see that in the last column.

This CSV is your bridge table for “what bean field came from which Excel cell and what was its original Excel format”.

## 2. Developer usage: DataFiller + JasperReports

Now, how you’d actually **run** that report using the generated bean and JRXML.

### 2.1. Assumptions

* You have the generated:

  * `AccountSummary.jrxml`
  * `AccountSummaryRowBean.java` (compiled into your classpath)
  * `AccountSummary.properties` (you may or may not use it at runtime)

* You have `DataFiller` in the generator package, roughly like:

```java
// package com.acme.jrgen;

  public final class DataFiller {
      private DataFiller() {}

      public static <T> T fill(Class<T> beanClass, Map<String, ?> values) {
          // reflection: new instance, call setters that match keys
          // (implementation already in your project)
      }
  }
```

* JasperReports dependencies are on the classpath (`jasperreports`, etc.).

### 2.2. Filling the bean with `DataFiller`

Say you want:

* Cash = 100.00
* Receivables = 250.00
* Total assets = 350.00

```java
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.acme.jrgen.DataFiller;
import nonprofitbookkeeping.reports.datasource.AccountSummaryRowBean;

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

...

// Populate the values map keyed by *field name*
Map<String, Object> values = new HashMap<>();
values.put("cashamount",        new BigDecimal("100.00"));
values.put("receivablesamount", new BigDecimal("250.00"));
values.put("total_assets",      new BigDecimal("350.00"));

// Let DataFiller reflectively construct & fill the bean
AccountSummaryRowBean bean =
    DataFiller.fill(AccountSummaryRowBean.class, values);

// Data source with a single record (one bean instance)
JRBeanCollectionDataSource dataSource =
    new JRBeanCollectionDataSource(Collections.singletonList(bean));
```

If you don’t want to use `DataFiller`, you can of course do:

```java
AccountSummaryRowBean bean = new AccountSummaryRowBean();
bean.setCashamount(new BigDecimal("100.00"));
bean.setReceivablesamount(new BigDecimal("250.00"));
bean.setTotal_assets(new BigDecimal("350.00"));
```

and skip the map entirely.

### 2.3. Compiling and filling the JRXML with Jasper

Now wire it into Jasper

```java
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

// Using the bean created above: 'bean'
// and the dataSource: new JRBeanCollectionDataSource(Collections.singletonList(bean))

public class RunAccountSummaryReport {

    public static void main(String[] args) throws Exception {

        // 1. Create the bean (either via DataFiller or manual setters)
        AccountSummaryRowBean bean = makeBean();  // imagine this returns the filled bean

        JRBeanCollectionDataSource dataSource =
            new JRBeanCollectionDataSource(Collections.singletonList(bean));

        // 2. Compile the semantic JRXML (assuming it is compatible/converted)
        //    If you keep the "element kind=..." semantic form, you’ll need your
        //    own pre-step to transform it into standard JasperReports JRXML tags.
        JasperReport report =
            JasperCompileManager.compileReport("AccountSummary.jrxml");

        // 3. Parameters: none in this minimal example, so just an empty map
        Map<String, Object> params = new HashMap<>();

        // 4. Fill the report
        JasperPrint print =
            JasperFillManager.fillReport(report, params, dataSource);

        // 5. Export to PDF
        JasperExportManager.exportReportToPdfFile(print, "AccountSummary.pdf");
    }

    private static AccountSummaryRowBean makeBean() {
        AccountSummaryRowBean bean = new AccountSummaryRowBean();
        bean.setCashamount(new BigDecimal("100.00"));
        bean.setReceivablesamount(new BigDecimal("250.00"));
        bean.setTotal_assets(new BigDecimal("350.00"));
        return bean;
    }
}
```

If you want to **force** a non-iterating report (single logical record, no row-based dataset semantics), you can also use `new net.sf.jasperreports.engine.JREmptyDataSource(1)` and move your values to **parameters** instead of fields — but that’s a different layout strategy than the current generator, which uses fields + bean.

---

If you want, next step I can do a second example where:

* Title / org name / period are **parameters** (`[[param ...]]`-style annotation),
* And show how those parameters appear both in JRXML and in the code `params` map when calling `fillReport`.
