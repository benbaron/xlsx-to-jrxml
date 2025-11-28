package com.acme.jrgen;

/**
 * One logical cell to be rendered into JRXML.
 */
public record CellItem(
    int col,
    int row,
    double x,
    double y,
    double width,
    double height,
    String value,
    Role role,
    FieldSpec fieldSpec,
    double fontSize,
    String alignment,
    boolean render
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

    public String fieldName()
    {
        return fieldSpec == null ? null : fieldSpec.name();
    }
}
