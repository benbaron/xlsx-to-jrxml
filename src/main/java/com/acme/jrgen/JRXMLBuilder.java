package com.acme.jrgen;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

        double availableWidth = pageWidth - (leftMargin + rightMargin);
        double availableHeight = pageHeight - (topMargin + bottomMargin);
        double scaleX = availableWidth / m.totalWidth();
        double scaleY = availableHeight / m.totalHeight();

        sb.append(FragmentLibrary.header(
            m.sheetName(),
            pageWidth,
            pageHeight,
            leftMargin,
            rightMargin,
            topMargin,
            bottomMargin
        ));

        sb.append(FragmentLibrary.styles());
        sb.append(FragmentLibrary.parameters());

        Set<String> fieldOrder = new LinkedHashSet<>();
        Map<String, FieldSpec> fieldLookup = new LinkedHashMap<>();
        m.bands().forEach(b -> b.items().stream()
            .filter(CellItem::isDynamic)
            .map(CellItem::fieldSpec)
            .filter(Objects::nonNull)
            .forEach(fs -> {
                fieldOrder.add(fs.name());
                fieldLookup.putIfAbsent(fs.name(), fs);
            }));

        for (String f : fieldOrder)
        {
            FieldSpec fs = fieldLookup.get(f);
            String clazz = fs.type() == null || fs.type().isBlank() ? "java.lang.String" : fs.type();
            sb.append("  <field name=\"")
              .append(FragmentLibrary.escape(f))
              .append("\" class=\"")
              .append(FragmentLibrary.escape(clazz))
              .append("\"/>\n");
        }

        sb.append("\n");

        sb.append(buildTitle(m.sheetName()));

        sb.append("  <detail>\n");
        for (Band band : m.bands())
        {
            sb.append(buildBand(band, scaleX, scaleY));
        }
        sb.append("  </detail>\n");

        sb.append(FragmentLibrary.footer());

        return sb.toString();
    }

    private static String buildTitle(String sheetName)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("  <title>\n");
        sb.append("    <band height=\"80\">\n");
        sb.append(FragmentLibrary.expressionTextField(10, 0, 400, 20, "$P{reportTitle}", 12, "Left"));
        sb.append(FragmentLibrary.expressionTextField(10, 22, 400, 20, "$P{organizationName}", 10, "Left"));
        sb.append(FragmentLibrary.staticText(10, 44, 400, 20, sheetName, 10, "Left"));
        sb.append("    </band>\n");
        sb.append("  </title>\n\n");
        return sb.toString();
    }

    private static String buildBand(Band band, double scaleX, double scaleY)
    {
        StringBuilder sb = new StringBuilder();
        double bandHeight = band.effectiveHeight(scaleY);
        sb.append("    <band height=\"")
          .append((int) Math.round(bandHeight))
          .append("\" splitType=\"")
          .append(band.splitType() == null ? "Stretch" : band.splitType())
          .append("\">");

        if (band.printWhenExpression() != null && !band.printWhenExpression().isBlank())
        {
            sb.append("\n      <printWhenExpression><![CDATA[")
              .append(band.printWhenExpression())
              .append("]]></printWhenExpression>");
        }
        sb.append('\n');

        for (CellItem ci : band.items())
        {
            if (!ci.render())
            {
                continue;
            }

            int x = (int) Math.round(ci.x() * scaleX);
            int y = (int) Math.round(ci.y() * scaleY);
            int w = (int) Math.round(ci.width() * scaleX);
            int h = (int) Math.round(ci.height() * scaleY);
            double font = clampFont(ci.fontSize() * scaleY);

            if (ci.isStatic())
            {
                sb.append(FragmentLibrary.staticText(x, y, w, h, ci.value(), font, ci.alignment()));
            }

            for (CellItem ci : m.items())
            {
                sb.append(FragmentLibrary.textField(x, y, w, h, ci.fieldName(), font, ci.fieldSpec().alignment(), ci.fieldSpec().pattern()));
            }

        sb.append("    </band>\n");
        return sb.toString();
    }

    private static double clampFont(double size)
    {
        return Math.max(6.0, Math.min(22.0, size));
    }
}
