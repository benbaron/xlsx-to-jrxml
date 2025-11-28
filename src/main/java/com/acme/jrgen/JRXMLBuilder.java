package com.acme.jrgen;

import java.util.Comparator;

/**
 * Build JRXML text from a SheetModel using the authoring rules.
 */
public class JRXMLBuilder
{
    public static String buildJRXML(SheetModel m,
                                    int pageWidth,
                                    int pageHeight,
                                    int leftMargin,
                                    int rightMargin,
                                    int topMargin,
                                    int bottomMargin)
    {
        StringBuilder sb = new StringBuilder(128_000);

        sb.append(
            FragmentLibrary.header(
                m.sheetName(),
                pageWidth,
                pageHeight,
                leftMargin,
                rightMargin,
                topMargin,
                bottomMargin
            )
        );

        sb.append(FragmentLibrary.styles());
        sb.append('\n');
        sb.append(FragmentLibrary.parameters());
        sb.append('\n');

        // Fields
        for (FieldInfo info : m.fields().values())
        {
            sb.append("      <field name=\"")
                .append(FragmentLibrary.escape(info.name()))
                .append("\" class=\"")
                .append(info.javaType())
                .append("\"/>")
                .append('\n');
        }

        sb.append('\n');

        // Title band
        sb.append("      <title>\n");
        sb.append("        <band height=\"").append((int) m.titleHeight()).append("\">\n");
        sb.append(FragmentLibrary.textField(
            0,
            0,
            pageWidth - leftMargin - rightMargin,
            20,
            "reportTitle",
            "Left",
            null,
            14,
            true
        ));
        sb.append(FragmentLibrary.textField(
            0,
            20,
            pageWidth - leftMargin - rightMargin,
            18,
            "organizationName",
            "Left",
            null,
            12,
            false
        ));
        sb.append(FragmentLibrary.staticText(
            0,
            38,
            pageWidth - leftMargin - rightMargin,
            18,
            m.sheetName(),
            11,
            false
        ));
        sb.append("        </band>\n");
        sb.append("      </title>\n\n");

        // Detail bands
        sb.append("      <detail>\n");
        var sortedBands = m.bands().stream()
            .sorted(Comparator.comparingInt(BandSpec::order))
            .toList();

        for (BandSpec band : sortedBands)
        {
            sb.append("        <band height=\"")
                .append((int) Math.round(band.height()))
                .append("\" splitType=\"")
                .append(band.splitType() == null ? "Stretch" : band.splitType())
                .append("\">");
            sb.append('\n');

            if (band.printWhenExpression() != null)
            {
                sb.append("          <printWhenExpression><![CDATA[")
                    .append(band.printWhenExpression())
                    .append("]]></printWhenExpression>\n");
            }

            for (CellItem ci : m.items())
            {
                if (ci.band() != band)
                {
                    continue;
                }

                if (ci.isStatic())
                {
                    sb.append(
                        FragmentLibrary.staticText(
                            (int) Math.round(ci.x()),
                            (int) Math.round(ci.y()),
                            (int) Math.round(ci.width()),
                            (int) Math.round(ci.height()),
                            ci.value(),
                            ci.fontSize(),
                            ci.bold()
                        )
                    );
                }
                else if (ci.isDynamic() && ci.fieldName() != null)
                {
                    sb.append(
                        FragmentLibrary.textField(
                            (int) Math.round(ci.x()),
                            (int) Math.round(ci.y()),
                            (int) Math.round(ci.width()),
                            (int) Math.round(ci.height()),
                            ci.fieldName(),
                            ci.alignment(),
                            ci.pattern(),
                            ci.fontSize(),
                            ci.bold()
                        )
                    );
                }
            }

            sb.append("        </band>\n");
        }

        sb.append("      </detail>\n");
        sb.append(FragmentLibrary.footer());
        return sb.toString();
    }
}
