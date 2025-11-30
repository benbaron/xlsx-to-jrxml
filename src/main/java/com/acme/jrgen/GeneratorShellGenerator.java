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

        sb.append("package ").append(genPkg).append(";\n\n");
        sb.append("import nonprofitbookkeeping.exception.ActionCancelledException;\n");
        sb.append("import nonprofitbookkeeping.exception.NoFileCreatedException;\n");
        sb.append("import nonprofitbookkeeping.reports.jasper.AbstractReportGenerator;\n");
        sb.append("import java.util.Collections;\n");
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
        sb.append("    @Override\n");
        sb.append("    protected List<").append(beanSimple).append("> getReportData()\n");
        sb.append("    {\n");
        sb.append("        // TODO supply data beans for the report\n");
        sb.append("        return Collections.emptyList();\n");
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
}
