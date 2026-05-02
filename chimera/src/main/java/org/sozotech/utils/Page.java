package org.sozotech.utils;
import javafx.scene.Parent;

public interface Page {
    Parent getView();
    void onMount();
    void onUnmount();
}
