package com.acme.jrgen;

import com.acme.jrgen.CellItem.Role;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans Excel sheets and produces SheetModel objects using the richer authoring rules.
 */
public class ExcelScanner
{
    private static final Pattern TAG_PATTERN = Pattern.compile("\\[\\[(field|band)\\s+([^]]+)\\]\\]");

    private final Path excelPath;

    public ExcelScanner(Path excelPath)
    {
        this.excelPath = excelPath;
    }

    public List<SheetModel> scan(List<String> restrictToSheets) throws Exception
    {
        try (InputStream in = Files.newInputStream(excelPath);
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

                SheetModel model = scanSheet(sheet);
                result.add(model);
            }

            return result;
        }
    }

    private SheetModel scanSheet(Sheet sheet)
    {
        Map<String, Integer> nameCounts = new HashMap<>();
        Map<String, CellRangeAddress> mergedTopLeft = new HashMap<>();
        Map<String, CellRangeAddress> mergedLookup = new HashMap<>();

        for (CellRangeAddress cra : sheet.getMergedRegions())
        {
            mergedLookup.put(cra.getFirstRow() + ":" + cra.getFirstColumn(), cra);
            for (int r = cra.getFirstRow(); r <= cra.getLastRow(); r++)
            {
                for (int c = cra.getFirstColumn(); c <= cra.getLastColumn(); c++)
                {
                    mergedTopLeft.put(r + ":" + c, cra);
                }
            }
        }

        int maxCol = 0;
        for (Row r : sheet)
        {
            if (r.getLastCellNum() > maxCol)
            {
                maxCol = r.getLastCellNum();
            }
        }

        double[] colWidths = new double[maxCol + 1];
        for (int c = 0; c <= maxCol; c++)
        {
            colWidths[c] = pxToPoints(SheetUtil.getColumnWidth(sheet, c, false));
        }

        List<Double> rowHeights = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();
        for (int r = 0; r <= lastRow; r++)
        {
            Row row = sheet.getRow(r);
            double h = (row == null) ? sheet.getDefaultRowHeightInPoints() : row.getHeightInPoints();
            rowHeights.add(h);
        }

        double totalWidth = Arrays.stream(colWidths).sum();
        double totalHeight = rowHeights.stream().mapToDouble(Double::doubleValue).sum();

        List<Band> bands = buildBands(sheet, colWidths, rowHeights);

        for (Band b : bands)
        {
            List<CellItem> bandItems = new ArrayList<>();
            for (Row row : sheet)
            {
                for (Cell cell : row)
                {
                    int colIdx = cell.getColumnIndex();
                    int rowIdx = cell.getRowIndex();

                    if (isMergedButNotTopLeft(mergedLookup, mergedTopLeft, rowIdx, colIdx))
                    {
                        continue;
                    }

                    if (!isInsideBand(b, rowIdx, colIdx))
                    {
                        continue;
                    }

                    Role role = classify(cell);
                    String value = getCellString(cell);

                    Optional<Map<String, String>> tag = readTag(cell, "field");
                    boolean forceDynamic = tag.map(m -> "1".equals(m.get("force"))).orElse(false);

                    if (!forceDynamic && role == null)
                    {
                        continue;
                    }

                    if (isMagenta(cell))
                    {
                        continue;
                    }

                    boolean render = true;
                    if (tag.isPresent())
                    {
                        render = false; // hide inline tag text
                    }

                    FieldSpec fieldSpec = null;
                    Role effectiveRole = role;

                    if (forceDynamic || tag.isPresent() || cell.getCellType() == CellType.FORMULA)
                    {
                        effectiveRole = Role.DYNAMIC;
                    }

                    if (effectiveRole == Role.DYNAMIC)
                    {
                        String baseName = tag.map(m -> m.get("name")).orElse(buildBaseName(sheet, cell));
                        String fieldName = uniquify(baseName, nameCounts);
                        String type = tag.map(m -> m.get("type")).orElse(inferType(cell));
                        String pattern = tag.map(m -> m.get("pattern")).orElse(inferPattern(cell));
                        String align = tag.map(m -> m.get("align")).orElse(defaultAlignment(type));
                        String originalFormat = cell.getCellStyle() == null ? null : cell.getCellStyle().getDataFormatString();

                        if (tag.isPresent() && tag.get().containsKey("name"))
                        {
                            fieldName = tag.get().get("name");
                        }
                        fieldSpec = new FieldSpec(fieldName, type, pattern, align, forceDynamic, originalFormat);
                    }

                    double rawX = sum(colWidths, 0, colIdx);
                    double rawY = sum(rowHeights, 0, rowIdx);
                    double rawW = mergedWidth(colWidths, mergedLookup, rowIdx, colIdx);
                    double rawH = mergedHeight(rowHeights, mergedLookup, rowIdx, colIdx);

                    double fontSize = readFontSize(cell);
                    String alignment = (fieldSpec != null && fieldSpec.alignment() != null)
                        ? fieldSpec.alignment()
                        : readAlignment(cell);

                    bandItems.add(new CellItem(
                        colIdx,
                        rowIdx,
                        rawX - b.originX(),
                        rawY - b.originY(),
                        rawW,
                        rawH,
                        cleanValue(value, tag),
                        effectiveRole,
                        fieldSpec,
                        fontSize,
                        alignment,
                        render
                    ));
                }
            }
            bandItems.sort(Comparator.comparingInt(CellItem::row).thenComparingInt(CellItem::col));
            bands.set(bands.indexOf(b), new Band(
                b.name(),
                b.idx(),
                b.startRow(),
                b.endRow(),
                b.startCol(),
                b.endCol(),
                b.originX(),
                b.originY(),
                b.width(),
                b.height(),
                b.explicitHeight(),
                b.splitType(),
                b.printWhenExpression(),
                bandItems
            ));
        }

        bands.sort(Comparator
            .comparing((Band b) -> b.idx() == null ? Integer.MAX_VALUE : b.idx())
            .thenComparingDouble(Band::originY)
            .thenComparingDouble(Band::originX));

        return new SheetModel(sheet.getSheetName(), totalWidth, totalHeight, bands);
    }

    private static List<Band> buildBands(Sheet sheet, double[] colWidths, List<Double> rowHeights)
    {
        List<Band> bands = new ArrayList<>();
        boolean hasMagenta = false;
        Set<String> visited = new HashSet<>();

        for (Row row : sheet)
        {
            for (Cell cell : row)
            {
                int r = cell.getRowIndex();
                int c = cell.getColumnIndex();
                String key = r + ":" + c;
                if (visited.contains(key))
                {
                    continue;
                }

                if (isMagenta(cell))
                {
                    hasMagenta = true;
                    Band rect = captureRectangle(sheet, r, c, visited, colWidths, rowHeights);
                    bands.add(rect);
                }
            }
        }

        if (!hasMagenta)
        {
            double height = rowHeights.stream().mapToDouble(Double::doubleValue).sum();
            double width = Arrays.stream(colWidths).sum();
            bands.add(new Band("detail", null, 0, rowHeights.size() - 1, 0, colWidths.length - 1, 0, 0, width, height, null, "Stretch", null, new ArrayList<>()));
        }

        return bands;
    }

    private static Band captureRectangle(Sheet sheet,
                                         int startRow,
                                         int startCol,
                                         Set<String> visited,
                                         double[] colWidths,
                                         List<Double> rowHeights)
    {
        int endCol = startCol;
        int endRow = startRow;

        while (isMagenta(sheet, startRow, endCol + 1))
        {
            endCol++;
        }

        while (isMagenta(sheet, endRow + 1, startCol))
        {
            endRow++;
        }

        for (int r = startRow; r <= endRow; r++)
        {
            for (int c = startCol; c <= endCol; c++)
            {
                if (!isMagenta(sheet, r, c))
                {
                    continue;
                }
                visited.add(r + ":" + c);
            }
        }

        Cell topLeft = sheet.getRow(startRow).getCell(startCol);
        Optional<Map<String, String>> tag = readTag(topLeft, "band");
        String name = tag.map(m -> m.getOrDefault("name", "detail" + startRow)).orElse("detail" + startRow);
        Integer idx = tag.map(m -> m.get("idx")).map(Integer::valueOf).orElse(null);
        Double explicitHeight = tag.map(m -> m.get("height")).map(Double::valueOf).orElse(null);
        String splitType = tag.map(m -> m.get("split")).orElse("Stretch");
        String printWhen = tag.map(m -> m.get("printWhen")).orElse(null);

        double originX = sum(colWidths, 0, startCol);
        double originY = sum(rowHeights, 0, startRow);
        double width = sum(colWidths, startCol, endCol + 1);
        double height = sum(rowHeights, startRow, endRow + 1);

        return new Band(name, idx, startRow, endRow, startCol, endCol, originX, originY, width, height, explicitHeight, splitType, printWhen, new ArrayList<>());
    }

    private static boolean isMagenta(Cell cell)
    {
        return isMagenta(cell.getSheet(), cell.getRowIndex(), cell.getColumnIndex());
    }

    private static boolean isMagenta(Sheet sheet, int row, int col)
    {
        Row r = sheet.getRow(row);
        if (r == null)
        {
            return false;
        }
        Cell c = r.getCell(col);
        if (c == null)
        {
            return false;
        }

        CellStyle style = c.getCellStyle();
        if (style == null)
        {
            return false;
        }

        if (style.getFillPattern() != FillPatternType.SOLID_FOREGROUND)
        {
            return false;
        }

        Color fg = style.getFillForegroundColorColor();
        if (fg instanceof XSSFColor xc)
        {
            byte[] rgb = xc.getRGB();
            if (rgb != null)
            {
                return (rgb[0] & 0xFF) == 0xFF && (rgb[1] & 0xFF) == 0x00 && (rgb[2] & 0xFF) == 0xFF;
            }
        }
        return false;
    }

    private static boolean isInsideBand(Band band, int row, int col)
    {
        return row >= band.startRow() && row <= band.endRow()
            && col >= band.startCol() && col <= band.endCol();
    }

    private static boolean isMergedButNotTopLeft(Map<String, CellRangeAddress> mergedLookup,
                                                 Map<String, CellRangeAddress> mergedTopLeft,
                                                 int row,
                                                 int col)
    {
        CellRangeAddress cra = mergedTopLeft.get(row + ":" + col);
        if (cra == null)
        {
            return false;
        }
        return !(cra.getFirstRow() == row && cra.getFirstColumn() == col);
    }

    private static Role classify(Cell cell)
    {
        CellStyle style = cell.getCellStyle();
        if (style != null)
        {
            FillPatternType pattern = style.getFillPattern();
            Color fg = style.getFillForegroundColorColor();

            if (pattern == FillPatternType.SOLID_FOREGROUND && fg != null)
            {
                String rgb = colorToARGB(fg);

                if (isBlue(style, fg))
                {
                    return Role.DYNAMIC;
                }

                if ("FFFFFFFF".equalsIgnoreCase(rgb))
                {
                    String v = getCellString(cell);
                    return (v == null || v.isBlank()) ? null : Role.STATIC;
                }
            }
        }

        if (cell.getCellType() == CellType.FORMULA)
        {
            return Role.DYNAMIC;
        }

        String v = getCellString(cell);
        if (v == null || v.isBlank())
        {
            return null;
        }

        return Role.STATIC;
    }

    private static boolean isBlue(CellStyle style, Color fg)
    {
        if (style.getFillPattern() != FillPatternType.SOLID_FOREGROUND)
        {
            return false;
        }

        if (style.getFillForegroundColor() == 41)
        {
            return true;
        }

        if (fg instanceof XSSFColor xc)
        {
            byte[] rgb = xc.getRGB();
            if (rgb != null && rgb.length >= 3)
            {
                int r = rgb[0] & 0xFF;
                int g = rgb[1] & 0xFF;
                int b = rgb[2] & 0xFF;
                return b > r + 20 && b > g + 20;
            }
        }
        return false;
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

        return sanitizeToJavaIdentifier(base);
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

    private static String sanitizeToJavaIdentifier(String raw)
    {
        String s = (raw == null) ? "" : raw.toLowerCase(Locale.ROOT);

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

    private static String uniquify(String base, Map<String, Integer> counts)
    {
        Integer n = counts.get(base);

        if (n == null)
        {
            counts.put(base, 1);
            return base;
        }
        else
        {
            n = n + 1;
            counts.put(base, n);
            return base + "_" + n;
        }
    }

    private static Optional<Map<String, String>> readTag(Cell cell, String kind)
    {
        if (cell == null)
        {
            return Optional.empty();
        }

        String comment = cell.getCellComment() == null ? null : cell.getCellComment().getString().getString();
        String text = cell.getCellType() == CellType.STRING ? cell.getStringCellValue() : null;

        for (String source : List.of(comment, text))
        {
            if (source == null)
            {
                continue;
            }
            Matcher m = TAG_PATTERN.matcher(source);
            if (m.find() && kind.equalsIgnoreCase(m.group(1)))
            {
                String body = m.group(2);
                Map<String, String> attrs = new HashMap<>();
                String[] parts = body.trim().split("\\s+");
                for (String p : parts)
                {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2)
                    {
                        attrs.put(kv[0].trim(), kv[1].replaceAll("^\"|\"$", ""));
                    }
                }
                return Optional.of(attrs);
            }
        }

        return Optional.empty();
    }

    private static String cleanValue(String original, Optional<Map<String, String>> tag)
    {
        if (original == null)
        {
            return "";
        }

        if (tag.isPresent())
        {
            return original.replaceAll("\\[\\[.*?\\]\\]", "").trim();
        }
        return original;
    }

    private static double mergedWidth(double[] colWidths, Map<String, CellRangeAddress> mergedLookup, int row, int col)
    {
        CellRangeAddress cra = mergedLookup.get(row + ":" + col);
        if (cra == null)
        {
            return colWidths[col];
        }
        return sum(colWidths, cra.getFirstColumn(), cra.getLastColumn() + 1);
    }

    private static double mergedHeight(List<Double> rowHeights, Map<String, CellRangeAddress> mergedLookup, int row, int col)
    {
        CellRangeAddress cra = mergedLookup.get(row + ":" + col);
        if (cra == null)
        {
            return rowHeights.get(row);
        }
        return sum(rowHeights, cra.getFirstRow(), cra.getLastRow() + 1);
    }

    private static double sum(double[] values, int fromInclusive, int toExclusive)
    {
        double total = 0;
        for (int i = fromInclusive; i < toExclusive && i < values.length; i++)
        {
            total += values[i];
        }
        return total;
    }

    private static double sum(List<Double> values, int fromInclusive, int toExclusive)
    {
        double total = 0;
        for (int i = fromInclusive; i < toExclusive && i < values.size(); i++)
        {
            total += values.get(i);
        }
        return total;
    }

    private static double pxToPoints(double px)
    {
        return px * 72.0 / 96.0;
    }

    private static double readFontSize(Cell cell)
    {
        CellStyle style = cell.getCellStyle();
        if (style == null)
        {
            return 10;
        }
        Font font = cell.getSheet().getWorkbook().getFontAt(style.getFontIndex());
        return font == null ? 10 : font.getFontHeightInPoints();
    }

    private static String readAlignment(Cell cell)
    {
        HorizontalAlignment align = cell.getCellStyle().getAlignment();
        return switch (align)
        {
            case CENTER, CENTER_SELECTION, JUSTIFY -> "Center";
            case RIGHT, FILL -> "Right";
            default -> "Left";
        };
    }

    private static String inferType(Cell cell)
    {
        if (DateUtil.isCellDateFormatted(cell))
        {
            return "java.util.Date";
        }

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA)
        {
            type = cell.getCachedFormulaResultType();
        }

        return switch (type)
        {
            case NUMERIC -> "java.lang.Double";
            default -> "java.lang.String";
        };
    }

    private static String inferPattern(Cell cell)
    {
        String format = cell.getCellStyle() == null ? null : cell.getCellStyle().getDataFormatString();
        if (format == null)
        {
            return null;
        }

        String lower = format.toLowerCase(Locale.ROOT);
        if (lower.contains("accounting") || lower.contains("$") || lower.contains("[$"))
        {
            return "$ #,##0.00";
        }

        if (DateUtil.isCellDateFormatted(cell))
        {
            return "MMM d, yyyy";
        }

        if (lower.matches(".*[0#/,].*"))
        {
            return "#,##0.###";
        }

        return null;
    }

    private static String defaultAlignment(String type)
    {
        if (type == null)
        {
            return "Left";
        }
        if (type.contains("Double") || type.contains("Integer") || type.contains("Long") || type.contains("BigDecimal"))
        {
            return "Right";
        }
        return "Left";
    }
}
