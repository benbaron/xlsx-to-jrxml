package com.acme.jrgen;

/**
 * Describes a detail band detected from magenta rectangles.
 */
public record BandSpec(
    String name,
    int order,
    double x,
    double y,
    double width,
    double height,
    String splitType,
    String printWhenExpression,
    int topRow,
    int bottomRow,
    int leftCol,
    int rightCol
)
{
    public boolean containsCell(int row, int col)
    {
        return row >= topRow && row <= bottomRow && col >= leftCol && col <= rightCol;
    }
}
