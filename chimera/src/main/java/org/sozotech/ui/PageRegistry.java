package org.sozotech.ui;

import org.sozotech.ui.pages.media.DevTrack;
import org.sozotech.ui.splash.SAboutPage;
import org.sozotech.ui.splash.SHomePage;
import org.sozotech.ui.splash.SToolsPage;
import org.sozotech.ui.splash.STranslationPage;
import org.sozotech.utils.core.AppContext;

import org.sozotech.ui.pages.LoadingScreen.LoadingScreen;
import org.sozotech.ui.pages.Home.Home;
import org.sozotech.ui.pages.dev.DebugPage;
import org.sozotech.ui.pages.media.HandTrack;
import org.sozotech.ui.pages.introduction.IntroPage;

public class PageRegistry {
    public static void loadRegisteredPages() {
        AppContext.router.register("/loading_screen", LoadingScreen::new);

        AppContext.router.register("/debug", DebugPage::new);
        AppContext.router.register("/dev/media/handtrack", DevTrack::new);

        AppContext.router.register("/home", Home::new);
        AppContext.router.register("/splash_home", SHomePage::new);

        AppContext.router.register("/splash_about", SAboutPage::new);


        AppContext.router.register("/media/handtrack", HandTrack::new);
        AppContext.router.register("splash_translation", STranslationPage::new);

        AppContext.router.register("splash_tools", SToolsPage::new);
        AppContext.router.register("/intropage", IntroPage::new);
    }
}
