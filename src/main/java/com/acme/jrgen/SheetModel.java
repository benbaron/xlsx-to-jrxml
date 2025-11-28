package com.acme.jrgen;

import java.util.List;
import java.util.Map;

/**
 * Sheet model: width/height plus ordered bands containing cell items.
 */
public record SheetModel(String sheetName,
                         double totalWidth,
                         double totalHeight,
                         List<Band> bands)
{
}
