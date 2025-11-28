package com.acme.jrgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes field mapping CSV for generated beans.
 */
public final class FieldMappingGenerator
{
    private FieldMappingGenerator()
    {
    }

    public static List<String> generateMappings(SheetModel model)
    {
        List<String> rows = new ArrayList<>();
        for (CellItem ci : model.items())
        {
            if (!ci.isDynamic() || ci.fieldName() == null)
            {
                continue;
            }
            String cellRef = toCellRef(ci.row(), ci.col());
            FieldInfo info = model.fields().get(ci.fieldName());
            rows.add(String.join(",",
                escape(model.sheetName()),
                escape(cellRef),
                escape(ci.fieldName()),
                escape(info.javaType()),
                escape(info.excelFormat() == null ? "" : info.excelFormat())
            ));
        }
        return rows;
    }

    private static String toCellRef(int row, int col)
    {
        StringBuilder colRef = new StringBuilder();
        int c = col;
        do
        {
            int rem = c % 26;
            colRef.insert(0, (char) ('A' + rem));
            c = (c / 26) - 1;
        } while (c >= 0);
        return colRef + Integer.toString(row + 1);
    }

    private static String escape(String s)
    {
        if (s == null)
        {
            return "";
        }
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
