package com.acme.jrgen;

import com.acme.jrgen.CellItem.Role;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans Excel sheets and produces SheetModel objects according to the
 * authoring rules described in the specification.
 */
public class ExcelScanner
{
    private static final double MIN_FONT = 6.0;
    private static final double MAX_FONT = 22.0;
    private static final int DEFAULT_TITLE_HEIGHT = 60;

    private static final Pattern TAG_PATTERN = Pattern.compile("\\[\\[([^]]+)]]");

    private final Path excelPath;
    private final int pageWidth;
    private final int pageHeight;
    private final int leftMargin;
    private final int rightMargin;
    private final int topMargin;
    private final int bottomMargin;

    public ExcelScanner(Path excelPath,
                        int pageWidth,
                        int pageHeight,
                        int leftMargin,
                        int rightMargin,
                        int topMargin,
                        int bottomMargin)
    {
        this.excelPath = excelPath;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.leftMargin = leftMargin;
        this.rightMargin = rightMargin;
        this.topMargin = topMargin;
        this.bottomMargin = bottomMargin;
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

                SheetContext ctx = new SheetContext(sheet);

                List<BandSpec> bands = detectBands(ctx);
                if (bands.isEmpty())
                {
                    BandSpec defaultBand = new BandSpec(
                        "Detail",
                        1,
                        0,
                        0,
                        ctx.totalWidthScaled,
                        ctx.totalHeightScaled,
                        "Stretch",
                        null,
                        0,
                        ctx.maxRow,
                        0,
                        ctx.maxCol
                    );
                    bands = List.of(defaultBand);
                }

                List<CellItem> items = new ArrayList<>();
                Map<String, Integer> nameCounts = new HashMap<>();
                Map<String, FieldInfo> fields = new LinkedHashMap<>();

                for (Row row : sheet)
                {
                    for (Cell cell : row)
                    {
                        if (ctx.isMergedAndNotTopLeft(cell))
                        {
                            continue;
                        }

                        ParsedFieldTag tag = ctx.parseFieldTag(cell);
                        Role role = ctx.classify(cell, tag);
                        if (role == null)
                        {
                            continue;
                        }

                        int colIdx = cell.getColumnIndex();
                        int rowIdx = cell.getRowIndex();
                        double x = ctx.positionX(colIdx);
                        double y = ctx.positionY(rowIdx);

                        double width = ctx.width(colIdx, cell);
                        double height = ctx.height(rowIdx, cell);

                        BandSpec band = findBandForCell(bands, rowIdx, colIdx);
                        double relX = x - band.x();
                        double relY = y - band.y();

                        String value = ctx.getCellDisplayText(cell);
                        String fieldName = null;
                        String javaType = null;
                        String pattern = null;
                        String alignment = null;

                        if (role == Role.DYNAMIC)
                        {
                            String baseName = (tag != null && tag.name != null)
                                ? tag.name
                                : buildBaseName(sheet, cell);
                            fieldName = uniquify(baseName, nameCounts);

                            javaType = (tag != null && tag.type != null)
                                ? tag.type
                                : ctx.inferType(cell);
                            pattern = (tag != null && tag.pattern != null)
                                ? tag.pattern
                                : ctx.inferPattern(cell, javaType);
                            alignment = (tag != null && tag.alignment != null)
                                ? tag.alignment
                                : ctx.inferAlignment(cell);

                            fields.put(fieldName, new FieldInfo(
                                fieldName,
                                javaType,
                                pattern,
                                alignment,
                                ctx.excelFormat(cell)
                            ));
                        }
                        else
                        {
                            value = ctx.stripTag(value);
                        }

                        CellItem item = new CellItem(
                            colIdx,
                            rowIdx,
                            relX,
                            relY,
                            width,
                            height,
                            value,
                            role,
                            fieldName,
                            javaType,
                            pattern,
                            alignment,
                            ctx.fontSize(cell),
                            ctx.isBold(cell),
                            ctx.excelFormat(cell),
                            band
                        );
                        items.add(item);
                    }
                }

                int titleHeight = DEFAULT_TITLE_HEIGHT;
                result.add(new SheetModel(name, items, bands, fields, titleHeight));
            }

