package com.acme.jrgen;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.acme.jrgen.model.CellItem;
import com.acme.jrgen.model.CellItem.Role;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Scans Excel sheets and produces SheetModel objects.
 *
 * Color rules:
 *   - green fill            -> IGNORE
 *   - magenta fill (#FF00FF)-> band marker (not a data cell)
 *   - non-white solid fill  -> DYNAMIC
 *   - no fill/white + text  -> STATIC
 *
 * Annotations:
 *   [[field name=... type=Double pattern="$ #,##0.00" align=Right force=1 section=pageHeader]]
 *   [[band  name=Deposits idx=1 height=160 split=Prevent printWhen=$V{PAGE_NUMBER}==1]]
 *   [[section pageHeader]]  (standalone, for static or dynamic cells)
 */
public class ExcelScanner
{
    private final Path excelPath;
    private final int cellW;
    private final int cellH;

    public ExcelScanner(Path excelPath, int cellW, int cellH)
    {
        this.excelPath = excelPath;
        this.cellW = cellW;
        this.cellH = cellH;
    }

    public List<SheetModel> scan(List<String> restrictToSheets) throws Exception
    {
        try (InputStream in = Files.newInputStream(this.excelPath);
             Workbook wb = new XSSFWorkbook(in))
        {
            List<SheetModel> result = new ArrayList<>();

            for (int s = 0; s < wb.getNumberOfSheets(); s++)
            {
                Sheet sheet = wb.getSheetAt(s);
                String name = sheet.getSheetName();

                if (restrictToSheets != null && !restrictToSheets.isEmpty())
                {
                    boolean match = restrictToSheets.stream()
                        .anyMatch(n -> n.equalsIgnoreCase(name));
                    if (!match)
                    {
                        continue;
                    }
                }

                List<CellItem> items = new ArrayList<>();
                Map<String, Integer> nameCounts = new HashMap<>();

                // Magenta-based band extents and hint
                int bandTopRow = Integer.MAX_VALUE;
                int bandBottomRow = -1;
                int bandTopLeftRow = Integer.MAX_VALUE;
                int bandTopLeftCol = Integer.MAX_VALUE;
                BandHint bandHint = null;

                for (Row row : sheet)
                {
                    if (row == null)
                    {
                        continue;
                    }

                    for (Cell cell : row)
                    {
                        if (cell == null)
                        {
                            continue;
                        }

                        int rowIdx = cell.getRowIndex();
                        int colIdx = cell.getColumnIndex();

                        // Detect magenta band cells first; they are not data.
                        if (isMagentaCell(cell))
                        {
                            // Track vertical extent
                            if (rowIdx < bandTopRow)
                            {
                                bandTopRow = rowIdx;
                            }
                            if (rowIdx > bandBottomRow)
                            {
                                bandBottomRow = rowIdx;
                            }

                            // Track top-left cell to read [[band ...]] from it
                            if (rowIdx < bandTopLeftRow
                                || (rowIdx == bandTopLeftRow && colIdx < bandTopLeftCol))
                            {
                                bandTopLeftRow = rowIdx;
                                bandTopLeftCol = colIdx;
                                BandHint h = readBandHint(cell);
                                if (h != null)
                                {
                                    bandHint = h;
                                }
                            }

                            // Magenta rectangles are markers only, not content
                            continue;
                        }

                        // Field + section hints (from comment or inline text)
                        FieldHint fieldHint = readFieldHint(cell);

                        Role role = classify(cell, fieldHint);
                        if (role == null)
                        {
                            continue;
                        }

                        int x = colIdx * this.cellW;
                        int y = rowIdx * this.cellH;

                        String rawValue = getCellString(cell);
                        String displayValue = stripTags(rawValue);

                        String fieldName = null;
                        String javaType = null;
                        String pattern = null;
                        String align = null;

                        if (role == Role.DYNAMIC)
                        {
                            String baseName;
                            if (fieldHint != null && fieldHint.name != null && !fieldHint.name.isBlank())
                            {
                                baseName = fieldHint.name;
                            }
                            else
                            {
                                baseName = buildBaseName(sheet, cell);
                            }

                            fieldName = uniquify(baseName, nameCounts);

                            // Type: annotation override, else infer from cell
                            if (fieldHint != null && fieldHint.type != null && !fieldHint.type.isBlank())
                            {
                                javaType = mapToJavaFqcn(fieldHint.type);
                            }
                            else
                            {
                                javaType = inferJavaType(cell);
                            }

                            pattern = (fieldHint != null) ? fieldHint.pattern : null;
                            align   = (fieldHint != null) ? fieldHint.align   : null;
                        }

                        String excelFormat = excelDataFormat(cell);

                        Section section = Section.DETAIL; // default: everything in DETAIL
                        if (fieldHint != null && fieldHint.section != null && !fieldHint.section.isBlank())
                        {
                            section = parseSection(fieldHint.section);
                        }

                        items.add(new CellItem(
                            colIdx,
                            rowIdx,
                            x,
                            y,
                            this.cellW,
                            this.cellH,
                            displayValue,
                            role,
                            fieldName,
                            javaType,
                            pattern,
                            align,
                            excelFormat,
                            section
                        ));
                    }
                }

                BandSpec bandSpec = null;
                if (bandTopRow != Integer.MAX_VALUE && bandBottomRow >= bandTopRow)
                {
                    bandSpec = new BandSpec(
                        bandTopRow,
                        bandBottomRow,
                        (bandHint != null ? bandHint.height : null),
                        (bandHint != null ? bandHint.split : null),
                        (bandHint != null ? bandHint.printWhenExpr : null),
                        (bandHint != null ? bandHint.name : null)
                    );
                }

                result.add(new SheetModel(name, items, bandSpec));
            }

            return result;
        }
    }

    // ---------------------------------------------------------------------
    // Classification & helpers
    // ---------------------------------------------------------------------

    /**
     * Role classification based on annotation, formulas, fill, and content.
     */
    private static Role classify(Cell cell, FieldHint hint)
    {
        if (cell == null)
        {
            return null;
        }

        // Annotation may force dynamic
        if (hint != null && hint.force)
        {
            return Role.DYNAMIC;
        }

        // Formulas are always dynamic
        if (cell.getCellType() == CellType.FORMULA)
        {
            return Role.DYNAMIC;
        }

        CellStyle style = cell.getCellStyle();
        if (style != null)
        {
            FillPatternType pattern = style.getFillPattern();
            Color fg = style.getFillForegroundColorColor();

            if (pattern == FillPatternType.SOLID_FOREGROUND && fg != null)
            {
                String argb = colorToARGB(fg);

                // Greens => ignore
                if ("FFCCFFCC".equalsIgnoreCase(argb) || "FFC6EFCE".equalsIgnoreCase(argb))
                {
                    return null; // IGNORE
                }

                // Explicit white => static (if there is text)
                if ("FFFFFFFF".equalsIgnoreCase(argb))
                {
                    String v = getCellString(cell);
                    return (v == null || v.isBlank()) ? null : Role.STATIC;
                }

                // Any other solid fill => dynamic (blue-ish, etc.)
                return Role.DYNAMIC;
            }
        }

        String v = getCellString(cell);
        if (v == null || v.isBlank())
        {
            return null;
        }

        return Role.STATIC;
    }

    private static String getCellString(Cell c)
    {
        if (c == null)
        {
            return "";
        }

        return switch (c.getCellType())
        {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(c)
                ? c.getDateCellValue().toString()
                : Double.toString(c.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(c.getBooleanCellValue());
            case FORMULA -> c.getCellFormula();
            case BLANK, _NONE, ERROR -> "";
        };
    }

    private static String excelDataFormat(Cell cell)
    {
        if (cell == null)
        {
            return null;
        }
        CellStyle style = cell.getCellStyle();
        if (style == null)
        {
            return null;
        }
        String fmt = style.getDataFormatString();
        if (fmt == null || fmt.isBlank())
        {
            return null;
        }
        return fmt;
    }

    /**
     * Convert POI color to ARGB hex when possible.
     */
    private static String colorToARGB(Color c)
    {
        if (c instanceof XSSFColor xc)
        {
            byte[] argb = xc.getARGB();
            if (argb != null)
            {
                StringBuilder sb = new StringBuilder();
                for (byte b : argb)
                {
                    sb.append(String.format("%02X", b));
                }
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * Sanitize arbitrary header/label text into a safe Java identifier.
     */
    private static String sanitizeToJavaIdentifier(String raw)
    {
        String s = (raw == null) ? "" : raw.toLowerCase(Locale.ROOT);

        // Replace non-alphanumeric with underscores
        s = s.replaceAll("[^a-z0-9]+", "_");
        s = s.replaceAll("_+", "_");
        s = s.replaceAll("^_+|_+$", "");

        if (s.isEmpty())
        {
            s = "field";
        }

        if (Character.isDigit(s.charAt(0)))
        {
            s = "_" + s;
        }

        return s;
    }

    /**
     * Ensure uniqueness of field names on a sheet.
     */
    private static String uniquify(String base, Map<String, Integer> counts)
    {
        String clean = sanitizeToJavaIdentifier(base);
        if (clean.isEmpty())
        {
            clean = "field";
        }

        Integer n = counts.get(clean);

        if (n == null)
        {
            counts.put(clean, 1);
            return clean;
        }
        else
        {
            n = n + 1;
            counts.put(clean, n);
            return clean + "_" + n;
        }
    }

    /**
     * Build a base field name using:
     *   - nearest header above in same column
     *   - nearest label to the left in same row
     *   - fallback: SHEET_RxCy
     */
    private static String buildBaseName(Sheet sheet, Cell cell)
    {
        int col = cell.getColumnIndex();
        int row = cell.getRowIndex();

        String header = findHeader(sheet, row, col);
        String rowLabel = findRowLabel(sheet, row, col);

        List<String> parts = new ArrayList<>();

        if (header != null && !header.isBlank())
        {
            parts.add(header);
        }

        if (rowLabel != null && !rowLabel.isBlank()
            && (header == null || !rowLabel.equalsIgnoreCase(header)))
        {
            parts.add(rowLabel);
        }

        String base;
        if (!parts.isEmpty())
        {
            base = String.join("_", parts);
        }
        else
        {
            base = sheet.getSheetName() + "_R" + (row + 1) + "C" + (col + 1);
        }

        return base;
    }

    private static String findHeader(Sheet sheet, int row, int col)
    {
        for (int r = row - 1; r >= 0; r--)
        {
            Row rr = sheet.getRow(r);
            if (rr == null)
            {
                continue;
            }

            Cell cc = rr.getCell(col);
            if (cc == null)
            {
                continue;
            }

            String text = getCellString(cc);
            if (text != null && !text.isBlank())
            {
                return text;
            }
        }
        return null;
    }

    private static String findRowLabel(Sheet sheet, int row, int col)
    {
        Row rr = sheet.getRow(row);
        if (rr == null)
        {
            return null;
        }

        for (int c = col - 1; c >= 0; c--)
        {
            Cell cc = rr.getCell(c);
            if (cc == null)
            {
                continue;
            }

            String text = getCellString(cc);
            if (text != null && !text.isBlank())
            {
                return text;
            }
        }
        return null;
    }

    /**
     * Strip any [[...]] tags from visible cell content.
     */
    private static String stripTags(String s)
    {
        if (s == null)
        {
            return null;
        }

        String out = s;
        int start = out.indexOf("[[");
        while (start >= 0)
        {
            int end = out.indexOf("]]", start + 2);
            if (end < 0)
            {
                break;
            }
            out = out.substring(0, start) + out.substring(end + 2);
            start = out.indexOf("[[");
        }
        return out.trim();
    }

    /**
     * Infer Java type from the cell when no explicit [[field type=...]] is provided.
     */
    private static String inferJavaType(Cell cell)
    {
        if (cell == null)
        {
            return "java.lang.String";
        }

        CellType t = cell.getCellType();
        switch (t)
        {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell))
                {
                    return "java.util.Date";
                }
                return "java.lang.Double";

            case BOOLEAN:
                return "java.lang.Boolean";

            case FORMULA:
                return "java.lang.Double";

            default:
                return "java.lang.String";
        }
    }

    /**
     * Map short type names to fully-qualified Java class names.
     */
    private static String mapToJavaFqcn(String t)
    {
        if (t == null || t.isBlank())
        {
            return "java.lang.String";
        }

        return switch (t)
        {
            case "String"     -> "java.lang.String";
            case "Double"     -> "java.lang.Double";
            case "Integer"    -> "java.lang.Integer";
            case "Long"       -> "java.lang.Long";
            case "BigDecimal" -> "java.math.BigDecimal";
            case "Date"       -> "java.util.Date";
            case "Boolean"    -> "java.lang.Boolean";
            default           -> "java.lang.String";
        };
    }

    /**
     * Parse a section name into a Section enum. Defaults to DETAIL.
     */
    private static Section parseSection(String s)
    {
        if (s == null)
        {
            return Section.DETAIL;
        }

        String v = s.trim().toLowerCase(Locale.ROOT);

        switch (v)
        {
            case "background":
                return Section.BACKGROUND;
            case "title":
                return Section.TITLE;
            case "pageheader":
            case "page_header":
                return Section.PAGE_HEADER;
            case "columnheader":
            case "column_header":
                return Section.COLUMN_HEADER;
            case "columnfooter":
            case "column_footer":
                return Section.COLUMN_FOOTER;
            case "pagefooter":
            case "page_footer":
                return Section.PAGE_FOOTER;
            case "summary":
                return Section.SUMMARY;
            case "detail":
            default:
                return Section.DETAIL;
        }
    }

    /**
     * Is this cell part of a magenta (#FF00FF) filled rectangle?
     */
    private static boolean isMagentaCell(Cell cell)
    {
        if (cell == null)
        {
            return false;
        }

        CellStyle style = cell.getCellStyle();
        if (style == null)
        {
            return false;
        }

        if (style.getFillPattern() != FillPatternType.SOLID_FOREGROUND)
        {
            return false;
        }

        Color fg = style.getFillForegroundColorColor();
        String argb = colorToARGB(fg);
        if (argb == null)
        {
            return false;
        }

        // Magenta: A=FF, R=FF, G=00, B=FF
        return "FFFF00FF".equalsIgnoreCase(argb);
    }

    // ---------------------------------------------------------------------
    // Annotation helpers: [[field ...]] and [[band ...]] and [[section ...]]
    // ---------------------------------------------------------------------

    static class FieldHint
    {
        String name;        // override field name
        String type;        // String, Double, Date, Integer, Long, BigDecimal
        String pattern;     // JR pattern (e.g., "$ #,##0.00")
        String align;       // Left, Center, Right, Justified
        boolean force;      // treat as dynamic even if cell not colored
        String section;     // optional section name (detail, pageHeader, pageFooter, etc.)
    }

    static class BandHint
    {
        String name;
        Integer idx;
        Integer height;
        String split;
        String printWhenExpr;
    }

    private static FieldHint readFieldHint(Cell cell)
    {
        if (cell == null)
        {
            return null;
        }

        String raw = null;
        if (cell.getCellComment() != null)
        {
            raw = cell.getCellComment().getString().getString();
        }
        else if (cell.getCellType() == CellType.STRING)
        {
            raw = cell.getStringCellValue();
        }

        if (raw == null)
        {
            return null;
        }

        raw = raw.trim();
        FieldHint h = null;

        // [[field ...]] tag
        if (raw.contains("[[field"))
        {
            int s = raw.indexOf("[[field");
            int e = raw.indexOf("]]", s);
            if (s >= 0 && e > s)
            {
                String body = raw.substring(s + 2, e); // "field name=... type=... ..."
                String[] toks = body.split("\\s+");

                h = new FieldHint();
                for (int i = 1; i < toks.length; i++)
                {
                    String t = toks[i];
                    int eq = t.indexOf('=');
                    if (eq <= 0)
                    {
                        continue;
                    }

                    String k = t.substring(0, eq);
                    String v = t.substring(eq + 1);
                    if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2)
                    {
                        v = v.substring(1, v.length() - 1);
                    }

                    switch (k)
                    {
                        case "name"    -> h.name = v;
                        case "type"    -> h.type = v;
                        case "pattern" -> h.pattern = v;
                        case "align"   -> h.align = v;
                        case "force"   -> h.force = "1".equals(v) || "true".equalsIgnoreCase(v);
                        case "section" -> h.section = v;
						default -> throw new IllegalArgumentException("Unexpected value: " + k);
                    }
                }
            }
        }

        // Standalone [[section ...]] tag (works for static or dynamic cells)
        String sectionFromTag = extractSectionTag(raw);
        if (sectionFromTag != null && !sectionFromTag.isBlank())
        {
            if (h == null)
            {
                h = new FieldHint();
            }
            h.section = sectionFromTag;
        }

        return h;
    }

    /**
     * Extract section name from a [[section ...]] tag.
     *
     * Supported forms:
     *   [[section pageHeader]]
     *   [[section name=pageHeader]]
     *   [[section section=pageHeader]]
     */
    private static String extractSectionTag(String raw)
    {
        if (raw == null)
        {
            return null;
        }

        int s = raw.indexOf("[[section");
        if (s < 0)
        {
            return null;
        }
        int e = raw.indexOf("]]", s);
        if (e < 0)
        {
            return null;
        }

        String body = raw.substring(s + 2, e); // "section pageHeader" or "section name=pageHeader"
        String[] toks = body.split("\\s+");
        for (int i = 1; i < toks.length; i++)
        {
            String t = toks[i];
            int eq = t.indexOf('=');
            if (eq > 0)
            {
                String k = t.substring(0, eq);
                String v = t.substring(eq + 1);
                if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2)
                {
                    v = v.substring(1, v.length() - 1);
                }
                if ("name".equals(k) || "section".equals(k))
                {
                    return v;
                }
            }
            else if (!t.isBlank())
            {
                // First bare token after "section" is the section name
                return t;
            }
        }
        return null;
    }

    private static BandHint readBandHint(Cell cell)
    {
        if (cell == null)
        {
            return null;
        }

        String raw = null;
        if (cell.getCellComment() != null)
        {
            raw = cell.getCellComment().getString().getString();
        }
        else if (cell.getCellType() == CellType.STRING)
        {
            raw = cell.getStringCellValue();
        }

        if (raw == null)
        {
            return null;
        }

        raw = raw.trim();
        if (!raw.contains("[[band"))
        {
            return null;
        }

        int s = raw.indexOf("[[band");
        int e = raw.indexOf("]]", s);
        if (s < 0 || e < 0)
        {
            return null;
        }

        String body = raw.substring(s + 2, e); // "band name=... idx=... ..."
        String[] toks = body.split("\\s+");

        BandHint h = new BandHint();
        for (int i = 1; i < toks.length; i++)
        {
            String t = toks[i];
            int eq = t.indexOf('=');
            if (eq <= 0)
            {
                continue;
            }

            String k = t.substring(0, eq);
            String v = t.substring(eq + 1);
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2)
            {
                v = v.substring(1, v.length() - 1);
            }

            switch (k)
            {
                case "name"      -> h.name = v;
                case "idx"       -> h.idx = safeInt(v);
                case "height"    -> h.height = safeInt(v);
                case "split"     -> h.split = v;
                case "printWhen" -> h.printWhenExpr = v;
				default -> throw new IllegalArgumentException("Unexpected value: " + k);
            }
        }

        return h;
    }

    private static Integer safeInt(String v)
    {
        try
        {
            return Integer.valueOf(v);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}
