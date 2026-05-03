package org.sozotech.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Router {
    private final Map<String, Supplier<Page>> routes = new HashMap<>();
    private final Renderer renderer;

    public Router(Renderer renderer) {
        this.renderer = renderer;
    }

    public void register(String path, Supplier<Page> pageSupplier) {
        routes.put(path, pageSupplier);
    }

    public void navigate(String path, Map<String, Object> params, Transition transition, String bgColor) {
        Supplier<Page> supplier = routes.get(path);

        if (supplier == null) {
            throw new RuntimeException("Route not found: " + path);
        }

        Page page = supplier.get();

        if (params != null) {
            page.parameters(params);
        }

        renderer.render(page, transition, bgColor);
    }
}