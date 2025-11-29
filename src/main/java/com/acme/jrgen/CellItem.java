package com.acme.jrgen;

/**
 * One logical cell to be rendered into JRXML.
 *
 * Coordinates (x, y, width, height) are already scaled to JR units
 * based on the cellWidth / cellHeight mapping in ExcelScanner.
 */
public record CellItem(
    int col,
    int row,
    int x,
    int y,
    int width,
    int height,
    String value,
    Role role,
    String fieldName,   // null for static cells
    String javaType,    // e.g., "java.lang.String", "java.lang.Double"
    String pattern,     // optional JR pattern from [[field pattern=...]]
    String hAlign,      // optional alignment from [[field align=...]]
    String excelFormat  // raw Excel data format string (CellStyle.getDataFormatString())
)
{
    public enum Role
    {
        STATIC,
        DYNAMIC
    }

    public boolean isStatic()
    {
        return role == Role.STATIC;
    }

    public boolean isDynamic()
    {
        return role == Role.DYNAMIC;
    }
}
