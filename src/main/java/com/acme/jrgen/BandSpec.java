package com.acme.jrgen;

/**
 * Optional detail-band specification derived from magenta rectangles and [[band ...]] tags.
 *
 * Rows are zero-based Excel row indices (inclusive for topRow and bottomRow).
 */
public record BandSpec(
    int topRow,
    int bottomRow,
    Integer heightOverride,      // explicit band height in JR units (optional)
    String splitType,            // Prevent | Stretch | Immediate (optional)
    String printWhenExpression,  // JR expression string (optional)
    String name                  // logical band name (optional)
) {}
