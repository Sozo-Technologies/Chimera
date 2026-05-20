package org.sozotech.utils.hooks;

import java.util.Arrays;
import java.util.function.Supplier;

public class EffectHook {

    private final Supplier<Runnable> body;   // returns cleanup, or null
    private final Object[] deps;             // null = run every render; [] = run once
    private Object[] prevDeps;
    private Runnable cleanup;

    public EffectHook(Supplier<Runnable> body, Object[] deps) {
        this.body = body;
        this.deps = deps;
    }

    /** Called after each render. Runs body if deps changed. */
    public void runIfNeeded() {
        if (shouldRun()) {
            runCleanup();
            cleanup = body.get();
            prevDeps = deps == null ? null : Arrays.copyOf(deps, deps.length);
        }
    }

    /** Called on unmount. */
    public void runCleanup() {
        if (cleanup != null) {
            cleanup.run();
            cleanup = null;
        }
    }

    private boolean shouldRun() {
        if (deps == null) return true;               // no deps → always run
        if (prevDeps == null) return true;           // first run
        if (deps.length == 0) return false;          // [] → run once only
        return !Arrays.equals(deps, prevDeps);       // run when deps changed
    }
}