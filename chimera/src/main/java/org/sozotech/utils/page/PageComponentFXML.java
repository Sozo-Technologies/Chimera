package org.sozotech.utils.page;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public abstract class PageComponentFXML implements Page {
    protected final Parent root;

    public PageComponentFXML(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            this.root = loader.load();
        }catch (Exception e) {
            throw new RuntimeException("Failed to load fxml: " + fxml, e);
        }
    }


    public Parent getView() {
        return this.root;
    }
}