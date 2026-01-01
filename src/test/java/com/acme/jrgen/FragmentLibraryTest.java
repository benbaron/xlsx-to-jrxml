package com.acme.jrgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FragmentLibraryTest
{
    @Test
    void textFieldUsesAlignmentAttribute()
    {
        String jrxml = FragmentLibrary.textField(0, 0, 100, 20, "field", 10.0, "Center", null);

        assertTrue(
            jrxml.contains("<textElement textAlignment=\"Center\">"),
            "Alignment should be on the textElement as an attribute");
        assertFalse(jrxml.contains("<textAlignment>"),
            "Alignment should not be emitted as a nested element");
    }

    @Test
    void textFieldOmitsAlignmentWhenNull()
    {
        String jrxml = FragmentLibrary.textField(0, 0, 100, 20, "field", 10.0, null, null);

        assertTrue(jrxml.contains("<textElement>"), "textElement should be present");
        assertFalse(jrxml.contains("textAlignment"),
            "textAlignment attribute should be omitted when alignment is null or blank");
    }
}
