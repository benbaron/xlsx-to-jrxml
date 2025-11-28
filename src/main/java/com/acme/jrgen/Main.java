package com.acme.jrgen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import com.acme.jrgen.CellItem;
import com.acme.jrgen.Band;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI entry point for Excel -> JRXML + Bean generation.
 */
@Command(
    name = "jrxml-excel-generator",
    mixinStandardHelpOptions = true,
    description = "Generate JRXML + Java Beans from Excel."
)
public class Main implements Callable<Integer>
{
    @Option(
        names = "--excel",
        required = true,
        description = "Path to .xlsx/.xlsm"
    )
    Path excelPath;

    @Option(
        names = "--sheets",
        split = ",",
        description = "Comma-separated sheet names (default: all sheets)"
    )
    List<String> sheets;

    @Option(
        names = "--out",
        required = true,
        description = "Output directory"
    )
    Path outDir;

    @Option(
        names = "--beans",
        defaultValue = "false",
        description = "Generate Java beans and field mapping CSV"
    )
    boolean beans;

    @Option(
        names = "--pageWidth",
        defaultValue = "612",
        description = "JR pageWidth (default Letter 612)"
    )
    int pageWidth;

    @Option(
        names = "--pageHeight",
        defaultValue = "792",
        description = "JR pageHeight (default Letter 792)"
    )
    int pageHeight;

    @Option(
        names = "--margins",
        split = ",",
        defaultValue = "20,20,20,20",
        description = "left,right,top,bottom margins (default 20,20,20,20)"
    )
    List<Integer> margins;

    @Option(
        names = "--xsd",
        description = "Optional path to JRXML XSD for validation"
    )
    Path xsdPath;

    @Option(
        names = "--skipValidation",
        defaultValue = "false",
        description = "Skip XSD validation"
    )
    boolean skipValidation;

    @Option(
        names = {"--beans-package", "--beanPackage"},
        defaultValue = "com.acme.jrgen.beans",
        description = "Package name for generated beans (default: ${DEFAULT-VALUE})"
    )
    String beanPackage;

    @Option(
        names = "--beanSuffix",
        defaultValue = "Bean",
        description = "Suffix appended to bean simple class name (default: ${DEFAULT-VALUE})"
    )
    String beanSuffix;

    @Option(
        names = "--generator-package",
        defaultValue = "com.acme.jrgen.generator",
        description = "Package name for generated Jasper generator classes (default: ${DEFAULT-VALUE})"
    )
    String generatorPackage;

    @Option(
        names = "--generatorSuffix",
        defaultValue = "JasperGenerator",
        description = "Suffix appended to generated Jasper generator class names (default: ${DEFAULT-VALUE})"
    )
    String generatorSuffix;

    @Option(
        names = "--reportTypeSuffix",
        defaultValue = "_JASPER",
        description = "Suffix appended to generated report type enums (default: ${DEFAULT-VALUE})"
    )
    String reportTypeSuffix;

    @Override
    public Integer call() throws Exception
    {
        Files.createDirectories(outDir);

        ExcelScanner scanner = new ExcelScanner(excelPath);
        var models = scanner.scan(sheets);

        StringBuilder mappingCsv = new StringBuilder();
        if (beans)
        {
            mappingCsv.append("sheet,cell,field_name,java_type,excel_number_format\n");
        }

        for (SheetModel model : models)
        {
            // Generate JRXML
            String jrxml = JRXMLBuilder.buildJRXML(
                model,
                pageWidth,
                pageHeight,
                margins.get(0),
                margins.get(1),
                margins.get(2),
                margins.get(3)
            );

            String baseName = model.sheetName();

            Path jrxmlOut = outDir.resolve(baseName + ".jrxml");
            Files.writeString(jrxmlOut, jrxml);

            Path javaOut = null;
            Path metaOut = null;

            if (beans)
            {
                // Generate Java Bean source
                String beanSrc = BeanGenerator.generateBean(
                    model,
                    beanPackage,
                    beanSuffix
                );
                javaOut = outDir.resolve(baseName + beanSuffix + ".java");
                Files.writeString(javaOut, beanSrc);

                // Metadata .properties file
                String metaText = MetadataGenerator.generateMetadata(
                    model,
                    beanPackage,
                    beanSuffix,
                    generatorPackage,
                    generatorSuffix,
                    reportTypeSuffix
                );
                metaOut = outDir.resolve(baseName + ".properties");
                Files.writeString(metaOut, metaText);

                appendMapping(model, mappingCsv);
            }

            // Optional validation
            if (!skipValidation && xsdPath != null)
            {
                var errs = XsdValidator.validate(jrxmlOut, xsdPath);
                if (!errs.isEmpty())
                {
                    System.err.println("XSD validation errors for " + jrxmlOut.getFileName() + ":");
                    for (String e : errs)
                    {
                        System.err.println("  - " + e);
                    }
                }
                else
                {
                    System.out.println("Validated OK: " + jrxmlOut.getFileName());
                }
            }

            if (beans)
            {
                System.out.println(
                    "Wrote: " + jrxmlOut.getFileName()
                    + ", " + javaOut.getFileName()
                    + ", " + metaOut.getFileName()
                );
            }
            else
            {
                System.out.println("Wrote: " + jrxmlOut.getFileName());
            }
        }

        if (beans)
        {
            Path metaDir = outDir.resolve("meta");
            Files.createDirectories(metaDir);
            Path mappingOut = metaDir.resolve("field_mapping.csv");
            Files.writeString(mappingOut, mappingCsv.toString());
        }

        return 0;
    }

    private void appendMapping(SheetModel model, StringBuilder mappingCsv)
    {
        for (Band band : model.bands())
        {
            for (CellItem ci : band.items())
            {
                if (ci.fieldSpec() == null)
                {
                    continue;
                }

                mappingCsv.append(model.sheetName()).append(',')
                    .append(toCellRef(ci.row(), ci.col())).append(',')
                    .append(ci.fieldName()).append(',')
                    .append(ci.fieldSpec().type()).append(',')
                    .append(ci.fieldSpec().originalFormat() == null ? "" : ci.fieldSpec().originalFormat())
                    .append('\n');
            }
        }
    }

    private static String toCellRef(int row, int col)
    {
        StringBuilder sb = new StringBuilder();
        int c = col;
        do
        {
            int rem = c % 26;
            sb.insert(0, (char) ('A' + rem));
            c = (c / 26) - 1;
        }
        while (c >= 0);

        sb.append(row + 1);
        return sb.toString();
    }

    public static void main(String[] args)
    {
        int code = new CommandLine(new Main()).execute(args);
        System.exit(code);
    }
}
