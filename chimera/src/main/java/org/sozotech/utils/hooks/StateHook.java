package org.sozotech.utils.hooks;

import javafx.application.Platform;
import java.util.function.UnaryOperator;

public class StateHook<T> {

    private T value;
    private final Runnable onUpdate;

    public StateHook(T initial, Runnable onUpdate) {
        this.value = initial;
        this.onUpdate = onUpdate;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        this.value = newValue;
        Platform.runLater(onUpdate);
    }

    public void update(UnaryOperator<T> updater) {
        set(updater.apply(value));
    }
}