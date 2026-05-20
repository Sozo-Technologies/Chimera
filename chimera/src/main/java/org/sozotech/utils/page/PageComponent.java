package org.sozotech.utils.page;

import javafx.scene.Parent;

public abstract class PageComponent extends BasePage {

    @Override
    protected void performRender() {
        slot.getChildren().setAll(createView());
        tickHooks();
    }

    protected abstract Parent createView();
}