package com.acme.jrgen;

/**
 * Metadata about a generated Jasper field.
 */
public record FieldInfo(
    String name,
    String javaType,
    String pattern,
    String alignment,
    String excelFormat
)
{
}
