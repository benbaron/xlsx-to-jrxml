package com.acme.jrgen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.FieldInfo;
import com.acme.jrgen.model.CellItem;

/**
 * Generates the *_fieldmap.csv file for a SheetModel.
 *
 * CSV format (matches the runtime FieldMapLoader):
 *
 *   sheetName,cellRef,fieldName,javaType,excelFormat
 *
 * Example row:
 *
 *   "AccountSummary","B5","cashamount","java.math.BigDecimal",""
 *
 * Notes:
 *  - There is always a header row (first line).
 *  - excelFormat may be empty ("") if you don't have a specific Excel format.
 *  - dbExpr is intentionally NOT generated here; the 6th column is for
 *    hand-written SQL expressions on the runtime side.
 */
public final class FieldMappingGenerator
{
    private FieldMappingGenerator()
    {
    }

    /**
     * Build CSV lines (including header) for the given sheet model.
     */
    public static List<String> generateMappings(SheetModel model)
    {
        List<String> rows = new ArrayList<>();

        // Header; the runtime FieldMapLoader reads and discards this line.
        rows.add("sheetName,cellRef,fieldName,javaType,excelFormat");

        Map<String, FieldInfo> fields = model.fields();

        for (CellItem ci : model.items())
        {
            if (!ci.isDynamic())
            {
                continue;
            }

            String fieldName = ci.fieldName();
            if (fieldName == null || fieldName.isBlank())
            {
                continue;
            }

            String cellRef = toCellRef(ci.row(), ci.col());

            FieldInfo info = (fields != null) ? fields.get(fieldName) : null;

            String javaType =
                (info != null && info.javaType() != null && !info.javaType().isBlank())
                    ? info.javaType()
                    : "java.lang.String";

            String excelFormat =
                (info != null && info.excelFormat() != null)
                    ? info.excelFormat()
                    : "";

            rows.add(String.join(",",
                escape(model.sheetName()),
                escape(cellRef),
                escape(fieldName),
                escape(javaType),
                escape(excelFormat)
            ));
        }

        return rows;
    }

    /**
     * Convenience: write "<baseName>_fieldmap.csv" into outDir and return the path.
     *
     * If baseName is null or blank, the sheetName is used instead.
     */
    public static Path writeCsv(SheetModel model, Path outDir, String baseName)
        throws IOException
    {
        List<String> lines = generateMappings(model);

        if (baseName == null || baseName.isBlank())
        {
            baseName = model.sheetName();
        }

        Files.createDirectories(outDir);
        Path csv = outDir.resolve(baseName + "_fieldmap.csv");
        Files.write(csv, lines, StandardCharsets.UTF_8);
        return csv;
    }

    // --- helpers ---

    /**
     * Convert 0-based (row, col) to Excel A1 cell reference.
     *
     * col: 0 -> A, 1 -> B ... 25 -> Z, 26 -> AA, etc.
     * row: 0 -> 1, 1 -> 2, etc.
     */
    private static String toCellRef(int row, int col)
    {
        StringBuilder colRef = new StringBuilder();
        int c = col;
        do
        {
            int rem = c % 26;
            colRef.insert(0, (char) ('A' + rem));
            c = (c / 26) - 1;
        }
        while (c >= 0);
        return colRef.toString() + (row + 1);
    }

    /**
     * Simple CSV escaping: wrap in quotes and double any internal quotes.
     */
    private static String escape(String s)
    {
        if (s == null)
        {
            return "";
        }
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
