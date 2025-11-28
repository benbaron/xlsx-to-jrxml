package com.acme.jrgen;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

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
        names = "--pageWidth",
        defaultValue = "612",
        description = "JR pageWidth (default 612 - Letter portrait)"
    )
    int pageWidth;

    @Option(
        names = "--pageHeight",
        defaultValue = "792",
        description = "JR pageHeight (default 792 - Letter portrait)"
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
        names = "--beanPackage",
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
        names = "--beans",
        defaultValue = "false",
        description = "Generate Java beans and field mapping CSV"
    )
    boolean beans;

    @Override
    public Integer call() throws Exception
    {
        Files.createDirectories(outDir);

        ExcelScanner scanner = new ExcelScanner(
            excelPath,
            pageWidth,
            pageHeight,
            margins.get(0),
            margins.get(1),
            margins.get(2),
            margins.get(3)
        );
        var models = scanner.scan(sheets);

        List<String> mappingRows = new ArrayList<>();
        mappingRows.add("sheet,cell,field_name,java_type,excel_number_format");

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

            if (beans)
            {
                String beanSrc = BeanGenerator.generateBean(
                    model,
                    beanPackage,
                    beanSuffix
                );
                Path javaOut = outDir.resolve(baseName + beanSuffix + ".java");
                Files.writeString(javaOut, beanSrc);

                mappingRows.addAll(FieldMappingGenerator.generateMappings(model));
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

            System.out.println("Wrote: " + jrxmlOut.getFileName());
        }

        if (beans)
        {
            Path metaDir = outDir.resolve("meta");
            Files.createDirectories(metaDir);
            Path mappingCsv = metaDir.resolve("field_mapping.csv");
            Files.write(mappingCsv, mappingRows);
            System.out.println("Wrote: " + mappingCsv.getFileName());
        }

        return 0;
    }

    public static void main(String[] args)
    {
        int code = new CommandLine(new Main()).execute(args);
        System.exit(code);
    }
}
