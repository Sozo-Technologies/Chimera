package org.sozotech.utils.hooks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Manages the hook context for a single PageComponent.
 * Hooks register themselves here; the render cycle calls tick() after each render.
 */
public class HookRuntime {

    private final List<EffectHook> effects = new ArrayList<>();
    private final Runnable scheduleRender;

    public HookRuntime(Runnable scheduleRender) {
        this.scheduleRender = scheduleRender;
    }

    /** Create a state hook bound to this runtime's render schedule. */
    public <T> StateHook<T> useState(T initial) {
        return new StateHook<>(initial, scheduleRender);
    }

    /**
     * Register an effect.
     * @param body   Supplier that runs the effect and returns an optional cleanup Runnable (or null).
     * @param deps   null = run every render; new Object[]{} = run once; new Object[]{a,b} = run when a or b change.
     */
    public void useEffect(Supplier<Runnable> body, Object... deps) {
        effects.add(new EffectHook(body, deps));
    }

    /** Convenience overload for effects with no cleanup. */
    public void useEffect(Runnable body, Object... deps) {
        useEffect(() -> { body.run(); return null; }, deps);
    }

    /** Called after each render pass — runs any pending effects. */
    public void tick() {
        for (EffectHook effect : effects) {
            effect.runIfNeeded();
        }
    }

    /** Called on page unmount — runs all cleanups. */
    public void cleanup() {
        for (EffectHook effect : effects) {
            effect.runCleanup();
        }
        effects.clear();
    }
}