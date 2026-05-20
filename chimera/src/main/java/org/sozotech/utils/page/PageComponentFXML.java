package org.sozotech.utils.page;

import javafx.fxml.FXMLLoader;

public abstract class PageComponentFXML extends BasePage {

    public PageComponentFXML(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setController(this);
            slot.getChildren().add(loader.load());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load FXML: " + fxml, e);
        }
    }

    @Override
    protected void performRender() {
        onRender();
        tickHooks();
    }

    protected abstract void onRender();
}