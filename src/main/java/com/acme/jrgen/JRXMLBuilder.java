package com.acme.jrgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.acme.jrgen.model.CellItem;

/**
 * Build JRXML text from a SheetModel using your semantic format:
 *
 * - <jasperReport ...>
 * - <field name="..." class="..."/>
 * - optional background/title/pageHeader/columnHeader/columnFooter/pageFooter/summary
 * - <detail>
 *       <band height="..." [splitType="..."]>
 *           [<printWhenExpression>...</printWhenExpression>]
 *           <element .../>
 *           ...
 *       </band>
 *   </detail>
 *
 * Section routing is driven by CellItem.section():
 *   BACKGROUND    -> <background ...>
 *   TITLE         -> <title ...>
 *   PAGE_HEADER   -> <pageHeader ...>
 *   COLUMN_HEADER -> <columnHeader ...>
 *   DETAIL        -> <detail><band ...>...</band></detail>
 *   COLUMN_FOOTER -> <columnFooter ...>
 *   PAGE_FOOTER   -> <pageFooter ...>
 *   SUMMARY       -> <summary ...>
 *
 * If no cells are mapped to a section, that section is omitted (except <detail>,
 * which is always present with at least a stub band).
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
        // Partition items into sections
        // -----------------------------------------------------------------
        List<CellItem> backgroundItems    = new ArrayList<>();
        List<CellItem> titleItems         = new ArrayList<>();
        List<CellItem> pageHeaderItems    = new ArrayList<>();
        List<CellItem> columnHeaderItems  = new ArrayList<>();
        List<CellItem> detailItems        = new ArrayList<>();
        List<CellItem> columnFooterItems  = new ArrayList<>();
        List<CellItem> pageFooterItems    = new ArrayList<>();
        List<CellItem> summaryItems       = new ArrayList<>();

        for (CellItem ci : m.items())
        {
            Section sec = ci.section() != null ? ci.section() : Section.DETAIL;

            switch (sec)
            {
                case BACKGROUND   -> backgroundItems.add(ci);
                case TITLE        -> titleItems.add(ci);
                case PAGE_HEADER  -> pageHeaderItems.add(ci);
                case COLUMN_HEADER-> columnHeaderItems.add(ci);
                case DETAIL       -> detailItems.add(ci);
                case COLUMN_FOOTER-> columnFooterItems.add(ci);
                case PAGE_FOOTER  -> pageFooterItems.add(ci);
                case SUMMARY      -> summaryItems.add(ci);
				default -> throw new IllegalArgumentException("Unexpected value: " + sec);
            }
        }

        // -----------------------------------------------------------------
        // Background
        // -----------------------------------------------------------------
        if (!backgroundItems.isEmpty())
        {
            int h = computeMaxBottom(backgroundItems);
            if (h <= 0)
            {
                h = 10;
            }

            sb.append("  <background height=\"")
              .append(h)
              .append("\">\n");

            for (CellItem ci : backgroundItems)
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

            sb.append("  </background>\n\n");
        }

        // -----------------------------------------------------------------
        // Title
        // -----------------------------------------------------------------
        if (!titleItems.isEmpty())
        {
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
        }

        // -----------------------------------------------------------------
        // Page header
        // -----------------------------------------------------------------
        if (!pageHeaderItems.isEmpty())
        {
            int h = computeMaxBottom(pageHeaderItems);
            if (h <= 0)
            {
                h = 10;
            }

            sb.append("  <pageHeader height=\"")
              .append(h)
              .append("\">\n");

            for (CellItem ci : pageHeaderItems)
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

            sb.append("  </pageHeader>\n\n");
        }

        // -----------------------------------------------------------------
        // Column header
        // -----------------------------------------------------------------
        if (!columnHeaderItems.isEmpty())
        {
            int h = computeMaxBottom(columnHeaderItems);
            if (h <= 0)
            {
                h = 10;
            }

            sb.append("  <columnHeader height=\"")
              .append(h)
              .append("\">\n");

            for (CellItem ci : columnHeaderItems)
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

            sb.append("  </columnHeader>\n\n");
        }

        // -----------------------------------------------------------------
        // Detail band (always present, stub if empty)
        // -----------------------------------------------------------------
        BandSpec band = m.band();

        if (detailItems.isEmpty())
        {
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

        // -----------------------------------------------------------------
        // Column footer
        // -----------------------------------------------------------------
        if (!columnFooterItems.isEmpty())
        {
            int h = computeMaxBottom(columnFooterItems);
            if (h <= 0)
            {
                h = 10;
            }

            sb.append("  <columnFooter height=\"")
              .append(h)
              .append("\">\n");

            for (CellItem ci : columnFooterItems)
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

            sb.append("  </columnFooter>\n\n");
        }

        // -----------------------------------------------------------------
        // Page footer
        // -----------------------------------------------------------------
        if (!pageFooterItems.isEmpty())
        {
            int h = computeMaxBottom(pageFooterItems);
            if (h <= 0)
            {
                h = 10;
            }

            sb.append("  <pageFooter height=\"")
              .append(h)
              .append("\">\n");

            for (CellItem ci : pageFooterItems)
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

            sb.append("  </pageFooter>\n\n");
        }

        // -----------------------------------------------------------------
        // Summary
        // -----------------------------------------------------------------
        if (!summaryItems.isEmpty())
        {
            int h = computeMaxBottom(summaryItems);
            if (h <= 0)
            {
                h = 10;
            }

            sb.append("  <summary height=\"")
              .append(h)
              .append("\">\n");

            for (CellItem ci : summaryItems)
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
        }

            sb.append("  </summary>\n\n");
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
