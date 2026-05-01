package org.sozotech.controllers;

import org.sozotech.frontend.MainFrame;

public class MainController {

    private final MainFrame view;

    public MainController(MainFrame view) {
        this.view = view;
        init();
    }

    private void init() {
        view.setTitle("SozoTech Initialized 🚀");
    }

    public void handleClick() {
        view.setTitle("Button Clicked!");
    }
}