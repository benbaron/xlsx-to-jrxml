package com.acme.jrgen;

/**
 * JRXML snippets used by the builder.
 */
public final class FragmentLibrary
{
    private FragmentLibrary()
    {
    }

    public static String header(String reportName,
                                int pageWidth,
                                int pageHeight,
                                int left,
                                int right,
                                int top,
                                int bottom)
    {
        int columnWidth = pageWidth - (left + right);

        return String.format("""
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <jasperReport name=\"%s\" language=\"java\"
                          pageWidth=\"%d\" pageHeight=\"%d\"
                          columnWidth=\"%d\"
                          leftMargin=\"%d\" rightMargin=\"%d\"
                          topMargin=\"%d\" bottomMargin=\"%d\"
                          uuid=\"00000000-0000-0000-0000-000000000000\">
            """,
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

    public static String styles()
    {
        return """
              <style name=\"base\" fontName=\"SansSerif\" fontSize=\"10\"/>
              <style name=\"label\" isBold=\"true\"/>
              <style name=\"value\"/>
            ";
    }

    public static String parameters()
    {
        return """
              <parameter name=\"reportTitle\" class=\"java.lang.String\"/>
              <parameter name=\"organizationName\" class=\"java.lang.String\"/>
            ";
    }

    public static String staticText(int x,
                                    int y,
                                    int w,
                                    int h,
                                    String text,
                                    double fontSize,
                                    String align)
    {
        return String.format("""
                <staticText>
                  <reportElement x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>
                  <textElement%s>
                    <font size=\"%s\"/>
                  </textElement>
                  <text><![CDATA[%s]]></text>
                </staticText>
            """,
            x, y, w, h,
            textAlignmentAttribute(align),
            formatFont(fontSize),
            cdata(text)
        );
    }

    public static String textField(int x,
                                   int y,
                                   int w,
                                   int h,
                                   String fieldName,
                                   double fontSize,
                                   String align,
                                   String pattern)
    {
        String patternFragment = (pattern == null || pattern.isBlank())
            ? ""
            : String.format("      <pattern>%s</pattern>%n", escape(pattern));

        return String.format("""
                <textField>
                  <reportElement x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>
                  <textElement%s>
                    <font size=\"%s\"/>
                  </textElement>
%s                  <textFieldExpression><![CDATA[$F{%s}]]></textFieldExpression>
                </textField>
            """,
            x, y, w, h,
            textAlignmentAttribute(align),
            formatFont(fontSize),
            patternFragment,
            escape(fieldName)
        );
    }

    public static String expressionTextField(int x,
                                             int y,
                                             int w,
                                             int h,
                                             String expression,
                                             double fontSize,
                                             String align)
    {
        return String.format("""
                <textField>
                  <reportElement x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>
                  <textElement%s>
                    <font size=\"%s\"/>
                  </textElement>
                  <textFieldExpression><![CDATA[%s]]></textFieldExpression>
                </textField>
            """,
            x, y, w, h,
            textAlignmentAttribute(align),
            formatFont(fontSize),
            expression
        );
    }

    public static String footer()
    {
        return "</jasperReport>\n";
    }

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

    private static String formatFont(double fontSize)
    {
        return String.format("%s", fontSize);
    }

    private static String textAlignmentAttribute(String align)
    {
        if (align == null || align.isBlank())
        {
            return "";
        }
        return String.format(" textAlignment=\"%s\"", escape(align));
    }
}
