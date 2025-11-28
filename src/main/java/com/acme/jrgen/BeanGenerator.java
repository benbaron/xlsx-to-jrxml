package com.acme.jrgen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Java Beans for dynamic fields.
 */
public class BeanGenerator
{
    public static String generateBean(SheetModel model,
                                      String beanPackage,
                                      String beanSuffix)
    {
        Map<String, FieldInfo> fields = new LinkedHashMap<>(model.fields());

        String pkg = (beanPackage == null || beanPackage.isBlank())
            ? "com.acme.jrgen.beans"
            : beanPackage;

        String suffix = (beanSuffix == null || beanSuffix.isBlank())
            ? "Bean"
            : beanSuffix;

        String baseName = model.sheetName();
        String cls = baseName + suffix;

        StringBuilder sb = new StringBuilder(32_000);

        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("/** Generated bean for sheet ").append(model.sheetName()).append(" */\n");
        sb.append("public class ").append(cls).append("\n");
        sb.append("{\n\n");

        for (FieldInfo f : fields.values())
        {
            sb.append("    private ").append(simpleType(f.javaType())).append(' ')
                .append(f.name()).append(";\n");
        }
        sb.append('\n');

        for (FieldInfo f : fields.values())
        {
            String cap = capitalize(f.name());
            String type = simpleType(f.javaType());
            sb.append("    public ").append(type).append(" get").append(cap).append("()\n");
            sb.append("    {\n");
            sb.append("        return ").append(f.name()).append(";\n");
            sb.append("    }\n\n");

            sb.append("    public void set").append(cap).append('(')
                .append(type).append(' ').append(f.name()).append(")\n");
            sb.append("    {\n");
            sb.append("        this.").append(f.name()).append(" = ")
                .append(f.name()).append(";\n");
            sb.append("    }\n\n");
        }

        sb.append("    public ").append(cls).append("()\n");
        sb.append("    {\n");
        sb.append("    }\n\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static String simpleType(String fqcn)
    {
        if (fqcn == null || fqcn.isBlank())
        {
            return "String";
        }
        int idx = fqcn.lastIndexOf('.');
        return (idx >= 0) ? fqcn.substring(idx + 1) : fqcn;
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
