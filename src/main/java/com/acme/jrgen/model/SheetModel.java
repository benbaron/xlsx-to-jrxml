package com.acme.jrgen.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.CellItem;

/**
 * Immutable model of a single Excel sheet as understood by the generator.
 *
 * It contains:
 *   - sheetName: the Excel sheet name
 *   - items:     all cell items (static + dynamic)
 *   - fields:    map from fieldName -> FieldInfo (for dynamic fields)
 */
public final class SheetModel
{
    private final String sheetName;
    private final List<CellItem> items;
    private final Map<String, FieldInfo> fields;

    /**
     * Main constructor used by your SheetModelBuilder.
     */
    public SheetModel(String sheetName,
                      List<CellItem> items,
                      Map<String, FieldInfo> fields)
    {
        this.sheetName = sheetName;
        this.items = Collections.unmodifiableList(items);
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    /**
     * Backwards-compatible constructor if you haven't wired the fields map yet.
     * The fields() map will simply be empty in that case.
     */
    public SheetModel(String sheetName, List<CellItem> items)
    {
        this.sheetName = sheetName;
        this.items = Collections.unmodifiableList(items);
        this.fields = Collections.emptyMap();
    }

    public String sheetName()
    {
        return sheetName;
    }

    public List<CellItem> items()
    {
        return items;
    }

    /**
     * Map from JR/bean field name -> FieldInfo.
     *
     * May be empty if your builder does not populate it yet.
     */
    public Map<String, FieldInfo> fields()
    {
        return fields;
    }
}
