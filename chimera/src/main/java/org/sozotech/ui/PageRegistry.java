package org.sozotech.ui;

import org.sozotech.ui.pages.media.DevTrack;
import org.sozotech.utils.core.AppContext;

import org.sozotech.ui.pages.LoadingScreen.LoadingScreen;
import org.sozotech.ui.pages.Home.Home;
import org.sozotech.ui.pages.dev.DebugPage;
import org.sozotech.ui.pages.media.HandTrack;

public class PageRegistry {
    public static void loadRegisteredPages() {
        AppContext.router.register("/loading_screen", LoadingScreen::new);
        AppContext.router.register("/home", Home::new);
        AppContext.router.register("/debug", DebugPage::new);
        AppContext.router.register("/media/handtrack", HandTrack::new);
        AppContext.router.register("/dev/media/handtrack", DevTrack::new);
    }
}
