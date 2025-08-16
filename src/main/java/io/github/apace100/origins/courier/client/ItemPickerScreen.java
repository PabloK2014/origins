package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.courier.CourierNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Экран выбора предметов с поиском и поддержкой модов
 */
public class ItemPickerScreen extends Screen {
    
    private final Consumer<ItemStack> callback;
    private final Screen parentScreen;
    private TextFieldWidget searchField;
    private ItemListWidget itemList;
    private ButtonWidget selectButton;
    private ButtonWidget cancelButton;
    
    private List<ItemEntry> allItems = new ArrayList<>();
    private List<ItemEntry> filteredItems = new ArrayList<>();
    private ItemEntry selectedItem = null;
    private String lastSearchText = "";
    private int currentPage = 0;
    private int totalPages = 0;
    
    public ItemPickerScreen(Consumer<ItemStack> callback) {
        this(callback, null);
    }
    
    public ItemPickerScreen(Consumer<ItemStack> callback, Screen parentScreen) {
        super(Text.literal("Выберите предмет"));
        this.callback = callback;
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = CourierNetworking.ITEM_PICKER_WIDTH;
        int screenHeight = CourierNetworking.ITEM_PICKER_HEIGHT;
        
        int left = centerX - screenWidth / 2;
        int top = centerY - screenHeight / 2;
        
        // Поле поиска
        searchField = new TextFieldWidget(this.textRenderer, left + 10, top + 25, screenWidth - 20, 20, Text.literal("Поиск..."));
        searchField.setPlaceholder(Text.literal("Введите название предмета...").formatted(Formatting.GRAY));
        searchField.setChangedListener(this::onSearchTextChanged);
        this.addSelectableChild(searchField);
        
        // Список предметов
        itemList = new ItemListWidget(this.client, screenWidth - 20, screenHeight - 120, top + 55, top + screenHeight - 65, 22);
        itemList.setLeftPos(left + 10);
        this.addSelectableChild(itemList);
        
        // Кнопки
        selectButton = ButtonWidget.builder(Text.literal("Выбрать"), button -> {
            if (selectedItem != null) {
                selectItem();
            }
        })
            .dimensions(left + 10, top + screenHeight - 35, 100, 20)
            .build();
        selectButton.active = false;
        this.addDrawableChild(selectButton);
        
        cancelButton = ButtonWidget.builder(Text.literal("Отмена"), button -> {
            if (parentScreen != null) {
                this.client.setScreen(parentScreen);
            } else {
                this.close();
            }
        })
            .dimensions(left + screenWidth - 110, top + screenHeight - 35, 100, 20)
            .build();
        this.addDrawableChild(cancelButton);
        
        // Кнопки пагинации
        ButtonWidget prevButton = ButtonWidget.builder(Text.literal("◀"), button -> previousPage())
            .dimensions(left + screenWidth / 2 - 60, top + screenHeight - 35, 20, 20)
            .build();
        this.addDrawableChild(prevButton);
        
        ButtonWidget nextButton = ButtonWidget.builder(Text.literal("▶"), button -> nextPage())
            .dimensions(left + screenWidth / 2 + 40, top + screenHeight - 35, 20, 20)
            .build();
        this.addDrawableChild(nextButton);
        
        // Загружаем все предметы
        loadAllItems();
        updateFilteredItems();
        
        // Устанавливаем фокус на поле поиска
        this.setInitialFocus(searchField);
    }
    
    @Override
    public void tick() {
        super.tick();
        animationTicks++;
    }
    
    /**
     * Загружает все доступные предметы из реестра
     */
    private void loadAllItems() {
        allItems.clear();
        
        for (Identifier id : Registries.ITEM.getIds()) {
            try {
                Item item = Registries.ITEM.get(id);
                if (item != null) {
                    ItemStack stack = new ItemStack(item);
                    if (!stack.isEmpty()) {
                        String name = stack.getName().getString().toLowerCase();
                        String modId = id.getNamespace();
                        allItems.add(new ItemEntry(stack, name, modId));
                    }
                }
            } catch (Exception e) {
                // Игнорируем ошибки при загрузке отдельных предметов
            }
        }
        
        // Сортируем по названию
        allItems.sort(Comparator.comparing(entry -> entry.displayName));
    }
    