            return result;
        }
    }

    private static BandSpec findBandForCell(List<BandSpec> bands, int rowIdx, int colIdx)
    {
        for (BandSpec b : bands)
        {
            if (b.containsCell(rowIdx, colIdx))
            {
                return b;
            }
        }
        return bands.get(0);
    }

    private List<BandSpec> detectBands(SheetContext ctx)
    {
        Map<String, Integer> nameCounts = new HashMap<>();
        List<BandSpec> bands = new ArrayList<>();
        Set<CellPos> visited = new HashSet<>();

        for (int r = 0; r <= ctx.maxRow; r++)
        {
            Row row = ctx.sheet.getRow(r);
            if (row == null)
            {
                continue;
            }
            for (int c = 0; c <= ctx.maxCol; c++)
            {
                Cell cell = row.getCell(c);
                if (cell == null || visited.contains(new CellPos(r, c)))
                {
                    continue;
                }
                if (!ctx.isMagenta(cell))
                {
                    continue;
                }

                Set<CellPos> component = ctx.collectComponent(r, c, visited);
                Rect rect = Rect.fromCells(component);
                Cell topLeft = ctx.sheet.getRow(rect.top).getCell(rect.left);
                ParsedBandTag tag = ctx.parseBandTag(topLeft);

                String baseName = (tag != null && tag.name != null) ? tag.name : "Band";
                int order = (tag != null && tag.idx != null) ? tag.idx : bands.size() + 1;
                String name = uniquify(baseName, nameCounts);

                double x = ctx.positionX(rect.left);
                double y = ctx.positionY(rect.top);
                double width = ctx.width(rect.left, rect.right);
                double height = (tag != null && tag.height != null)
                    ? tag.height
                    : ctx.height(rect.top, rect.bottom);

                BandSpec spec = new BandSpec(
                    name,
                    order,
                    x,
                    y,
                    width,
                    height,
                    tag != null ? tag.splitType : "Stretch",
                    tag != null ? tag.printWhen : null,
                    rect.top,
                    rect.bottom,
                    rect.left,
                    rect.right
                );
                bands.add(spec);
            }
        }

        bands.sort(Comparator.comparingInt(BandSpec::order)
            .thenComparingDouble(BandSpec::y)
            .thenComparingDouble(BandSpec::x));

        return bands;
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

    private static class ParsedFieldTag
    {
        final String name;
        final String type;
        final String pattern;
        final String alignment;
        final boolean force;

        ParsedFieldTag(String name, String type, String pattern, String alignment, boolean force)
        {
            this.name = name;
            this.type = type;
            this.pattern = pattern;
            this.alignment = alignment;
            this.force = force;
        }
    }

    private static class ParsedBandTag
    {
        final String name;
        final Integer idx;
        final Double height;
        final String splitType;
        final String printWhen;

        ParsedBandTag(String name, Integer idx, Double height, String splitType, String printWhen)
        {
            this.name = name;
            this.idx = idx;
            this.height = height;
            this.splitType = splitType;
            this.printWhen = printWhen;
        }
    }

    private static class Rect
    {
        final int top;
        final int bottom;
        final int left;
        final int right;

        Rect(int top, int bottom, int left, int right)
        {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;
        }

        static Rect fromCells(Set<CellPos> cells)
        {
            int top = cells.stream().mapToInt(cp -> cp.row).min().orElse(0);
            int bottom = cells.stream().mapToInt(cp -> cp.row).max().orElse(0);
            int left = cells.stream().mapToInt(cp -> cp.col).min().orElse(0);
            int right = cells.stream().mapToInt(cp -> cp.col).max().orElse(0);
            return new Rect(top, bottom, left, right);
        }
    }

    private record CellPos(int row, int col){}

    private class SheetContext
    {
        private final Sheet sheet;
        private final double[] colWidths;
        private final double[] rowHeights;
        private final double scaleX;
        private final double scaleY;
        private final int maxRow;
        private final int maxCol;
        private final double totalWidthScaled;
        private final double totalHeightScaled;
        private final List<CellRangeAddress> merges;

        SheetContext(Sheet sheet)
        {
            this.sheet = sheet;
            this.maxRow = sheet.getLastRowNum();
            this.maxCol = findMaxColumn(sheet);
            this.colWidths = new double[maxCol + 1];
            this.rowHeights = new double[maxRow + 1];

            for (int c = 0; c <= maxCol; c++)
            {
                colWidths[c] = sheet.getColumnWidth(c) / 256.0;
            }

            double defaultRowHeight = sheet.getDefaultRowHeightInPoints();
            for (int r = 0; r <= maxRow; r++)
            {
                Row row = sheet.getRow(r);
                rowHeights[r] = (row == null) ? defaultRowHeight : row.getHeightInPoints();
            }

            double totalWidth = Arrays.stream(colWidths).sum();
            double totalHeight = Arrays.stream(rowHeights).sum();

            double availableWidth = pageWidth - leftMargin - rightMargin;
            double availableHeight = pageHeight - topMargin - bottomMargin - DEFAULT_TITLE_HEIGHT;

            this.scaleX = (totalWidth == 0) ? 1.0 : availableWidth / totalWidth;
            this.scaleY = (totalHeight == 0) ? 1.0 : availableHeight / totalHeight;
            this.totalWidthScaled = totalWidth * scaleX;
            this.totalHeightScaled = totalHeight * scaleY;
            this.merges = sheet.getMergedRegions();
        }

        double positionX(int col)
        {
            double sum = 0;
            for (int c = 0; c < col; c++)
            {
                sum += colWidths[c];
            }
            return sum * scaleX;
        }

        double positionY(int row)
        {
            double sum = 0;
            for (int r = 0; r < row; r++)
            {
                sum += rowHeights[r];
            }
            return sum * scaleY;
        }

        double width(int startCol, Cell cell)
        {
            int endCol = startCol;
            CellRangeAddress region = findMerge(cell);
            if (region != null)
            {
                endCol = region.getLastColumn();
            }
            return width(startCol, endCol);
        }

        double width(int startCol, int endCol)
        {
            double sum = 0;
            for (int c = startCol; c <= endCol && c < colWidths.length; c++)
            {
                sum += colWidths[c];
            }
            return sum * scaleX;
        }

        double height(int startRow, Cell cell)
        {
            int endRow = startRow;
            CellRangeAddress region = findMerge(cell);
            if (region != null)
            {
                endRow = region.getLastRow();
            }
            return height(startRow, endRow);
        }

        double height(int startRow, int endRow)
        {
            double sum = 0;
            for (int r = startRow; r <= endRow && r < rowHeights.length; r++)
            {
                sum += rowHeights[r];
            }
            return sum * scaleY;
        }

        boolean isMergedAndNotTopLeft(Cell cell)
        {
            CellRangeAddress region = findMerge(cell);
            if (region == null)
            {
                return false;
            }
            return !(cell.getRowIndex() == region.getFirstRow()
                && cell.getColumnIndex() == region.getFirstColumn());
        }

        CellRangeAddress findMerge(Cell cell)
        {
            if (cell == null)
            {
                return null;
            }
            int r = cell.getRowIndex();
            int c = cell.getColumnIndex();
            for (CellRangeAddress reg : merges)
            {
                if (reg.isInRange(r, c))
                {
                    return reg;
                }
            }
            return null;
        }

        Role classify(Cell cell, ParsedFieldTag tag)
        {
            if (tag != null && tag.force)
            {
                return Role.DYNAMIC;
            }

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
                    if (isBlue(fg))
                    {
                        return Role.DYNAMIC;
                    }
                    if (isMagenta(fg))
                    {
                        return null;
                    }
                    if (isGreen(fg))
                    {
                        return null;
                    }
                }
            }

            String v = getCellDisplayText(cell);
            if (v == null || v.isBlank())
            {
                return null;
            }

            return Role.STATIC;
        }

        String getCellDisplayText(Cell cell)
        {
            String raw = getCellString(cell);
            return stripTag(raw);
        }

        String stripTag(String raw)
        {
            if (raw == null)
            {
                return null;
            }
            Matcher m = TAG_PATTERN.matcher(raw);
            return m.replaceAll("").trim();
        }

        boolean isMagenta(Cell cell)
        {
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
            return isMagenta(fg);
        }

        boolean isMagenta(Color color)
        {
            if (color instanceof XSSFColor xc)
            {
                byte[] rgb = xc.getRGB();
                if (rgb != null)
                {
                    return (rgb[0] & 0xFF) == 0xFF
                        && (rgb[1] & 0xFF) == 0x00
                        && (rgb[2] & 0xFF) == 0xFF;
                }
            }
            return false;
        }

        boolean isGreen(Color c)
        {
            if (c instanceof XSSFColor xc)
            {
                byte[] rgb = xc.getRGB();
                if (rgb != null)
                {
                    return (rgb[0] & 0xFF) == 0xCC && (rgb[1] & 0xFF) == 0xFF && (rgb[2] & 0xFF) == 0xCC
                        || (rgb[0] & 0xFF) == 0xC6 && (rgb[1] & 0xFF) == 0xEF && (rgb[2] & 0xFF) == 0xCE;
                }
            }
            return false;
        }

        boolean isBlue(Color c)
        {
            if (c instanceof XSSFColor xc)
            {
                if (xc.getIndex() == 41)
                {
                    return true;
                }

                byte[] rgb = xc.getRGB();
                if (rgb != null)
                {
                    int r = rgb[0] & 0xFF;
                    int g = rgb[1] & 0xFF;
                    int b = rgb[2] & 0xFF;
                    return b > (r + g) / 2 && b > 80;
                }
            }
            return false;
        }

        ParsedFieldTag parseFieldTag(Cell cell)
        {
            String src = null;
            if (cell.getCellComment() != null)
            {
                src = cell.getCellComment().getString().getString();
            }
            else
            {
                src = getCellString(cell);
            }

            if (src == null)
            {
                return null;
            }

            Matcher m = TAG_PATTERN.matcher(src);
            while (m.find())
            {
                String inner = m.group(1).trim();
                if (inner.startsWith("field"))
                {
                    Map<String, String> attrs = parseAttributes(inner.substring("field".length()));
                    String name = attrs.get("name");
                    String type = attrs.get("type");
                    String pattern = attrs.get("pattern");
                    String align = attrs.get("align");
                    boolean force = "1".equals(attrs.get("force"));
                    return new ParsedFieldTag(name, type, pattern, align, force);
                }
            }
            return null;
        }

        ParsedBandTag parseBandTag(Cell cell)
        {
            if (cell == null)
            {
                return null;
            }
            String src = getCellString(cell);
            Matcher m = TAG_PATTERN.matcher(src);
            while (m.find())
            {
                String inner = m.group(1).trim();
                if (inner.startsWith("band"))
                {
                    Map<String, String> attrs = parseAttributes(inner.substring("band".length()));
                    String name = attrs.get("name");
                    Integer idx = attrs.containsKey("idx") ? Integer.parseInt(attrs.get("idx")) : null;
                    Double height = attrs.containsKey("height") ? Double.parseDouble(attrs.get("height")) : null;
                    String split = attrs.getOrDefault("split", "Stretch");
                    String printWhen = attrs.get("printWhen");
                    return new ParsedBandTag(name, idx, height, split, printWhen);
                }
            }
            return null;
        }

        Map<String, String> parseAttributes(String src)
        {
            Map<String, String> attrs = new HashMap<>();
            Matcher m = Pattern.compile("([a-zA-Z]+)=\\"?([^\\"\\s]+[^\\"]*)\\"?").matcher(src);
            while (m.find())
            {
                attrs.put(m.group(1).trim(), m.group(2).trim());
            }
            return attrs;
        }

        Set<CellPos> collectComponent(int startRow, int startCol, Set<CellPos> visited)
        {
            Set<CellPos> component = new HashSet<>();
            Deque<CellPos> stack = new ArrayDeque<>();
            stack.push(new CellPos(startRow, startCol));

            while (!stack.isEmpty())
            {
                CellPos pos = stack.pop();
                if (visited.contains(pos))
                {
                    continue;
                }
                visited.add(pos);
                component.add(pos);

                int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
                for (int[] d : dirs)
                {
                    int nr = pos.row + d[0];
                    int nc = pos.col + d[1];
                    if (nr < 0 || nc < 0 || nr > maxRow || nc > maxCol)
                    {
                        continue;
                    }
                    Row row = sheet.getRow(nr);
                    if (row == null)
                    {
                        continue;
                    }
                    Cell neighbor = row.getCell(nc);
                    if (neighbor != null && isMagenta(neighbor))
                    {
                        CellPos np = new CellPos(nr, nc);
                        if (!visited.contains(np))
                        {
                            stack.push(np);
                        }
                    }
                }
            }
            return component;
        }

        String inferType(Cell cell)
        {
            if (DateUtil.isCellDateFormatted(cell))
            {
                return "java.util.Date";
            }
            if (cell.getCellType() == CellType.NUMERIC)
            {
                return "java.lang.Double";
            }
            return "java.lang.String";
        }

        String inferPattern(Cell cell, String javaType)
        {
            String fmt = excelFormat(cell);
            if (fmt == null)
            {
                return null;
            }

            if (fmt.contains("[$") || fmt.contains("$"))
            {
                return "$ #,##0.00";
            }
            if (DateUtil.isCellDateFormatted(cell) || "java.util.Date".equals(javaType))
            {
                return "MMM d, yyyy";
            }
            if (fmt.contains("0") || fmt.contains("#") || fmt.contains(","))
            {
                return "#,##0.###";
            }
            return null;
        }

        String excelFormat(Cell cell)
        {
            CellStyle style = cell.getCellStyle();
            if (style == null)
            {
                return null;
            }
            return style.getDataFormatString();
        }

        String inferAlignment(Cell cell)
        {
            HorizontalAlignment ha = cell.getCellStyle() != null
                ? cell.getCellStyle().getAlignment()
                : HorizontalAlignment.LEFT;

            return switch (ha)
            {
                case RIGHT -> "Right";
                case CENTER -> "Center";
                case JUSTIFY -> "Justified";
                default -> "Left";
            };
        }

        double fontSize(Cell cell)
        {
            CellStyle style = cell.getCellStyle();
            if (style == null)
            {
                return clampFont(10.0 * scaleY);
            }
            Font font = sheet.getWorkbook().getFontAt(style.getFontIndexAsInt());
            double size = font.getFontHeightInPoints();
            return clampFont(size * scaleY);
        }

        boolean isBold(Cell cell)
        {
            CellStyle style = cell.getCellStyle();
            if (style == null)
            {
                return false;
            }
            Font font = sheet.getWorkbook().getFontAt(style.getFontIndexAsInt());
            return font.getBold();
        }

        private double clampFont(double size)
        {
            if (size < MIN_FONT)
            {
                return MIN_FONT;
            }
            if (size > MAX_FONT)
            {
                return MAX_FONT;
            }
            return size;
        }
    }

    private static int findMaxColumn(Sheet sheet)
    {
        int max = 0;
        for (Row row : sheet)
        {
            int last = row.getLastCellNum();
            if (last > 0 && (last - 1) > max)
            {
                max = last - 1;
            }
        }
        return max;
    }
}
