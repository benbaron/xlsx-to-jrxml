package com.acme.jrgen;

/**
 * Encapsulates JRXML fragments in your *semantic* format:
 *
 * - Root: <jasperReport ...>
 * - Design elements: <element kind="...">
 */
public class FragmentLibrary
{
    public static String header(String reportName,
                                int pageWidth,
                                int pageHeight,
                                int left,
                                int right,
                                int top,
                                int bottom)
    {
        int columnWidth = pageWidth - (left + right);

        // Match your examples:
        // <jasperReport name="..." language="java" pageWidth="..." ...>
        String template = """
            <?xml version="1.0" encoding="UTF-8"?>
            <jasperReport name="%s" language="java"
                          pageWidth="%d" pageHeight="%d"
                          columnWidth="%d"
                          leftMargin="%d" rightMargin="%d"
                          topMargin="%d" bottomMargin="%d"
                          uuid="00000000-0000-0000-0000-000000000000">
            """;

        return String.format(
            template,
            escape(reportName),
            pageWidth,
            pageHeight,
            columnWidth,
            left,
            right,
            top,
            bottom
        );
    }

    public static String footer()
    {
        return "</jasperReport>\n";
    }

    /**
     * Semantic "staticText" element:
     *
     * <element kind="staticText" x="..." y="..." width="..." height="...">
     *   <text><![CDATA[...]]></text>
     * </element>
     */
    public static String staticText(int x,
                                    int y,
                                    int w,
                                    int h,
                                    String text)
    {
        String template = """
                  <element kind="staticText" x="%d" y="%d" width="%d" height="%d">
                    <text><![CDATA[%s]]></text>
                  </element>
            """;

        return String.format(template, x, y, w, h, cdata(text));
    }

    /**
     * Semantic "textField" element:
     *
     * <element kind="textField" x="..." y="..." width="..." height="...">
     *   <expression><![CDATA[$F{fieldName}]]></expression>
     * </element>
     */
    public static String textField(int x,
                                   int y,
                                   int w,
                                   int h,
                                   String fieldName)
    {
        String template = """
                  <element kind="textField" x="%d" y="%d" width="%d" height="%d">
                    <expression><![CDATA[$F{%s}]]></expression>
                  </element>
            """;

        return String.format(template, x, y, w, h, escape(fieldName));
    }

    // --- helpers ---------------------------------------------------------

    static String escape(String s)
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

    static String cdata(String s)
    {
        return (s == null) ? "" : s;
    }
}
