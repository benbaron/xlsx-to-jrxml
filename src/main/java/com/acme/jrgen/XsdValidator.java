package com.acme.jrgen;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * XSD-based JRXML validator (optional).
 */
public class XsdValidator
{
    public static List<String> validate(Path xml, Path xsd)
    {
        List<String> errors = new ArrayList<>();

        try
        {
            SchemaFactory sf =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(xsd.toFile());
            Validator v = schema.newValidator();

            v.setErrorHandler(new org.xml.sax.helpers.DefaultHandler()
            {
                @Override
                public void error(org.xml.sax.SAXParseException e)
                {
                    errors.add(format(e));
                }

                @Override
                public void fatalError(org.xml.sax.SAXParseException e)
                {
                    errors.add(format(e));
                }

                @Override
                public void warning(org.xml.sax.SAXParseException e)
                {
                    // ignore warnings
                }

                private String format(org.xml.sax.SAXParseException e)
                {
                    return "line " + e.getLineNumber()
                        + ", col " + e.getColumnNumber()
                        + ": " + e.getMessage();
                }
            });

            v.validate(new StreamSource(xml.toFile()));
        }
        catch (Exception ex)
        {
            errors.add("Validator error: " + ex.getMessage());
        }

        return errors;
    }
}
