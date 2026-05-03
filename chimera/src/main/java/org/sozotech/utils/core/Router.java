package org.sozotech.utils.core;

import org.sozotech.utils.Transition;
import org.sozotech.utils.page.Page;

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
        if (supplier == null) throw new RuntimeException("Route not found: " + path);
        Page page = supplier.get();
        if (params != null) page.parameters(params);
        renderer.render(page, transition, bgColor);
    }

    public void navigate(String path, Map<String, Object> params) {
        this.navigate(path, params, Transition.NONE, null);
    }

    public void navigate(String path) {
        this.navigate(path, null, Transition.NONE, null);
    }

    public void navigate(String path, Transition transition, String bgColor) {
        this.navigate(path, null, transition, bgColor);
    }
}