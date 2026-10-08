package dev.clipboardhud.client.reading;

import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import dev.clipboardhud.client.input.HudPageInputPolicy;
import dev.clipboardhud.client.layout.ClipboardHudFooter;
import dev.clipboardhud.create.ClipboardDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PersonalPageNavigationTest {
    @Test
    void navigationRequiresVisibleHudAndHeldModifierAndMultiplePages() {
        assertTrue(HudPageInputPolicy.handles(true, true, 3, -1));
        assertFalse(HudPageInputPolicy.handles(false, true, 3, -1));
        assertFalse(HudPageInputPolicy.handles(true, false, 3, -1));
        assertFalse(HudPageInputPolicy.handles(true, true, 1, -1));
        assertFalse(HudPageInputPolicy.handles(true, true, 0, -1));
        assertFalse(HudPageInputPolicy.handles(true, true, 3, 0));
        assertFalse(HudPageInputPolicy.handles(true, true, 3, Double.NaN));
        assertFalse(HudPageInputPolicy.handles(true, true, 3, Double.POSITIVE_INFINITY));
    }

    @Test
    void wheelDirectionMatchesCreateAndStopsAtBothBoundariesWithoutFallingThrough() {
        var positions = new PersonalPagePositions<Object>();
        Object target = new Object();
        assertEquals(1, HudPageInputPolicy.step(-1));
        assertEquals(-1, HudPageInputPolicy.step(1));
        assertEquals(1, HudPageInputPolicy.step(-.1));
        assertEquals(1, HudPageInputPolicy.step(-100));
        assertEquals(0, HudPageInputPolicy.step(Double.NaN));
        positions.move(target, -1, 3);
        assertEquals(0, positions.current(target, 3));
        assertTrue(HudPageInputPolicy.handles(true, true, 3, 1));
        positions.move(target, 1, 3);
        assertEquals(1, positions.current(target, 3));
        positions.move(target, Integer.MAX_VALUE, 3);
        assertEquals(2, positions.current(target, 3));
        assertTrue(HudPageInputPolicy.handles(true, true, 3, -1));
        positions.move(target, Integer.MIN_VALUE, 3);
        assertEquals(0, positions.current(target, 3));
    }

    @Test
    void targetAndPlayerPositionsRemainSeparateAndSessionClearRemovesThem() {
        var firstPlayer = new PersonalPagePositions<Object>();
        var secondPlayer = new PersonalPagePositions<Object>();
        Object a = new Object(), b = new Object(), replacementA = new Object();
        firstPlayer.remember(a, 2, 4);
        firstPlayer.remember(b, 1, 4);
        secondPlayer.remember(a, 3, 4);
        assertEquals(2, firstPlayer.current(a, 4));
        assertEquals(1, firstPlayer.current(b, 4));
        assertEquals(3, secondPlayer.current(a, 4));
        assertEquals(0, firstPlayer.current(replacementA, 4));
        firstPlayer.clear();
        assertEquals(0, firstPlayer.current(a, 4));
        assertEquals(3, secondPlayer.current(a, 4));
    }

    @Test
    void closedScreenPositionSurvivesAnOlderSnapshotAndAlwaysDisplaysAValidPage() {
        var positions = new PersonalPagePositions<Object>();
        Object target = new Object();
        positions.remember(target, 3, 4);
        assertEquals(0, positions.current(target, 1));
        assertEquals(0, positions.current(target, 0));
        assertEquals(3, positions.current(target, 4));
        assertEquals(1, positions.current(target, 2));
        positions.remember(target, -100, 4);
        assertEquals(0, positions.current(target, 4));
        positions.remember(target, 100, 4);
        assertEquals(3, positions.current(target, 4));
    }

    @Test
    void personalMemoryHasABoundAndEvictsLeastRecentlyUsedTargets() {
        var positions = new PersonalPagePositions<Object>(2);
        Object a = new Object(), b = new Object(), c = new Object();
        positions.remember(a, 1, 3);
        positions.remember(b, 2, 3);
        assertEquals(1, positions.current(a, 3));
        positions.remember(c, 1, 3);
        assertEquals(0, positions.current(b, 3));
        assertEquals(1, positions.current(a, 3));
        assertEquals(1, positions.current(c, 3));
    }

    @Test
    void selectedPageAndNumberStayTogetherWithoutChangingCreateContentOrSharedPosition() {
        var registries = HolderLookup.Provider.create(BuiltInRegistries.REGISTRY.stream().map(registry -> registry.asLookup()));
        var first = new ClipboardEntry(false, Component.literal("첫 페이지"));
        var second = new ClipboardEntry(true, Component.literal("# 둘째 주소"));
        var rawPages = new ArrayList<List<ClipboardEntry>>();
        rawPages.add(new ArrayList<>(List.of(first)));
        rawPages.add(new ArrayList<>(List.of(second)));
        var source = new ClipboardContent(ClipboardType.WRITTEN, rawPages, false).setPreviouslyOpenedPage(0);
        var document = new ClipboardDocumentSnapshot(source.pages(), registries);
        var positions = new PersonalPagePositions<Object>();
        Object target = new Object();
        positions.move(target, 1, document.pages().size());
        var view = ClipboardPageView.select(document, positions.current(target, document.pages().size()));
        assertEquals(1, view.index());
        assertEquals("# 둘째 주소", view.entries().getFirst().text().getString());
        assertTrue(view.entries().getFirst().checked());
        var footer = ClipboardHudFooter.text(view.index() + 1, view.pageCount(), false);
        assertArrayEquals(new Object[]{2, 2}, ((TranslatableContents) footer.getContents()).getArgs());
        view.entries().getFirst().text().copy().append("display-only");
        assertEquals("# 둘째 주소", second.text.getString());
        assertEquals(0, source.previouslyOpenedPage());
        assertFalse(first.checked);
        assertTrue(second.checked);
        assertTrue(document.matches(source.pages()));
        assertEquals(1, ClipboardPageView.select(document, 500).index());
        assertEquals(0, ClipboardPageView.select(null, 500).index());
        assertTrue(ClipboardPageView.select(null, 500).entries().isEmpty());
    }

    @Test
    void originalScreenContinuesFromThePersonalPageOnlyOnAScreenCopy() {
        var positions = new PersonalPagePositions<Object>();
        Object target = new Object(), unseen = new Object();
        positions.move(target, 1, 3);
        positions.move(target, 1, 3);
        assertTrue(positions.has(target));
        assertFalse(positions.has(unseen));
        // The HUD page wins over another player's shared page; without a personal page Create keeps its own.
        assertEquals(2, ClipboardScreenStartPage.choose(positions.has(target), positions.current(target, 3), 0, 3));
        assertEquals(1, ClipboardScreenStartPage.choose(false, positions.current(unseen, 3), 1, 3));
        // An unseen target shown in the HUD starts on the page it displayed, the first page.
        assertEquals(0, ClipboardScreenStartPage.choose(true, positions.current(unseen, 3), 2, 3));
        // Pages removed after the HUD read them still open a valid page; an empty document is left to Create.
        assertEquals(1, ClipboardScreenStartPage.choose(true, positions.current(target, 2), 0, 2));
        assertEquals(4, ClipboardScreenStartPage.choose(true, 2, 4, 0));

        var rawPages = new ArrayList<List<ClipboardEntry>>();
        for (int i = 0; i < 3; i++) rawPages.add(new ArrayList<>(List.of(new ClipboardEntry(false, Component.literal("page " + i)))));
        var shared = new ClipboardContent(ClipboardType.WRITTEN, rawPages, false).setPreviouslyOpenedPage(0);
        var screenCopy = shared.setPreviouslyOpenedPage(positions.current(target, shared.pages().size()));
        assertEquals(2, screenCopy.previouslyOpenedPage());
        assertEquals(0, shared.previouslyOpenedPage());
        assertSame(shared.pages(), screenCopy.pages());
    }
}
