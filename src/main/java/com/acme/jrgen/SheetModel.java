package com.acme.jrgen;

import java.util.List;
import java.util.Map;

/**
 * Sheet-level model containing cells, bands, and field metadata.
 */
public record SheetModel(
    String sheetName,
    List<CellItem> items,
    List<BandSpec> bands,
    Map<String, FieldInfo> fields,
    double titleHeight
)
{
}
