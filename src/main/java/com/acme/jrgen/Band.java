package com.acme.jrgen;

import java.util.List;

import com.acme.jrgen.model.CellItem;

/**
 * Represents a detail band derived from a magenta rectangle or the full sheet.
 */
public record Band(String name,
                   Integer idx,
                   int startRow,
                   int endRow,
                   int startCol,
                   int endCol,
                   double originX,
                   double originY,
                   double width,
                   double height,
                   Double explicitHeight,
                   String splitType,
                   String printWhenExpression,
                   List<CellItem> items)
{
    public double effectiveHeight(double scaleY)
    {
        if (this.explicitHeight != null)
        {
            return this.explicitHeight;
        }
        return this.height * scaleY;
    }
}
