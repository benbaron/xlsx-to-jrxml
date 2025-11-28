package com.acme.jrgen;

/**
 * Encapsulates JRXML fragments using real Jasper elements.
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
                          uuid=\"00000000-0000-0000-0000-000000000000\">\n""",
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
            """;
    }

    public static String parameters()
    {
        return """
              <parameter name=\"reportTitle\" class=\"java.lang.String\"/>
              <parameter name=\"organizationName\" class=\"java.lang.String\"/>
            """;
    }

    public static String footer()
    {
        return "</jasperReport>\n";
    }

    public static String staticText(int x,
                                    int y,
                                    int w,
                                    int h,
                                    String text,
                                    double fontSize,
                                    boolean bold)
    {
        String template = """
                  <staticText>
                    <reportElement x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" style=\"label\"/>
                    <textElement>
                      <font size=\"%.1f\"%s/>
                    </textElement>
                    <text><![CDATA[%s]]></text>
                  </staticText>\n""";

        String boldAttr = bold ? " isBold=\"true\"" : "";
        return String.format(template, x, y, w, h, fontSize, boldAttr, cdata(text));
    }

    public static String textField(int x,
                                   int y,
                                   int w,
                                   int h,
                                   String fieldName,
                                   String alignment,
                                   String pattern,
                                   double fontSize,
                                   boolean bold)
    {
        String patternAttr = (pattern == null) ? "" : " pattern=\"" + escape(pattern) + "\"";
        String alignAttr = alignment == null ? "" : String.format("<textAlignment>%s</textAlignment>\n", alignment);
        String template = """
                  <textField%s>
                    <reportElement x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" style=\"value\"/>
                    <textElement>
                      %s      <font size=\"%.1f\"%s/>
                    </textElement>
                    <textFieldExpression><![CDATA[$F{%s}]]></textFieldExpression>
                  </textField>\n""";

        String boldAttr = bold ? " isBold=\"true\"" : "";
        return String.format(
            template,
            patternAttr,
            x,
            y,
            w,
            h,
            alignAttr,
            fontSize,
            boldAttr,
            escape(fieldName)
        );
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
}
