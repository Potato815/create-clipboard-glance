package dev.clipboardhud.create;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateClipboardReaderTest {
    private final HolderLookup.Provider registries = HolderLookup.Provider.create(BuiltInRegistries.REGISTRY
            .stream().map(registry -> registry.asLookup()));

    @Test
    void missingDataIsNotDeclaredEmptyAndDoesNotKeepOldContent() {
        var reader = new CreateClipboardReader();
        var ready = reader.read(components(List.of(List.of(entry()))), registries);
        assertEquals(CreateClipboardReader.Status.READY, ready.status());
        assertTrue(ready.hasContent());
        var unavailable = reader.read(DataComponentMap.EMPTY, registries);
        assertEquals(CreateClipboardReader.Status.DATA_UNAVAILABLE, unavailable.status());
        assertTrue(unavailable.document().isEmpty());
        assertFalse(unavailable.hasContent());
        assertFalse(CreateClipboardReader.Result.error().hasContent());
    }

    @Test
    void confirmedEmptyContentPreservesBlankPages() {
        var result = new CreateClipboardReader().read(components(List.of(List.of(), List.of())), registries);
        assertEquals(CreateClipboardReader.Status.EMPTY, result.status());
        assertFalse(result.hasContent());
        assertEquals(2, result.document().orElseThrow().pages().size());
    }

    @Test
    void unchangedContentReusesSnapshotButEntryEditInvalidatesIt() {
        var reader = new CreateClipboardReader();
        var original = entry().displayItem(new ItemStack(Items.IRON_INGOT), 128);
        var components = components(List.of(List.of(original)));
        var before = reader.read(components, registries);
        assertSame(before, reader.read(components, registries));
        original.checked = true; // Simulates a mutable Create entry, even with the same component/list identity.
        var after = reader.read(components, registries);
        assertNotSame(before, after);
        assertFalse(before.document().orElseThrow().pages().getFirst().getFirst().checked());
        assertTrue(after.document().orElseThrow().pages().getFirst().getFirst().checked());
    }

    @Test
    void snapshotDetachesPagesNestedTextAndIconComponentsInBothDirections() {
        var suffix = Component.literal(" nested");
        var iconName = Component.literal("named item");
        var icon = new ItemStack(Items.IRON_INGOT, 2);
        icon.set(DataComponents.CUSTOM_NAME, iconName);
        var original = new ClipboardEntry(false, Component.literal("원문").append(suffix)).displayItem(icon, 128);
        var entries = new ArrayList<>(List.of(original));
        var pages = new ArrayList<List<ClipboardEntry>>(List.of(entries));
        var snapshot = new ClipboardDocumentSnapshot(pages, registries);
        var saved = snapshot.pages().getFirst().getFirst();
        suffix.append(" changed");
        iconName.append(" changed");
        icon.setCount(9);
        entries.clear();
        pages.clear();
        assertEquals("원문 nested", saved.text().getString());
        assertEquals(2, saved.icon().getCount());
        assertEquals("named item", saved.icon().get(DataComponents.CUSTOM_NAME).getString());
        var exposedText = saved.text();
        ((net.minecraft.network.chat.MutableComponent) exposedText.getSiblings().getFirst()).append(" caller");
        var exposedIcon = saved.icon();
        ((net.minecraft.network.chat.MutableComponent) exposedIcon.get(DataComponents.CUSTOM_NAME)).append(" caller");
        assertEquals("원문 nested", saved.text().getString());
        assertEquals("named item", saved.icon().get(DataComponents.CUSTOM_NAME).getString());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.pages().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.pages().getFirst().clear());
    }

    @Test
    void readDoesNotChangeOriginalTextChecksIconsOrSharedPage() {
        var original = entry().displayItem(new ItemStack(Items.IRON_INGOT), 128);
        var content = new ClipboardContent(ClipboardType.WRITTEN, List.of(List.of(original)), true, 3, Optional.empty());
        var components = DataComponentMap.builder().set(AllDataComponents.CLIPBOARD_CONTENT, content).build();
        var originalText = Component.Serializer.toJson(original.text, registries);
        new CreateClipboardReader().read(components, registries);
        assertSame(content, components.get(AllDataComponents.CLIPBOARD_CONTENT));
        assertSame(original, content.pages().getFirst().getFirst());
        assertEquals(3, content.previouslyOpenedPage());
        assertTrue(content.readOnly());
        assertFalse(original.checked);
        assertEquals(originalText, Component.Serializer.toJson(original.text, registries));
        assertEquals(1, original.icon.getCount());
        assertEquals(128, original.itemAmount);
    }

    @Test
    void orderCheckStyleAndItemComponentsArePartOfChangeDetection() {
        var first = entry();
        var second = new ClipboardEntry(true, Component.literal("second"));
        var pages = List.of(List.of(first, second));
        var snapshot = new ClipboardDocumentSnapshot(pages, registries);
        assertTrue(snapshot.matches(pages));
        assertFalse(snapshot.matches(List.of(List.of(second, first))));
        first.checked = true;
        assertFalse(snapshot.matches(pages));
        first.checked = false;
        first.text.withStyle(ChatFormatting.RED);
        assertFalse(snapshot.matches(pages));
        first.text.setStyle(net.minecraft.network.chat.Style.EMPTY);
        first.displayItem(new ItemStack(Items.IRON_INGOT), 1);
        var withIcon = new ClipboardDocumentSnapshot(pages, registries);
        first.icon.set(DataComponents.CUSTOM_NAME, Component.literal("custom"));
        assertFalse(withIcon.matches(pages));
    }

    @Test
    void addressTextAndStoredPageOrderAreKeptExactly() {
        var snapshot = new ClipboardDocumentSnapshot(List.of(List.of(entry()), List.of()), registries);
        assertEquals("# 배송 주소", snapshot.pages().getFirst().getFirst().text().getString());
        assertTrue(snapshot.pages().get(1).isEmpty());
    }

    @Test
    void actualCreateMaterialChecklistKeepsTextAndReusesSnapshot() {
        var checklist = new MaterialChecklist();
        checklist.required.put(Items.IRON_INGOT, 128);
        var clipboard = checklist.createWrittenClipboard();
        var content = clipboard.get(AllDataComponents.CLIPBOARD_CONTENT);
        var reader = new CreateClipboardReader();
        var result = reader.read(clipboard.getComponents(), registries);
        assertEquals(CreateClipboardReader.Status.READY, result.status());
        var snapshot = result.document().orElseThrow();
        assertTrue(snapshot.matches(content.pages()));
        assertSame(result, reader.read(clipboard.getComponents(), registries));
        var saved = snapshot.pages().getFirst().getFirst();
        assertEquals(Component.Serializer.toJson(content.pages().getFirst().getFirst().text, registries),
                Component.Serializer.toJson(saved.text(), registries));
    }

    private ClipboardEntry entry() { return new ClipboardEntry(false, Component.literal("# 배송 주소")); }

    private DataComponentMap components(List<List<ClipboardEntry>> pages) {
        return DataComponentMap.builder().set(AllDataComponents.CLIPBOARD_CONTENT,
                new ClipboardContent(ClipboardType.WRITTEN, pages, false)).build();
    }
}
