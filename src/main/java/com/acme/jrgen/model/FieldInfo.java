package com.acme.jrgen.model;

/**
 * Describes a dynamic Jasper field inferred from the Excel sheet.
 *
 * This is used both for:
 *   - bean generation (field name & Java type)
 *   - fieldmap CSV generation (Java type & Excel format)
 */
public final class FieldInfo
{
    private final String fieldName;
    private final String javaType;      // e.g. "java.math.BigDecimal"
    private final String excelFormat;   // raw Excel data format, may be null

    /**
     * Primary constructor.
     *
     * @param fieldName   logical JR/bean field name (e.g. "cashamount")
     * @param javaType    fully-qualified Java type (e.g. "java.math.BigDecimal")
     * @param excelFormat Excel data format string or null
     */
    public FieldInfo(String fieldName, String javaType, String excelFormat)
    {
        this.fieldName = fieldName;
        this.javaType = javaType;
        this.excelFormat = excelFormat;
    }

    /**
     * Backwards-compatible convenience constructor if you don't yet
     * track Excel formats.
     */
    public FieldInfo(String fieldName, String javaType)
    {
        this(fieldName, javaType, null);
    }

    public String fieldName()
    {
        return fieldName;
    }

    public String javaType()
    {
        return javaType;
    }

    /**
     * The original Excel data format for this field, if any.
     *
     * May be null if not known.
     */
    public String excelFormat()
    {
        return excelFormat;
    }
}
