package com.acme.jrgen;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Build JRXML text from a SheetModel using your semantic format:
 *
 * - <jasperReport ...>
 * - <field .../>
 * - <title height="..."> <element .../> ... </title>
 * - <detail> <band height="..."> <element .../> ... </band> </detail>
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

        // --- Fields: one per dynamic cell (String for now) ----------------
        Set<String> dynamicFields = m.items().stream()
            .filter(CellItem::isDynamic)
            .map(CellItem::fieldName)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String f : dynamicFields)
        {
            sb.append("  <field name=\"")
              .append(escape(f))
              .append("\" class=\"java.lang.String\"/>\n");
        }

        sb.append("\n");

        // --- Title section: put everything in the title for now -----------
        // (Single-page forms are fine with this. If you later want a big
        //  detail band instead, we can move the elements there.)
        int maxBottom = m.items().stream()
            .mapToInt(ci -> ci.y() + ci.height())
            .max()
            .orElse(0);

        int titleHeight = maxBottom + 10;  // small padding

        sb.append("  <title height=\"")
          .append(titleHeight)
          .append("\">\n");

        for (CellItem ci : m.items())
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

        // --- Minimal detail band (empty, but structurally valid) ----------
        // Your examples always have <detail><band ...> even if it does
        // nothing. We'll keep a tiny band here.
        sb.append("  <detail>\n");
        sb.append("    <band height=\"10\"/>\n");
        sb.append("  </detail>\n");

        // Close report
        sb.append(FragmentLibrary.footer());

        return sb.toString();
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
