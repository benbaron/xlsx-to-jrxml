package com.acme.jrgen;

/**
 * Field definition collected from dynamic cells and inline [[field ...]] tags.
 */
public record FieldSpec(String name,
                        String type,
                        String pattern,
                        String alignment,
                        boolean forced,
                        String originalFormat)
{
}
