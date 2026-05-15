package org.sozotech.utils.page;
import javafx.scene.Parent;

public abstract class PageComponent implements Page {
    protected Parent root;

    public PageComponent() {
        this.root = createView();
    }

    protected abstract Parent createView();

    public Parent getView() {
        return root;
    }
}