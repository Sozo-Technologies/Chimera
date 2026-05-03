package org.sozotech.utils;
import javafx.scene.Parent;

import java.util.Map;

public interface Page {
    Parent getView();
    void parameters(Map<String, Object> args);
    void onMount();
    void onUnmount();
}