    /**
     * Обновляет отфильтрованный список предметов
     */
    private void updateFilteredItems() {
        String searchText = searchField.getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            filteredItems = allItems.stream()
                .filter(entry -> entry.displayName.contains(searchText) || 
                               entry.modId.contains(searchText))
                .collect(Collectors.toList());
        }
        
        // Обновляем пагинацию
        totalPages = Math.max(1, (filteredItems.size() + CourierNetworking.ITEMS_PER_PAGE - 1) / CourierNetworking.ITEMS_PER_PAGE);
        currentPage = Math.min(currentPage, totalPages - 1);
        
        // Обновляем список
        updateItemList();
    }
    
    /**
     * Обновляет отображаемый список предметов
     */
    private void updateItemList() {
        itemList.clearAllEntries();
        
        int startIndex = currentPage * CourierNetworking.ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + CourierNetworking.ITEMS_PER_PAGE, filteredItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            ItemEntry entry = filteredItems.get(i);
            itemList.addItemEntry(new ItemListEntry(entry));
        }
    }
    
    /**
     * Обработчик изменения текста поиска
     */
    private void onSearchTextChanged(String text) {
        if (!text.equals(lastSearchText)) {
            lastSearchText = text;
            currentPage = 0;
            selectedItem = null;
            selectButton.active = false;
            updateFilteredItems();
        }
    }
    
    /**
     * Переход на предыдущую страницу
     */
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            selectedItem = null;
            selectButton.active = false;
            updateItemList();
        }
    }
    
    /**
     * Переход на следующую страницу
     */
    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            selectedItem = null;
            selectButton.active = false;
            updateItemList();
        }
    }
    
    /**
     * Выбор предмета
     */
    private void selectItem() {
        if (selectedItem != null && callback != null) {
            System.out.println("DEBUG: ItemPickerScreen - selecting item: " + selectedItem.itemStack.getName().getString());
            callback.accept(selectedItem.itemStack.copy());
            System.out.println("DEBUG: ItemPickerScreen - callback executed, returning to parent screen");
            
            // Возвращаемся к родительскому экрану, если он есть
            if (parentScreen != null) {
                // Принудительно обновляем родительский экран
                this.client.execute(() -> {
                    this.client.setScreen(parentScreen);
                });
            } else {
                this.close();
            }
        } else {
            System.out.println("DEBUG: ItemPickerScreen - cannot select item. selectedItem: " + selectedItem + ", callback: " + callback);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим фон
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = CourierNetworking.ITEM_PICKER_WIDTH;
        int screenHeight = CourierNetworking.ITEM_PICKER_HEIGHT;
        
        int left = centerX - screenWidth / 2;
        int top = centerY - screenHeight / 2;
        
        // Рендерим фон окна с анимацией
        float animationProgress = Math.min(1.0f, animationTicks / 10.0f);
        int alpha = (int)(255 * animationProgress);
        int backgroundColor = (alpha << 24) | (CourierNetworking.COLOR_BACKGROUND & 0xFFFFFF);
        
        context.fill(left, top, left + screenWidth, top + screenHeight, backgroundColor);
        context.drawBorder(left, top, screenWidth, screenHeight, CourierNetworking.COLOR_PRIMARY);
        
        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, top + 8, CourierNetworking.COLOR_TEXT);
        
        // Информация о пагинации
        if (totalPages > 1) {
            String pageInfo = String.format("Страница %d из %d (%d предметов)", 
                                           currentPage + 1, totalPages, filteredItems.size());
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(pageInfo), 
                                             centerX, top + screenHeight - 55, CourierNetworking.COLOR_TEXT_SECONDARY);
        } else if (!filteredItems.isEmpty()) {
            String itemInfo = String.format("Найдено предметов: %d", filteredItems.size());
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(itemInfo), 
                                             centerX, top + screenHeight - 55, CourierNetworking.COLOR_TEXT_SECONDARY);
        }
        
        // Рендерим поле поиска
        searchField.render(context, mouseX, mouseY, delta);
        
        // Рендерим список предметов
        itemList.render(context, mouseX, mouseY, delta);
        
        // Рендерим кнопки
        super.render(context, mouseX, mouseY, delta);
        
        // Если ничего не найдено
        if (filteredItems.isEmpty() && !searchField.getText().trim().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("Предметы не найдены").formatted(Formatting.GRAY), 
                centerX, centerY, CourierNetworking.COLOR_TEXT_SECONDARY);
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC для закрытия
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            if (parentScreen != null) {
                this.client.setScreen(parentScreen);
            } else {
                this.close();
            }
            return true;
        }
        
        // Enter для выбора
        if (keyCode == 257 && selectedItem != null) { // GLFW_KEY_ENTER
            selectItem();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Класс для хранения информации о предмете
     */
    private static class ItemEntry {
        public final ItemStack itemStack;
        public final String displayName;
        public final String modId;
        
        public ItemEntry(ItemStack itemStack, String displayName, String modId) {
            this.itemStack = itemStack;
            this.displayName = displayName;
            this.modId = modId;
        }
    }
    
    /**
     * Виджет списка предметов
     */
    private class ItemListWidget extends ElementListWidget<ItemListEntry> {
        
        public ItemListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }
        
        @Override
        protected int getScrollbarPositionX() {
            return this.getRowLeft() + this.getRowWidth() - 6;
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        public void clearAllEntries() {
            this.clearEntries();
        }
        
        public void addItemEntry(ItemListEntry entry) {
            this.addEntry(entry);
        }
    }
    
    /**
     * Элемент списка предметов
     */
    private class ItemListEntry extends ElementListWidget.Entry<ItemListEntry> {
        private final ItemEntry itemEntry;
        
        public ItemListEntry(ItemEntry itemEntry) {
            this.itemEntry = itemEntry;
        }
        
        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, 
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {
            
            // Фон при наведении или выборе
            if (selectedItem == itemEntry) {
                // Выбранный предмет - яркое выделение
                context.fill(x, y, x + entryWidth, y + entryHeight, 0xFF4A90E2);
                context.drawBorder(x, y, entryWidth, entryHeight, 0xFFFFFFFF);
            } else if (hovered) {
                // Наведение - слабое выделение
                context.fill(x, y, x + entryWidth, y + entryHeight, 0x40FFFFFF);
            }
            
            // Иконка предмета
            context.getMatrices().push();
            context.getMatrices().translate(x + 2, y + 2, 0);
            context.drawItem(itemEntry.itemStack, 0, 0);
            context.getMatrices().pop();
            
            // Название предмета
            String itemName = itemEntry.itemStack.getName().getString();
            int textColor = selectedItem == itemEntry ? 0xFFFFFF : CourierNetworking.COLOR_TEXT;
            context.drawText(ItemPickerScreen.this.textRenderer, itemName, x + 22, y + 2, textColor, false);
            
            // Мод
            String modName = itemEntry.modId;
            int modColor = selectedItem == itemEntry ? 0xCCCCCC : CourierNetworking.COLOR_TEXT_SECONDARY;
            context.drawText(ItemPickerScreen.this.textRenderer, 
                           Text.literal("[" + modName + "]").formatted(Formatting.GRAY), 
                           x + 22, y + 12, modColor, false);
            
            // Индикатор выбора (галочка)
            if (selectedItem == itemEntry) {
                context.drawText(ItemPickerScreen.this.textRenderer, "✓", 
                               x + entryWidth - 20, y + 6, 0xFF00FF00, false);
            }
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) { // Левая кнопка мыши
                // Проверяем двойной клик ПЕРЕД установкой выбора
                boolean isDoubleClick = (System.currentTimeMillis() - lastClickTime < 300) && (selectedItem == itemEntry);
                lastClickTime = System.currentTimeMillis();
                
                selectedItem = itemEntry;
                selectButton.active = true;
                
                // Двойной клик для быстрого выбора
                if (isDoubleClick) {
                    selectItem();
                }
                
                return true;
            }
            return false;
        }
        
        @Override
        public List<? extends net.minecraft.client.gui.Element> children() {
            return List.of();
        }
        
        @Override
        public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
            return List.of();
        }
    }
    
    private long lastClickTime = 0;
    private int animationTicks = 0;
}