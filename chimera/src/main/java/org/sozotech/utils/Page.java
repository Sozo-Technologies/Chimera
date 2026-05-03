package org.sozotech.utils;
import javafx.scene.Parent;

public interface Page {
    Parent getView();
    void parameters(Object... args);
    void onMount();
    void onUnmount();
}
