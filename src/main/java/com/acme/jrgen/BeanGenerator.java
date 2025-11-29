package com.acme.jrgen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates a Java Bean source for a sheet, using dynamic fields.
 * Field types are derived from CellItem.javaType().
 */
public class BeanGenerator
{
    /**
     * Generate a bean source file for the given sheet.
     *
     * @param model        sheet model
     * @param beanPackage  package name for the bean (e.g. nonprofitbookkeeping.reports.datasource)
     * @param beanSuffix   suffix for the simple class name (e.g. "Bean" or "RowBean")
     * @return Java source text
     */
    public static String generateBean(SheetModel model,
                                      String beanPackage,
                                      String beanSuffix)
    {
        String pkg = (beanPackage == null || beanPackage.isBlank())
            ? "com.acme.jrgen.beans"
            : beanPackage;

        String suffix = (beanSuffix == null || beanSuffix.isBlank())
            ? "Bean"
            : beanSuffix;

        String baseName = model.sheetName();
        String cls = baseName + suffix;

        // fieldName -> javaType
        Map<String, String> fieldTypes = new LinkedHashMap<>();

        for (CellItem ci : model.items())
        {
            if (ci.isDynamic() && ci.fieldName() != null)
            {
                String name = ci.fieldName();
                String type = ci.javaType();
                if (type == null || type.isBlank())
                {
                    type = "java.lang.String";
                }

                fieldTypes.putIfAbsent(name, type);
            }
        }

        StringBuilder sb = new StringBuilder(32_000);

        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("/** Generated bean for sheet ").append(model.sheetName()).append(" */\n");
        sb.append("public class ").append(cls).append("\n");
        sb.append("{\n\n");

        // fields
        for (Map.Entry<String, String> e : fieldTypes.entrySet())
        {
            String name = e.getKey();
            String type = e.getValue();

            sb.append("    private ").append(type).append(" ").append(name).append(";\n");
        }

        sb.append("\n");

        // getters/setters
        for (Map.Entry<String, String> e : fieldTypes.entrySet())
        {
            String name = e.getKey();
            String type = e.getValue();
            String cap = capitalize(name);

            sb.append("    public ").append(type).append(" get").append(cap).append("()\n");
            sb.append("    {\n");
            sb.append("        return ").append(name).append(";\n");
            sb.append("    }\n\n");

            sb.append("    public void set").append(cap).append("(").append(type).append(" v)\n");
            sb.append("    {\n");
            sb.append("        this.").append(name).append(" = v;\n");
            sb.append("    }\n\n");
        }

        // default ctor
        sb.append("    public ").append(cls).append("()\n");
        sb.append("    {\n");
        sb.append("    }\n\n");

        sb.append("}\n");

        return sb.toString();
    }

    private static String capitalize(String s)
    {
        if (s == null || s.isEmpty())
        {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
