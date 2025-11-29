package com.acme.jrgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build JRXML text from a SheetModel using your semantic format:
 *
 * - <jasperReport ...>
 * - <field name="..." class="..."/>
 * - <title height="..."> <element .../> ... </title>
 * - <detail>
 *       <band height="..." [splitType="..."]>
 *           [<printWhenExpression>...</printWhenExpression>]
 *           <element .../>
 *           ...
 *       </band>
 *   </detail>
 *
 * Magenta rectangles (via BandSpec) define which rows belong to the detail band.
 * All other cells remain in the title section.
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
        StringBuilder sb = new StringBuilder(64_000);

        // <jasperReport ...>
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

        // -----------------------------------------------------------------
        // Fields: one per dynamic cell, with types
        // -----------------------------------------------------------------
        Map<String, String> fieldTypes = new LinkedHashMap<>();

        for (CellItem ci : m.items())
        {
            if (ci.isDynamic() && ci.fieldName() != null)
            {
                String f = ci.fieldName();
                String t = ci.javaType();
                if (t == null || t.isBlank())
                {
                    t = "java.lang.String";
                }

                // Use first-seen type if duplicates
                fieldTypes.putIfAbsent(f, t);
            }
        }

        for (Map.Entry<String, String> e : fieldTypes.entrySet())
        {
            sb.append("  <field name=\"")
              .append(escape(e.getKey()))
              .append("\" class=\"")
              .append(escape(e.getValue()))
              .append("\"/>\n");
        }

        sb.append("\n");

        // -----------------------------------------------------------------
        // Partition items into title vs detail (if band is defined)
        // -----------------------------------------------------------------
        BandSpec band = m.band();
        List<CellItem> titleItems = new ArrayList<>();
        List<CellItem> detailItems = new ArrayList<>();

        if (band != null && band.topRow() <= band.bottomRow())
        {
            int topRow = band.topRow();
            int bottomRow = band.bottomRow();

            for (CellItem ci : m.items())
            {
                if (ci.row() >= topRow && ci.row() <= bottomRow)
                {
                    detailItems.add(ci);
                }
                else
                {
                    titleItems.add(ci);
                }
            }
        }
        else
        {
            // No band: everything goes into title; detail is a stub band
            titleItems.addAll(m.items());
        }

        // -----------------------------------------------------------------
        // Title section
        // -----------------------------------------------------------------
        int titleHeight = computeMaxBottom(titleItems);
        if (titleHeight <= 0)
        {
            titleHeight = 10;
        }

        sb.append("  <title height=\"")
          .append(titleHeight)
          .append("\">\n");

        for (CellItem ci : titleItems)
        {
            if (ci.isStatic())
            {
                sb.append(
                    FragmentLibrary.staticText(
                        ci.x(),
                        ci.y(),
                        ci.width(),
                        ci.height(),
                        ci.value()
                    )
                );
            }
            else if (ci.isDynamic() && ci.fieldName() != null)
            {
                sb.append(
                    FragmentLibrary.textField(
                        ci.x(),
                        ci.y(),
                        ci.width(),
                        ci.height(),
                        ci.fieldName()
                    )
                );
            }
        }

        sb.append("  </title>\n\n");

        // -----------------------------------------------------------------
        // Detail band
        // -----------------------------------------------------------------
        if (detailItems.isEmpty())
        {
            // No magenta band or no cells in band: keep a tiny stub
            sb.append("  <detail>\n");
            sb.append("    <band height=\"10\"/>\n");
            sb.append("  </detail>\n");
        }
        else
        {
            int minY = Integer.MAX_VALUE;
            int maxBottom = 0;

            for (CellItem ci : detailItems)
            {
                if (ci.y() < minY)
                {
                    minY = ci.y();
                }
                int bottom = ci.y() + ci.height();
                if (bottom > maxBottom)
                {
                    maxBottom = bottom;
                }
            }

            if (minY == Integer.MAX_VALUE)
            {
                minY = 0;
            }

            int bandHeight = maxBottom - minY;
            if (bandHeight <= 0)
            {
                bandHeight = 10;
            }

            // band.heightOverride, if present, wins
            if (band != null && band.heightOverride() != null && band.heightOverride() > 0)
            {
                bandHeight = band.heightOverride();
            }

            sb.append("  <detail>\n");
            sb.append("    <band height=\"").append(bandHeight).append("\"");

            if (band != null && band.splitType() != null && !band.splitType().isBlank())
            {
                sb.append(" splitType=\"").append(escape(band.splitType())).append("\"");
            }

            sb.append(">\n");

            if (band != null && band.printWhenExpression() != null
                && !band.printWhenExpression().isBlank())
            {
                sb.append("      <printWhenExpression><![CDATA[")
                  .append(band.printWhenExpression())
                  .append("]]></printWhenExpression>\n");
            }

            for (CellItem ci : detailItems)
            {
                int relY = ci.y() - minY;

                if (ci.isStatic())
                {
                    sb.append(
                        FragmentLibrary.staticText(
                            ci.x(),
                            relY,
                            ci.width(),
                            ci.height(),
                            ci.value()
                        )
                    );
                }
                else if (ci.isDynamic() && ci.fieldName() != null)
                {
                    sb.append(
                        FragmentLibrary.textField(
                            ci.x(),
                            relY,
                            ci.width(),
                            ci.height(),
                            ci.fieldName()
                        )
                    );
                }
            }

            sb.append("    </band>\n");
            sb.append("  </detail>\n");
        }

        // Close report
        sb.append(FragmentLibrary.footer());

        return sb.toString();
    }

    private static int computeMaxBottom(List<CellItem> items)
    {
        int max = 0;
        for (CellItem ci : items)
        {
            int bottom = ci.y() + ci.height();
            if (bottom > max)
            {
                max = bottom;
            }
        }
        return max;
    }

    private static String escape(String s)
    {
        if (s == null)
        {
            return "";
        }

        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
