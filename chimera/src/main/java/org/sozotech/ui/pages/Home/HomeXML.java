package org.sozotech.ui.pages.Home;

import org.sozotech.utils.page.PageComponentFXML;

import java.util.Map;

public class HomeXML extends PageComponentFXML {

    public HomeXML() {
        super("/fxml/Home.fxml");
    }

    @Override
    public void parameters(Map<String, Object> args) {

    }

    @Override
    public void onMount() {
        System.out.println("Home Page Mounted!");
    }

    @Override
    public void onUnmount() {

    }
}