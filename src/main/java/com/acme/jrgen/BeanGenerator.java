package com.acme.jrgen;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates Java Beans for dynamic fields.
 */
public class BeanGenerator
{
    public static String generateBean(SheetModel model,
                                      String beanPackage,
                                      String beanSuffix)
    {
        var dyn = model.bands().stream()
            .flatMap(b -> b.items().stream())
            .filter(CellItem::isDynamic)
            .filter(ci -> ci.fieldSpec() != null && ci.fieldName() != null)
            .toList();

        String pkg = (beanPackage == null || beanPackage.isBlank())
            ? "com.acme.jrgen.beans"
            : beanPackage;

        String suffix = (beanSuffix == null || beanSuffix.isBlank())
            ? "Bean"
            : beanSuffix;

        String baseName = model.sheetName();
        String cls = baseName + suffix;

        var typeLookup = new LinkedHashMap<String, String>();
        dyn.forEach(ci -> typeLookup.putIfAbsent(ci.fieldName(), normalizeType(ci.fieldSpec().type())));

        Set<String> names = dyn.stream()
            .map(CellItem::fieldName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        StringBuilder sb = new StringBuilder(32_000);

        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("/** Generated bean for sheet ").append(model.sheetName()).append(" */\n");
        sb.append("public class ").append(cls).append("\n");
        sb.append("{\n\n");

        for (FieldInfo f : fields.values())
        {
            sb.append("    private ")
              .append(typeLookup.getOrDefault(n, "java.lang.String"))
              .append(" ")
              .append(n)
              .append(";\n");
        }
        sb.append('\n');

        for (FieldInfo f : fields.values())
        {
            String cap = capitalize(n);

            String type = typeLookup.getOrDefault(n, "java.lang.String");
            sb.append("    public ").append(type).append(" get").append(cap).append("()\n");
            sb.append("    {\n");
            sb.append("        return ").append(f.name()).append(";\n");
            sb.append("    }\n\n");

            sb.append("    public void set").append(cap).append("(").append(type).append(" v)\n");
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

    private static String normalizeType(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return "java.lang.String";
        }

        return switch (raw)
        {
            case "String", "java.lang.String" -> "java.lang.String";
            case "Double", "java.lang.Double" -> "java.lang.Double";
            case "Date", "java.util.Date" -> "java.util.Date";
            case "Integer", "java.lang.Integer" -> "java.lang.Integer";
            case "Long", "java.lang.Long" -> "java.lang.Long";
            case "BigDecimal", "java.math.BigDecimal" -> "java.math.BigDecimal";
            default -> raw;
        };
    }
}
