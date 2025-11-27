package com.acme.jrgen;

import com.acme.jrgen.CellItem.Role;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Scans Excel sheets and produces SheetModel objects.
 *
 * Color rules:
 *   - green fill            -> IGNORE
 *   - non-white solid fill  -> DYNAMIC
 *   - no fill/white + text  -> STATIC
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

                List<CellItem> items = new ArrayList<>();
                Map<String, Integer> nameCounts = new HashMap<>();

                for (Row row : sheet)
                {
                    for (Cell cell : row)
                    {
                        Role role = classify(cell);
                        if (role == null)
                        {
                            continue;
                        }

                        int colIdx = cell.getColumnIndex();
                        int rowIdx = cell.getRowIndex();

                        int x = colIdx * cellW;
                        int y = rowIdx * cellH;

                        String value = getCellString(cell);
                        String fieldName = null;

                        if (role == Role.DYNAMIC)
                        {
                            String baseName = buildBaseName(sheet, cell);
                            fieldName = uniquify(baseName, nameCounts);
                        }

                        items.add(new CellItem(
                            colIdx,
                            rowIdx,
                            x,
                            y,
                            cellW,
                            cellH,
                            value,
                            role,
                            fieldName
                        ));
                    }
                }

                result.add(new SheetModel(name, items));
            }

            return result;
        }
    }

    /**
     * Role classification based on fill + value.
     */
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

                // Greens => ignore
                if ("FFCCFFCC".equalsIgnoreCase(rgb) || "FFC6EFCE".equalsIgnoreCase(rgb))
                {
                    return null; // IGNORE
                }

                // Explicit white => treat as static text
                if ("FFFFFFFF".equalsIgnoreCase(rgb))
                {
                    String v = getCellString(cell);
                    return (v == null || v.isBlank()) ? null : Role.STATIC;
                }

                // Any other solid fill => treat as dynamic
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
}
