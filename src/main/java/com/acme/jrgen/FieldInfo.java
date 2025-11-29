package com.acme.jrgen;

/**
 * Simple descriptor for a dynamic field: name, Java type, pattern, alignment, and Excel format.
 */
public record FieldInfo(
    String name,
    String type,
    String pattern,
    String align,
    String excelFormat
)
{
    /**
     * Convenience alias in case any code expects info.javaType().
     */
    public String javaType()
    {
        return type;
    }
}
