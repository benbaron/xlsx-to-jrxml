package com.acme.jrgen;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI entry point for Excel -> JRXML + Bean + metadata generation.
 */
@Command(
    name = "jrxml-excel-generator",
    mixinStandardHelpOptions = true,
    description = "Generate JRXML + Java Beans + metadata from Excel."
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
        defaultValue = "src/main/resources/nonprofitbookkeeping/reports/jrxml",
        description = "Output directory (default: ${DEFAULT-VALUE})"
    )
    Path outDir;

    @Option(
        names = "--pageWidth",
        defaultValue = "595",
        description = "JR pageWidth (default 595)"
    )
    int pageWidth;

    @Option(
        names = "--pageHeight",
        defaultValue = "842",
        description = "JR pageHeight (default 842)"
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
        names = "--cellSize",
        split = ",",
        defaultValue = "80,20",
        description = "cellWidth,cellHeight pixel mapping (default 80,20)"
    )
    List<Integer> cellSize;

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

    /**
     * Package for generated bean classes and for the metadata beanClass.
     * Example: nonprofitbookkeeping.reports.datasource
     */
    @Option(
        names = "--beanPackage",
        defaultValue = "nonprofitbookkeeping.reports.beans",
        description = "Package name for generated beans and metadata beanClass (default: ${DEFAULT-VALUE})"
    )
    String beanPackage;

    /**
     * Suffix for generated bean simple class names.
     * Example: RowBean -> AccountSummaryRowBean
     */
    @Option(
        names = "--beanSuffix",
        defaultValue = "Bean",
        description = "Suffix appended to bean simple class name (default: ${DEFAULT-VALUE})"
    )
    String beanSuffix;

    /**
     * Base package for generatorClass in metadata.
     * Example: nonprofitbookkeeping.reports.jasper
     */
    @Option(
        names = "--generatorPackage",
        defaultValue = "nonprofitbookkeeping.reports.datasource",
        description = "Base package for generatorClass in metadata (default: ${DEFAULT-VALUE})"
    )
    String generatorPackage;

    /**
     * Suffix for generator simple class names in metadata.
     * Example: JasperGenerator -> AccountSummaryJasperGenerator
     */
    @Option(
        names = "--generatorSuffix",
        defaultValue = "JasperGenerator",
        description = "Suffix appended to generator class simple name in metadata (default: ${DEFAULT-VALUE})"
    )
    String generatorSuffix;

    /**
     * Suffix appended to the reportType constant in metadata.
     * Example: _JASPER -> ACCOUNT_SUMMARY_JASPER
     */
    @Option(
        names = "--reportTypeSuffix",
        defaultValue = "_JASPER",
        description = "Suffix appended to reportType constant in metadata (default: ${DEFAULT-VALUE})"
    )
    String reportTypeSuffix;

    @Option(
        names = "--inlineOverrides",
        defaultValue = "true",
        description = "Generate SQL override placeholders inline in generator skeletons (default: ${DEFAULT-VALUE})"
    )
    boolean inlineOverrides;

    @Override
    public Integer call() throws Exception
    {
        Files.createDirectories(this.outDir);

        Path jrxmlDir = this.outDir.resolve("jrxml");
        Path beanDir = this.outDir.resolve("beans");
        Path generatorDir = this.outDir.resolve("generators");
        Path fieldmapDir = jrxmlDir; // colocate with JRXML + metadata for now

        Files.createDirectories(jrxmlDir);
        Files.createDirectories(beanDir);
        Files.createDirectories(generatorDir);

        String resolvedBeanSuffix =
            (this.beanSuffix == null || this.beanSuffix.isBlank())
                ? "Bean"
                : this.beanSuffix;
        String resolvedGeneratorSuffix =
            (this.generatorSuffix == null || this.generatorSuffix.isBlank())
                ? "JasperGenerator"
                : this.generatorSuffix;

        int cellW = this.cellSize.get(0);
        int cellH = this.cellSize.get(1);

        ExcelScanner scanner = new ExcelScanner(this.excelPath, cellW, cellH);
        var models = scanner.scan(this.sheets);

        for (SheetModel model : models)
        {
            // Generate JRXML
            String jrxml = JRXMLBuilder.buildJRXML(
                model,
                this.pageWidth,
                this.pageHeight,
                this.margins.get(0),
                this.margins.get(1),
                this.margins.get(2),
                this.margins.get(3)
            );

            String baseName = model.sheetName();

            Path jrxmlOut = jrxmlDir.resolve(baseName + ".jrxml");
            Files.writeString(jrxmlOut, jrxml);

            // Generate Java Bean source
            String beanSrc = BeanGenerator.generateBean(
                model,
                this.beanPackage,
                resolvedBeanSuffix
            );
            Path javaOut = beanDir.resolve(baseName + resolvedBeanSuffix + ".java");
            Files.writeString(javaOut, beanSrc);

            // Generate generator shell
            String generatorSrc = GeneratorShellGenerator.generateGenerator(
                model,
                this.beanPackage,
                resolvedBeanSuffix,
                this.generatorPackage,
                resolvedGeneratorSuffix,
                this.inlineOverrides
            );
            Path generatorOut =
                generatorDir.resolve(baseName + resolvedGeneratorSuffix + ".java");
            Files.writeString(generatorOut, generatorSrc);

            // Optional validation
            if (!this.skipValidation && this.xsdPath != null)
            {
                var errs = XsdValidator.validate(jrxmlOut, this.xsdPath);
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

            // Metadata .properties file
            String metaText = MetadataGenerator.generateMetadata(
                model,
                this.beanPackage,
                resolvedBeanSuffix,
                this.generatorPackage,
                resolvedGeneratorSuffix,
                this.reportTypeSuffix
            );
            Path metaOut = jrxmlDir.resolve(baseName + ".properties");
            Files.writeString(metaOut, metaText);

            // Field mapping CSV
            Path fieldmapOut = FieldMappingGenerator.writeCsv(
                model,
                fieldmapDir,
                baseName
            );

            System.out.println(
                "Wrote: " + jrxmlOut.getFileName()
                + " (" + jrxmlDir.getFileName() + ")"
                + ", " + javaOut.getFileName()
                + " (" + beanDir.getFileName() + ")"
                + ", " + generatorOut.getFileName()
                + " (" + generatorDir.getFileName() + ")"
                + ", " + metaOut.getFileName()
                + " (" + jrxmlDir.getFileName() + ")"
                + ", " + fieldmapOut.getFileName()
                + " (" + fieldmapDir.getFileName() + ")"
            );
        }

        return 0;
    }

    public static void main(String[] args)
    {
        CommandLine commandLine = new CommandLine(new Main());
        commandLine.setColorScheme(
            CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.OFF)
        );
        int code = commandLine.execute(args);
        System.exit(code);
    }
}
