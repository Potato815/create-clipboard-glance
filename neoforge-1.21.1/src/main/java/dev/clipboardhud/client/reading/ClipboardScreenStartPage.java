package dev.clipboardhud.client.reading;

/** Start page of Create's own screen. A personal HUD position wins; otherwise Create's saved page is kept. */
public final class ClipboardScreenStartPage {
    private ClipboardScreenStartPage() {}

    public static int choose(boolean personal, int personalPage, int createPage, int pageCount) {
        return personal && pageCount > 0 ? PersonalPagePositions.clamp(personalPage, pageCount) : createPage;
    }
}
