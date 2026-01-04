package com.acme.jrgen;

/**
 * Creates a concrete {@code AbstractReportGenerator} shell for each generated
 * JRXML + bean pair. The generated classes extend the provided abstract base
 * and include TODO markers for supplying data, parameters, and JRXML location.
 */
public final class GeneratorShellGenerator
{
    private GeneratorShellGenerator()
    {
    }

    public static String generateGenerator(SheetModel model,
                                           String beanPackage,
                                           String beanSuffix,
                                           String generatorPackage,
                                           String generatorSuffix)
    {
        String beanPkg = (beanPackage == null || beanPackage.isBlank())
            ? "com.acme.jrgen.beans"
            : beanPackage;

        String beanSfx = (beanSuffix == null || beanSuffix.isBlank())
            ? "Bean"
            : beanSuffix;

        String genPkg = (generatorPackage == null || generatorPackage.isBlank())
            ? "com.acme.jrgen.generator"
            : generatorPackage;

        String genSfx = (generatorSuffix == null || generatorSuffix.isBlank())
            ? "JasperGenerator"
            : generatorSuffix;

        String baseName = model.sheetName();
        String beanSimple = baseName + beanSfx;
        String beanFqcn = beanPkg + "." + beanSimple;
        String genSimple = baseName + genSfx;

        StringBuilder sb = new StringBuilder();

        sb.append("/*\n");
        sb.append(" * AUTO-GENERATED FILE. DO NOT EDIT.\n");
        sb.append(" */\n");
        sb.append("package ").append(genPkg).append(";\n\n");
        sb.append("import java.io.IOException;\n");
        sb.append("import nonprofitbookkeeping.exception.ActionCancelledException;\n");
        sb.append("import nonprofitbookkeeping.exception.NoFileCreatedException;\n");
        sb.append("import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;\n");
        sb.append("import nonprofitbookkeeping.reports.query.FieldMapSqlBuilder;\n");
        sb.append("import nonprofitbookkeeping.reports.query.ReportDataFetcher;\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("import ").append(beanFqcn).append(";\n\n");
        sb.append("/** Skeleton generator for JRXML template ")
            .append(baseName)
            .append(".jrxml */\n");
        sb.append("public class ").append(genSimple)
            .append(" extends AbstractReportGenerator\n");
        sb.append("{\n");
        sb.append("    /**\n");
        sb.append("     * Override @see nonprofitbookkeeping.reports.jasper.AbstractReportGenerator#getReportData()\n");
        sb.append("     */\n");
        sb.append("    @Override\n");
        sb.append("    protected List<").append(beanSimple).append("> getReportData()\n");
        sb.append("    {\n");
        sb.append("        Map<String, String> overrides = new HashMap<>();\n");
        sb.append("        // TODO replace placeholder expressions with real column expressions\n");
        for (var entry : model.fields().entrySet())
        {
            String fieldName = entry.getKey();
            String javaType = entry.getValue().javaType();
            String placeholder = sqlPlaceholderFor(javaType);
            sb.append("        overrides.put(\"")
                .append(fieldName)
                .append("\", \"")
                .append(placeholder)
                .append("\");\n");
        }
        sb.append("\n");
        sb.append("        String selectList;\n");
        sb.append("        try\n");
        sb.append("        {\n");
        sb.append("            selectList = FieldMapSqlBuilder.buildSelectList(\n");
        sb.append("                \"/nonprofitbookkeeping/reports/")
            .append(baseName)
            .append("_fieldmap.csv\",\n");
        sb.append("                overrides\n");
        sb.append("            );\n");
        sb.append("        }\n");
        sb.append("        catch (IOException ex)\n");
        sb.append("        {\n");
        sb.append("            throw new IllegalStateException(\n");
        sb.append("                \"Unable to load ")
            .append(baseName)
            .append(" field map\", ex);\n");
        sb.append("        }\n\n");
        sb.append("        String sql = \"select\\n\" +\n");
        sb.append("            selectList + \"\\n\" +\n");
        sb.append("            \"from journal_transaction jt\\n\" +\n");
        sb.append("            \"join journal_entry je on je.txn_id = jt.id\";\n\n");
        sb.append("        return ReportDataFetcher.queryBeans(")
            .append(beanSimple)
            .append(".class, sql);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    protected Map<String, Object> getReportParameters()\n");
        sb.append("    {\n");
        sb.append("        Map<String, Object> params = new HashMap<>();\n");
        sb.append("        // TODO populate report parameters such as title or filters\n");
        sb.append("        return params;\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append(
            "    protected String getReportPath() throws ActionCancelledException, NoFileCreatedException\n");
        sb.append("    {\n");
        sb.append("        // TODO return the classpath or filesystem path to ")
            .append(baseName)
            .append(".jrxml\n");
        sb.append("        return bundledReportPath();\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public String getBaseName()\n");
        sb.append("    {\n");
        sb.append("        return \"").append(baseName).append("\";\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static String sqlPlaceholderFor(String javaType)
    {
        if (javaType == null || javaType.isBlank())
        {
            return "NULL";
        }

        return switch (javaType)
        {
            case "java.lang.String" -> "CAST(NULL AS VARCHAR(255))";
            case "java.lang.Double", "double" -> "CAST(NULL AS DOUBLE)";
            case "java.lang.Integer", "int" -> "CAST(NULL AS INTEGER)";
            case "java.lang.Long", "long" -> "CAST(NULL AS BIGINT)";
            case "java.lang.Boolean", "boolean" -> "CAST(NULL AS BOOLEAN)";
            case "java.math.BigDecimal" -> "CAST(NULL AS DECIMAL(18,2))";
            case "java.util.Date" -> "CAST(NULL AS DATE)";
            default -> "NULL";
        };
    }
}
