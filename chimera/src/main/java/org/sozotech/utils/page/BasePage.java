package org.sozotech.utils.page;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.sozotech.utils.hooks.HookRuntime;
import org.sozotech.utils.hooks.StateHook;

import java.util.Map;
import java.util.function.Supplier;

public abstract class BasePage implements Page {

    protected final StackPane slot = new StackPane();
    private final HookRuntime hooks = new HookRuntime(this::scheduleRender);

    private boolean renderPending = false;
    private boolean mounted = false;

    protected <T> StateHook<T> useState(T initial) {
        return hooks.useState(initial);
    }

    protected void useEffect(Runnable body, Object... deps) {
        hooks.useEffect(body, deps);
    }

    protected void useEffect(Supplier<Runnable> body, Object... deps) {
        hooks.useEffect(body, deps);
    }

    @Override
    public Parent getView() {
        return slot;
    }

    @Override
    public void parameters(Map<String, Object> args) {}

    @Override
    public void onMount() {
        mounted = true;
        performRender();
    }

    @Override
    public void onUnmount() {
        mounted = false;
        hooks.cleanup();
    }

    protected abstract void performRender();

    private void scheduleRender() {
        if (renderPending || !mounted) return;
        renderPending = true;
        Platform.runLater(() -> {
            renderPending = false;
            if (mounted) performRender();
        });
    }

    protected void tickHooks() {
        hooks.tick();
    }
}