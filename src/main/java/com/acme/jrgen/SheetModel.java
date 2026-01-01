package com.acme.jrgen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.model.CellItem;

/**
 * Simple per-sheet model:
 * - sheetName: Excel sheet name
 * - items:     all non-ignored cells (static / dynamic)
 * - band:      optional magenta-defined detail band spec (may be null)
 */
public record SheetModel(
    String sheetName,
    List<CellItem> items,
    BandSpec band
) {
    /**
     * Derived field map: fieldName -> FieldInfo (type/pattern/align/excelFormat).
     */
    public Map<String, FieldInfo> fields()
    {
        Map<String, FieldInfo> map = new LinkedHashMap<>();

        for (CellItem ci : this.items)
        {
            if (ci.isDynamic() && ci.fieldName() != null)
            {
                String name        = ci.fieldName();
                String type        = ci.javaType() != null ? ci.javaType() : "java.lang.String";
                String pattern     = ci.pattern();
                String align       = ci.hAlign();
                String excelFormat = ci.excelFormat();

                // keep first occurrence; don't overwrite later duplicates
                map.putIfAbsent(name, new FieldInfo(name, type, pattern, align, excelFormat));
            }
        }

        return map;
    }
}
