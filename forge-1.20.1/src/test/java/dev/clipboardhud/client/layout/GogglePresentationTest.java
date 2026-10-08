package dev.clipboardhud.client.layout;

import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import dev.clipboardhud.client.render.ClipboardCheckbox;
import dev.clipboardhud.client.render.ClipboardPageArrow;
import dev.clipboardhud.client.style.ClipboardEntryAppearance;
import dev.clipboardhud.client.style.ClipboardHudPadding;
import dev.clipboardhud.client.style.ClipboardPageStyle;
import dev.clipboardhud.client.style.ClipboardPixelScale;
import dev.clipboardhud.create.ClipboardEntrySnapshot;
import dev.clipboardhud.testing.VanillaBootstrap;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GogglePresentationTest {
    private static final int WHITE = 0xFFFFFFFF;

    @Test
    void followsCreateEntranceAndSlideDirectionAndFinishesWithoutDrift() {
        assertEquals(0, GoggleHudGeometry.fade(0, 0));
        assertEquals(.5f, GoggleHudGeometry.fade(12, 0));
        assertEquals(1, GoggleHudGeometry.fade(24, .5f));
        assertEquals(8, GoggleHudGeometry.slide(0, 20));
        assertEquals(-8, GoggleHudGeometry.slide(0, -20));
        assertEquals(0, GoggleHudGeometry.slide(1, 20));
    }

    @Test
    void normalPositionUsesTheSameCenterAnchorAsCreate() {
        assertEquals(340, GoggleHudGeometry.anchorX(640, 100, 20));
        assertEquals(8, GoggleHudGeometry.tooltipHeight(1));
        assertEquals(38, GoggleHudGeometry.tooltipHeight(4));
        assertEquals(10, GoggleHudGeometry.lineY(1));
    }

    @Test
    void checkboxStateCannotMoveTextOrMaterialIconsAndWrappedLinesStayAligned() {
        // A font/resource pack may assign different widths to the two checkbox glyphs.
        int column = ClipboardMarkerSlot.checkboxColumn(6, 9, 4);
        var text = Component.literal("한글 English");
        var empty = EntryLine.row(text, EntryMarker.OPEN_BOX, column, false, WHITE);
        var checked = EntryLine.row(text, EntryMarker.CHECKED_BOX, column, false, WHITE);
        var continued = EntryLine.row(text, EntryMarker.NONE, column, false, WHITE);
        assertEquals(13, empty.textOffset());
        assertEquals(empty.textOffset(), checked.textOffset());
        assertEquals(empty.textOffset(), continued.textOffset());
        var emptyMaterial = EntryLine.row(text, empty.marker(), column, true, WHITE);
        var checkedMaterial = EntryLine.row(text, checked.marker(), column, true, WHITE);
        assertEquals(33, emptyMaterial.textOffset());
        assertEquals(emptyMaterial.textOffset(), checkedMaterial.textOffset());
        assertEquals(20, emptyMaterial.textOffset() - empty.textOffset());
        assertEquals(empty.measuredWidth(line -> 40), checked.measuredWidth(line -> 40));
    }

    @Test
    void pageLabelScaleKeepsWholeScreenPixelsPerGlyphPixelAndUsesGoggleSecondaryShade() {
        // Nearest to 70%: GUI 2 -> 1px, 3 -> 2px, 4 -> 3px, 5 -> 4px (3.5 rounds up), 6 -> 4px per glyph pixel.
        assertEquals(.5f, ClipboardPixelScale.of(.70f, 2), 1e-6f);
        assertEquals(2f / 3, ClipboardPixelScale.of(.70f, 3), 1e-6f);
        assertEquals(.75f, ClipboardPixelScale.of(.70f, 4), 1e-6f);
        assertEquals(.8f, ClipboardPixelScale.of(.70f, 5), 1e-6f);
        assertEquals(4f / 6, ClipboardPixelScale.of(.70f, 6), 1e-6f);
        for (int gui = 1; gui <= 8; gui++) {
            float scale = ClipboardPixelScale.of(.70f, gui);
            double screenPixels = scale * gui;
            assertEquals(Math.rint(screenPixels), screenPixels, 1e-6, "gui " + gui);
            assertTrue(scale > 0 && scale <= 1);
        }
        assertEquals(1f, ClipboardPixelScale.of(.70f, 1));
        assertEquals(.70f, ClipboardPixelScale.of(.70f, 0));
        assertEquals(.70f, ClipboardPixelScale.of(.70f, Double.NaN));
        // Right-aligned positions land on whole screen pixels.
        assertEquals(10.25f, ClipboardPixelScale.snap(10.2f, 4), 1e-6f);
        assertEquals(10.5f, ClipboardPixelScale.snap(10.4f, 2), 1e-6f);
        // Create goggle HUDs draw secondary notes (e.g. "at current speed") in DARK_GRAY.
        assertEquals(ChatFormatting.DARK_GRAY.getColor(), ClipboardPageStyle.color());
    }

    @Test
    void pageArrowsKeepAConstantGapToTheNumberAndArrowSlotsNeverMoveTheLabel() {
        int arrow = 4;
        float scale = .75f;
        ToIntFunction<FormattedText> measure = text -> text.getString().length() * 6;
        int gap = ClipboardPageFooter.arrowGap();
        // e.g. a 14-page and an 8-page clipboard: the same gaps between arrows and number.
        for (String label : new String[]{"3 / 8", "3 / 14", "10 / 14", "1 / 2"}) {
            int labelWidth = measure.applyAsInt(Component.literal(label));
            int x = ClipboardPageFooter.labelX(arrow, labelWidth, labelWidth);
            assertEquals(arrow + gap, x, label);
            assertEquals(gap, ClipboardPageFooter.nextArrowX(arrow, labelWidth) - (x + labelWidth), label);
            var widths = new java.util.HashSet<Float>();
            for (boolean previous : new boolean[]{false, true}) {
                for (boolean next : new boolean[]{false, true}) {
                    var footer = new ClipboardPageFooter(Component.literal(label), null, previous, next, labelWidth, scale);
                    widths.add(footer.measuredWidth(measure, arrow));
                    assertTrue(ClipboardPageFooter.isFooter(footer));
                }
            }
            assertEquals(1, widths.size(), "an arrow appearing or disappearing must not move the label");
        }
        int slot = measure.applyAsInt(Component.literal("1 / 12"));
        // The clipped mark sits at the left, so it adds width without moving the right-aligned group.
        var clippedFooter = new ClipboardPageFooter(Component.literal("1 / 12"), Component.literal("…"), false, true, slot, scale);
        assertTrue(clippedFooter.measuredWidth(measure, arrow) > ClipboardPageFooter.groupWidth(arrow, slot) * scale);
        var clippedOnly = new ClipboardPageFooter(null, Component.literal("…"), false, false, 0, scale);
        assertEquals(6 * scale, clippedOnly.measuredWidth(measure, arrow), 1e-6);
        // Spacing and the box treat the new row as the footer; plain body rows are not footers.
        var body = EntryLine.row(Component.literal("본문"), EntryMarker.OPEN_BOX, 12, false, WHITE);
        List<HudLine> lines = List.of(body, clippedFooter);
        assertTrue(ClipboardHudBox.hasFooter(lines));
        assertEquals(10 + ClipboardHudSpacing.footerGap(), ClipboardHudSpacing.lineY(lines, 1));
        assertFalse(ClipboardPageFooter.isFooter(body));
    }

    @Test
    void unusableArrowStaysVisibleButFaint() {
        assertEquals(1, ClipboardPageArrow.alpha(true));
        float faint = ClipboardPageArrow.alpha(false);
        assertTrue(faint > .1f && faint < .6f, "visible but clearly weaker: " + faint);
    }

    @Test
    void whenTheFooterSetsTheBoxWidthANewDigitWidensOnlyToTheRight() {
        // Label widths 9 / 14 -> 10 / 14 with a 3px-per-glyph-pixel scale at GUI 4: fractional GUI widths.
        int arrow = 4;
        float scale = .75f;
        ToIntFunction<FormattedText> measure = text -> text.getString().length() * 6;
        for (Component mark : new Component[]{null, Component.literal("…")}) {
            var lefts = new java.util.HashSet<Float>();
            for (String label : new String[]{"9 / 14", "10 / 14", "1 / 2", "12 / 51"}) {
                int slot = measure.applyAsInt(Component.literal(label));
                var footer = new ClipboardPageFooter(Component.literal(label), mark, true, true, slot, scale);
                // The box content is exactly as wide as the footer; the group is right-aligned in it.
                float boxContent = footer.measuredWidth(measure, arrow);
                float groupLeft = boxContent - ClipboardPageFooter.groupWidth(arrow, slot) * scale;
                lefts.add(groupLeft);
                assertEquals(Math.rint(boxContent * 4), boxContent * 4, 1e-4, "whole screen pixels at GUI 4");
            }
            assertEquals(1, lefts.size(), "the left arrow keeps its place: " + lefts);
        }
    }

    @Test
    void checkedBoxKeepsAFadedBoxUnderTheCheckMarkLikeCreate() {
        // Create: box 0xFF8D7F6B / 0x668D7F6B at y + 51, check 0x31B25D over it at y + 50.
        assertEquals(0xFF, ClipboardCheckbox.boxColor(false) >>> 24);
        assertEquals(0x66, ClipboardCheckbox.boxColor(true) >>> 24);
        assertEquals(ClipboardCheckbox.boxColor(false) & 0xFFFFFF, ClipboardCheckbox.boxColor(true) & 0xFFFFFF);
        assertEquals(0x31B25D, ClipboardCheckbox.checkColor() & 0xFFFFFF);
        assertEquals(1, ClipboardCheckbox.boxYOffset());
        assertTrue(EntryMarker.CHECKED_BOX.checked());
        assertFalse(EntryMarker.OPEN_BOX.checked());
        assertEquals(EntryMarker.CHECK_GLYPH, EntryMarker.CHECKED_BOX.glyph());
        assertEquals(EntryMarker.BOX_GLYPH, EntryMarker.OPEN_BOX.glyph());
    }

    @Test
    void paddedBoxStaysInsideTheWindowAtTheRightEdge() {
        assertTrue(ClipboardHudPadding.top() > 0 && ClipboardHudPadding.left() > 0 && ClipboardHudPadding.right() > 0);
        assertEquals(ClipboardHudPadding.left() + ClipboardHudPadding.right(), ClipboardHudPadding.horizontal());
        for (int screen : new int[]{240, 480, 960}) {
            for (int text : new int[]{40, 160, screen - 32 - ClipboardHudPadding.horizontal()}) {
                int box = text + ClipboardHudPadding.horizontal();
                int anchor = GoggleHudGeometry.anchorX(screen, box, 20);
                // Outer background edge: anchor + 12 + box + 4.
                assertTrue(anchor + 12 + box + 4 <= screen, () -> screen + "/" + text);
            }
        }
    }

    @Test
    void boxWithoutAPageFooterGetsItsOwnBottomPadding() {
        var body = EntryLine.row(Component.literal("한 페이지"), EntryMarker.OPEN_BOX, 12, false, WHITE);
        List<HudLine> single = List.of(body, body);
        List<HudLine> multi = List.of(body, body,
                new ClipboardPageFooter(Component.literal("1 / 3"), null, false, true, 30, .75f));
        List<HudLine> status = List.of(new PlainLine(Component.literal("status")));
        assertFalse(ClipboardHudBox.hasFooter(single));
        assertFalse(ClipboardHudBox.hasFooter(status));
        assertTrue(ClipboardHudBox.hasFooter(multi));
        assertTrue(ClipboardHudBox.bottomWithoutFooter() > 0);
        assertEquals(ClipboardHudSpacing.height(single) + ClipboardHudPadding.top() + ClipboardHudBox.bottomWithoutFooter(),
                ClipboardHudBox.height(single));
        assertEquals(ClipboardHudSpacing.height(status) + ClipboardHudPadding.top() + ClipboardHudBox.bottomWithoutFooter(),
                ClipboardHudBox.height(status));
        // The footer row and its gap already leave room below the body; no extra padding is stacked on it.
        assertEquals(ClipboardHudSpacing.height(multi) + ClipboardHudPadding.top(), ClipboardHudBox.height(multi));
    }

    @Test
    void checkboxAndAddressIconShareACenteredMarkerSlot() {
        assertEquals(5, ClipboardMarkerSlot.glyphWidth(6));
        assertEquals(0, ClipboardMarkerSlot.glyphWidth(0));
        // Glyph narrower or wider than the 8px Create address icon, odd and even differences.
        for (int box : new int[]{4, 5, 6, 7, 8, 9, 11}) {
            int icon = 8;
            int slot = ClipboardMarkerSlot.width(box, icon);
            int boxOffset = ClipboardMarkerSlot.offset(slot, box);
            int iconOffset = ClipboardMarkerSlot.offset(slot, icon);
            assertTrue(boxOffset == 0 || iconOffset == 0, "the wider marker starts the slot");
            assertTrue(boxOffset + box <= slot && iconOffset + icon <= slot);
            // Centers differ only by integer pixel rounding.
            assertTrue(Math.abs((2 * boxOffset + box) - (2 * iconOffset + icon)) <= 1, () -> "box " + box);
        }
    }

    @Test
    void bodyWrapsAtCreateClipboardWidthRegardlessOfScreenSize() {
        // Create 6.0.10 ClipboardScreen: font.split(text, 150 - iconOffset), iconOffset 16 for item rows.
        for (int available : new int[]{150, 600, 1900, Integer.MAX_VALUE}) {
            assertEquals(150, ClipboardWrapWidth.of(false, available));
            assertEquals(134, ClipboardWrapWidth.of(true, available));
        }
        // Only a window narrower than the paper wraps earlier, so the panel still fits on screen.
        assertEquals(100, ClipboardWrapWidth.of(false, 100));
        assertEquals(100, ClipboardWrapWidth.of(true, 100));
        assertEquals(1, ClipboardWrapWidth.of(false, 0));
        assertEquals(1, ClipboardWrapWidth.of(true, -20));
    }

    @Test
    void bodyHasUniformLineSpacingAndTheSmallerPageFooterIsIncludedInPanelWidth() {
        for (int i = 1; i < 12; i++) {
            assertEquals(10, GoggleHudGeometry.lineY(i) - GoggleHudGeometry.lineY(i - 1));
        }
        float scale = ClipboardPageStyle.scale();
        // Body rows are drawn at full size.
        assertTrue(scale < 1);
        assertTrue(scale < .75f);
        assertTrue(ClipboardPageStyle.color() < ChatFormatting.GRAY.getColor(), "darker than the body labels");
        var footer = new ClipboardPageFooter(Component.literal("1 / 12"), null, false, true, 21, scale);
        assertEquals(ClipboardPageFooter.groupWidth(4, 21) * scale, footer.measuredWidth(line -> 21, 4), 1e-4);
    }

    @Test
    void singlePageHidesTheFooterButClippedTextAndMultiplePagesRemainDistinguishable() {
        assertNull(ClipboardHudFooter.text(1, 0, false));
        assertNull(ClipboardHudFooter.text(1, 1, false));
        var clippedSingle = ClipboardHudFooter.text(1, 1, true);
        assertEquals("\u2026", clippedSingle.getString());
        assertEquals(ClipboardPageStyle.color(), clippedSingle.getStyle().getColor().getValue());
        var multiple = ClipboardHudFooter.text(1, 3, false);
        var contents = assertInstanceOf(TranslatableContents.class, multiple.getContents());
        assertEquals("create_clipboard_hud.preview.page", contents.getKey());
        assertArrayEquals(new Object[]{1, 3}, contents.getArgs());
        assertTrue(multiple.getSiblings().isEmpty());
        var clippedMultiple = ClipboardHudFooter.text(1, 3, true);
        assertEquals(" \u2026", clippedMultiple.getSiblings().get(clippedMultiple.getSiblings().size() - 1).getString());
        var body = EntryLine.row(Component.literal("본문"), EntryMarker.NONE, 12, false, WHITE);
        assertEquals(8, ClipboardHudSpacing.height(List.of(body)));
        assertEquals(22, ClipboardHudSpacing.height(List.<HudLine>of(body, new ClipboardPageFooter(null, clippedSingle, false, false, 0, .75f))));
    }

    @Test
    void addressPresentationMatchesCreateWithoutChangingRawTextOrCheckState() {
        VanillaBootstrap.ensure();
        var source = new ClipboardEntry(true, Component.literal("#   공장 A  "));
        var snapshot = new ClipboardEntrySnapshot(source);
        var text = snapshot.text();
        assertTrue(ClipboardAddress.isAddress(text));
        assertEquals("공장 A  ", ClipboardAddress.displayText(text).getString());
        assertEquals("#   공장 A  ", source.text.getString());
        assertEquals(source.text.getString(), text.getString());
        assertEquals(source.text.getString(), snapshot.text().getString());
        assertTrue(source.checked);
        assertTrue(snapshot.checked());
        var shown = ClipboardAddress.displayText(text);
        var unchecked = EntryLine.row(shown, EntryMarker.first(true, false), 12, false, WHITE);
        var checked = EntryLine.row(shown, EntryMarker.first(true, true), 12, false, WHITE);
        // The address icon replaces the checkbox; its check state only picks the inactive icon.
        assertEquals(EntryMarker.ADDRESS, unchecked.marker());
        assertEquals(EntryMarker.INACTIVE_ADDRESS, checked.marker());
        assertNull(checked.marker().glyph());
        assertEquals(unchecked.textOffset(), checked.textOffset());
        assertEquals(12 + 40, checked.measuredWidth(line -> 40));
    }

    @Test
    void emptyHashAndNonPrefixHashesRemainOrdinaryClipboardText() {
        for (String raw : new String[]{"#", "# \t\n", " #주소", "본문 #주소"}) {
            var text = Component.literal(raw).withStyle(ChatFormatting.RED);
            assertFalse(ClipboardAddress.isAddress(text));
            assertSame(text, ClipboardAddress.displayText(text));
        }
        assertEquals("주소 #2", ClipboardAddress.displayText(Component.literal("#주소 #2")).getString());
    }

    @Test
    void footerGapLeavesBodyAndMaterialRowsUnchangedAndIsIncludedInWindowBounds() {
        List<HudLine> lines = List.of(
                EntryLine.row(Component.literal("아이템"), EntryMarker.OPEN_BOX, 12, true, WHITE),
                EntryLine.row(Component.literal("수량"), EntryMarker.NONE, 12, true, WHITE),
                new ClipboardPageFooter(Component.literal("1 / 2"), null, false, true, 30, .75f));
        assertEquals(0, ClipboardHudSpacing.lineY(lines, 0));
        assertEquals(10, ClipboardHudSpacing.lineY(lines, 1));
        assertEquals(24, ClipboardHudSpacing.lineY(lines, 2));
        assertEquals(32, ClipboardHudSpacing.height(lines));
        int anchor = ClipboardHudPlacement.centeredAnchorY(140, ClipboardHudSpacing.height(lines), Integer.MAX_VALUE);
        assertTrue(anchor - 12 + ClipboardHudSpacing.height(lines) + 4 <= 140, "the footer gap stays inside the window");
        List<HudLine> bodyOnly = lines.subList(0, 2);
        assertEquals(18, ClipboardHudSpacing.height(bodyOnly));
        assertEquals(10, ClipboardHudSpacing.lineY(bodyOnly, 1));
        List<HudLine> pageOnly = List.of(lines.get(lines.size() - 1));
        assertEquals(0, ClipboardHudSpacing.lineY(pageOnly, 0));
        assertEquals(8, ClipboardHudSpacing.height(pageOnly));
    }

    @Test
    void centeredHudUsesAvailableHeightAtDifferentGuiScalesAndPreservesThePageCenter() {
        assertEquals(31, ClipboardHudPlacement.maxLines(360));
        assertEquals(13, ClipboardHudPlacement.maxLines(180));
        assertTrue(ClipboardHudPlacement.maxLines(360) > 12);
        for (int screenHeight : new int[]{180, 270, 360}) {
            for (int textHeight : new int[]{32, 112}) {
                int anchor = ClipboardHudPlacement.centeredAnchorY(screenHeight, textHeight, 0);
                int top = anchor - 16;
                int bottom = anchor - 8 + textHeight;
                assertEquals(screenHeight / 2, (top + bottom) / 2);
                assertTrue(top >= 8);
                assertTrue(bottom <= screenHeight - 8);
            }
            int lines = ClipboardHudPlacement.maxLines(screenHeight);
            int fullHeight = GoggleHudGeometry.tooltipHeight(lines) + ClipboardHudSpacing.footerGap();
            int anchor = ClipboardHudPlacement.centeredAnchorY(screenHeight, fullHeight, 0);
            assertTrue(anchor - 16 >= 8);
            assertTrue(anchor - 8 + fullHeight <= screenHeight - 8);
        }
    }

    @Test
    void centeredHudOffsetsRemainBoundedAndDoNotOverflow() {
        assertEquals(24, ClipboardHudPlacement.centeredAnchorY(180, 112, Integer.MIN_VALUE));
        assertEquals(68, ClipboardHudPlacement.centeredAnchorY(180, 112, Integer.MAX_VALUE));
        assertEquals(136, ClipboardHudPlacement.centeredAnchorY(360, 112, 0));
        assertEquals(146, ClipboardHudPlacement.centeredAnchorY(360, 112, 10));
        assertEquals(0, ClipboardHudPlacement.maxLines(20));
    }

    @Test
    void extremeOffsetsCannotOverflowOrPushTheTooltipOutsideTheWindow() {
        assertEquals(-8, GoggleHudGeometry.anchorX(320, 100, Integer.MIN_VALUE));
        assertEquals(200, GoggleHudGeometry.anchorX(320, 100, Integer.MAX_VALUE));
    }

    @Test
    void completedBodyUsesCreateGreenIncludingMaterialQuantityWithoutChangingSourceStyles() {
        VanillaBootstrap.ensure();
        var quantity = Component.literal(" x128").withStyle(ChatFormatting.BLACK);
        var red = Component.literal(" red").withStyle(ChatFormatting.RED);
        var source = Component.literal("한글 English").withStyle(ChatFormatting.BOLD).append(quantity).append(red);
        var original = new ClipboardEntry(true, source);
        var snapshot = new ClipboardEntrySnapshot(original);
        var completed = ClipboardEntryAppearance.prepare(snapshot.text(), true, false);
        var open = ClipboardEntryAppearance.prepare(snapshot.text(), false, false);
        var completeColors = new java.util.ArrayList<Integer>();
        var openColors = new java.util.ArrayList<Integer>();
        completed.visit((style, text) -> {
            if (!text.isEmpty()) { completeColors.add(style.getColor().getValue()); assertTrue(style.isBold()); }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        open.visit((style, text) -> {
            if (!text.isEmpty()) openColors.add(style.getColor().getValue());
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        assertEquals(List.of(0x31B25D, 0x31B25D, ChatFormatting.RED.getColor()), completeColors);
        assertEquals(List.of(0xFFFFFF, 0xFFFFFF, ChatFormatting.RED.getColor()), openColors);
        assertEquals(source.getString(), completed.getString());
        assertNull(source.getStyle().getColor());
        assertEquals(ChatFormatting.BLACK.getColor(), quantity.getStyle().getColor().getValue());
        assertEquals(ChatFormatting.BLACK.getColor(), snapshot.text().getSiblings().get(0).getStyle().getColor().getValue());
        assertTrue(original.checked);
        assertTrue(snapshot.checked());
    }

    @Test
    void inactiveAddressUsesCreateColorAndAlphaAcrossWrappedTextAndKeepsPageAppearanceSeparate() {
        var raw = Component.literal("#  긴 주소 Long address");
        var shown = ClipboardAddress.displayText(raw);
        var inactive = ClipboardEntryAppearance.prepare(shown, true, true);
        inactive.visit((style, text) -> {
            if (!text.isEmpty()) assertEquals(0x8D7F6B, style.getColor().getValue());
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        assertEquals(0x668D7F6B, ClipboardEntryAppearance.color(true, true));
        assertEquals(0xFF31B25D, ClipboardEntryAppearance.color(true, false));
        assertEquals(0xFFFFFFFF, ClipboardEntryAppearance.color(false, true));
        assertEquals("#  긴 주소 Long address", raw.getString());
        assertNull(raw.getStyle().getColor());
        int color = ClipboardEntryAppearance.color(true, true);
        var wrappedFirst = EntryLine.row(inactive, EntryMarker.first(true, true), 12, false, color);
        var wrappedNext = EntryLine.row(Component.literal("이어지는 주소"), EntryMarker.NONE, 12, false, color);
        assertTrue(wrappedFirst.marker().isAddress());
        assertEquals(EntryMarker.NONE, wrappedNext.marker());
        assertEquals(wrappedFirst.color(), wrappedNext.color());
        assertEquals(wrappedFirst.textOffset(), wrappedNext.textOffset());
        var footer = new ClipboardPageFooter(ClipboardHudFooter.text(1, 2, false), null, false, true, 30, .75f);
        var lines = List.<HudLine>of(wrappedFirst, wrappedNext, footer);
        assertEquals(10, ClipboardHudSpacing.lineY(lines, 1));
        assertEquals(24, ClipboardHudSpacing.lineY(lines, 2));
        assertEquals(32, ClipboardHudSpacing.height(lines));
        assertEquals(ClipboardPageStyle.color(), footer.label().getStyle().getColor().getValue());
    }
}
