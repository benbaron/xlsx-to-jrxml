package com.acme.jrgen;

import java.util.Locale;

/**
 * Generates a .properties-style metadata file alongside each JRXML:
 *
 * # Generated metadata for Jasper report bundle
 * displayName=Account Summary
 * generatorClass=nonprofitbookkeeping.reports.jasper.AccountSummaryJasperGenerator
 * reportType=ACCOUNT_SUMMARY_JASPER
 * template=AccountSummary.jrxml
 * beanClass=nonprofitbookkeeping.reports.datasource.AccountSummaryRowBean
 * description=Summary balances by account. Data bean: AccountSummaryRowBean.
 */
public final class MetadataGenerator
{
    private MetadataGenerator()
    {
    }

    public static String generateMetadata(SheetModel model,
                                          String beanPackage,
                                          String beanSuffix,
                                          String generatorPackage,
                                          String generatorSuffix,
                                          String reportTypeSuffix)
    {
        String baseName = model.sheetName();

        String displayName = toDisplayName(baseName);

        String beanPkg = (beanPackage == null || beanPackage.isBlank())
            ? "com.acme.jrgen.beans"
            : beanPackage;

        String beanSfx = (beanSuffix == null || beanSuffix.isBlank())
            ? "Bean"
            : beanSuffix;

        String beanSimple = baseName + beanSfx;
        String beanFqcn = beanPkg + "." + beanSimple;

        String genPkg = (generatorPackage == null || generatorPackage.isBlank())
            ? "com.acme.jrgen.generator"
            : generatorPackage;

        String genSfx = (generatorSuffix == null || generatorSuffix.isBlank())
            ? "JasperGenerator"
            : generatorSuffix;

        String genSimple = baseName + genSfx;
        String genFqcn = genPkg + "." + genSimple;

        String rtSfx = (reportTypeSuffix == null || reportTypeSuffix.isBlank())
            ? "_JASPER"
            : reportTypeSuffix;

        String reportType = toEnumName(baseName) + rtSfx;

        String template = baseName + ".jrxml";

        String description = displayName + ". Data bean: " + beanSimple + ".";

        StringBuilder sb = new StringBuilder();
        sb.append("# Generated metadata for Jasper report bundle").append('\n');
        sb.append("displayName=").append(displayName).append('\n');
        sb.append("generatorClass=").append(genFqcn).append('\n');
        sb.append("reportType=").append(reportType).append('\n');
        sb.append("template=").append(template).append('\n');
        sb.append("beanClass=").append(beanFqcn).append('\n');
        sb.append("description=").append(description).append('\n');

        return sb.toString();
    }

    private static String toDisplayName(String baseName)
    {
        if (baseName == null || baseName.isBlank())
        {
            return "";
        }

        String s = baseName;

        // Insert space between camel-case boundaries: AccountSummary -> Account Summary
        s = s.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " $0");
        // Treat underscores and other non-alnums as spaces
        s = s.replace('_', ' ');
        s = s.replaceAll("[^A-Za-z0-9]+", " ").trim();

        if (s.isEmpty())
        {
            return baseName;
        }

        String[] parts = s.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++)
        {
            String p = parts[i];
            if (p.isEmpty())
            {
                continue;
            }
            if (i > 0)
            {
                out.append(' ');
            }
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1)
            {
                out.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }

    private static String toEnumName(String baseName)
    {
        if (baseName == null || baseName.isBlank())
        {
            return "";
        }

        // Insert underscore at camel-case boundaries: AccountSummary -> Account_Summary
        String s = baseName.replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_$0");
        // Replace non-alnums with underscores
        s = s.replaceAll("[^A-Za-z0-9]+", "_");
        s = s.replaceAll("_+", "_");
        s = s.replaceAll("^_+|_+$", "");
        return s.toUpperCase(Locale.ROOT);
    }
}
