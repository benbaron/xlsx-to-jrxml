package com.acme.jrgen;

import java.util.List;

/**
 * Sheet model: width/height plus ordered bands containing cell items.
 */
public record SheetModel(String sheetName,
                         double totalWidth,
                         double totalHeight,
                         List<Band> bands)
{
}
