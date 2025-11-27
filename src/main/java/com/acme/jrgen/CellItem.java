package com.acme.jrgen;

/**
 * One logical cell to be rendered into JRXML.
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
    String fieldName
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
