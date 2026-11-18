package com.periomeasure.web;

import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;

/**
 * Tells TeaVM to embed the main project's resource files in the JavaScript,
 * so {@code Messages} (all UI texts) and {@code Version} read exactly the same
 * files as the desktop program.
 */
public final class BundledResources implements ResourceSupplier {

    @Override
    public String[] supplyResources(ResourceSupplierContext context) {
        return new String[] {
            "com/periomeasure/i18n/messages.properties",
            "com/periomeasure/periomeasure-version.properties"
        };
    }
}
