package com.acme.jrgen;

import java.util.List;

/**
 * Simple per-sheet model: sheet name + list of cell items.
 */
public record SheetModel(String sheetName, List<CellItem> items)
{
}
