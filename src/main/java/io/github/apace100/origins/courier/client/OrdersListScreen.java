package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.courier.ClientOrder;
import io.github.apace100.origins.courier.CourierNetworking;
import io.github.apace100.origins.courier.Order;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Экран списка заказов для курьеров
 */
public class OrdersListScreen extends Screen {
    
    private OrderListWidget orderList;
    private ButtonWidget refreshButton;
    private ButtonWidget closeButton;
    private ButtonWidget createOrderButton;
    
    // Вкладки
    private ButtonWidget allOrdersTab;
    private ButtonWidget openOrdersTab;
    private ButtonWidget activeOrdersTab;
    private ButtonWidget completedOrdersTab;
    
    private final List<ClientOrder> allOrders = new ArrayList<>();
    private final List<ClientOrder> filteredOrders = new ArrayList<>();
    private final UUID highlightOrderId;
    private boolean isLoading = false;
    private int animationTicks = 0;
    
    // Текущая вкладка
    private OrderTab currentTab = OrderTab.ALL;
    
    public enum OrderTab {
        ALL("Все"),
        OPEN("Открытые"),
        ACTIVE("Принятые"),
        COMPLETED("Завершенные");
        
        private final String displayName;
        
        OrderTab(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public OrdersListScreen() {
        this(null);
    }
    
    public OrdersListScreen(UUID highlightOrderId) {
        super(Text.literal("Список заказов"));
        this.highlightOrderId = highlightOrderId;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = CourierNetworking.ORDERS_LIST_WIDTH;
        int screenHeight = CourierNetworking.ORDERS_LIST_HEIGHT;
        
        int left = centerX - screenWidth / 2;
        int top = centerY - screenHeight / 2;
        
        // Вкладки
        int tabWidth = 80;
        int tabY = top + 25;
        
        allOrdersTab = ButtonWidget.builder(Text.literal(OrderTab.ALL.getDisplayName()), 
            button -> switchTab(OrderTab.ALL))
            .dimensions(left + 10, tabY, tabWidth, 20)
            .build();
        this.addDrawableChild(allOrdersTab);
        
        openOrdersTab = ButtonWidget.builder(Text.literal(OrderTab.OPEN.getDisplayName()), 
            button -> switchTab(OrderTab.OPEN))
            .dimensions(left + 100, tabY, tabWidth, 20)
            .build();
        this.addDrawableChild(openOrdersTab);
        
        activeOrdersTab = ButtonWidget.builder(Text.literal(OrderTab.ACTIVE.getDisplayName()), 
            button -> switchTab(OrderTab.ACTIVE))
            .dimensions(left + 190, tabY, tabWidth, 20)
            .build();
        this.addDrawableChild(activeOrdersTab);
        
        completedOrdersTab = ButtonWidget.builder(Text.literal(OrderTab.COMPLETED.getDisplayName()), 
            button -> switchTab(OrderTab.COMPLETED))
            .dimensions(left + 280, tabY, tabWidth, 20)
            .build();
        this.addDrawableChild(completedOrdersTab);
        
        // Список заказов (сдвигаем вниз из-за вкладок)
        orderList = new OrderListWidget(this.client, screenWidth - 20, screenHeight - 110, 
                                       top + 55, top + screenHeight - 55, 60);
        orderList.setLeftPos(left + 10);
        this.addSelectableChild(orderList);
        
        // Кнопки
        refreshButton = ButtonWidget.builder(Text.literal("Обновить"), button -> requestSync())
            .dimensions(left + 10, top + screenHeight - 35, 80, 20)
            .build();
        this.addDrawableChild(refreshButton);
        
        // Кнопка создания заказа (только для не-курьеров)
        if (this.client != null && this.client.player != null) {
            // Здесь мы не можем проверить класс игрока на клиенте, поэтому показываем кнопку всем
            createOrderButton = ButtonWidget.builder(Text.literal("Создать заказ"), 
                button -> MinecraftClient.getInstance().setScreen(new CreateOrderScreen()))
                .dimensions(left + 100, top + screenHeight - 35, 120, 20)
                .build();
            this.addDrawableChild(createOrderButton);
        }
        
        closeButton = ButtonWidget.builder(Text.literal("Закрыть"), button -> this.close())
            .dimensions(left + screenWidth - 90, top + screenHeight - 35, 80, 20)
            .build();
        this.addDrawableChild(closeButton);
        
        // Уведомляем ClientOrderManager о том, что экран открыт
        ClientOrderManager.getInstance().setCurrentOrdersScreen(this);
        
        // Очищаем старые данные
        allOrders.clear();
        filteredOrders.clear();
        
        // Запрашиваем синхронизацию при открытии
        requestSync();
    }
    
    @Override
    public void tick() {
        super.tick();
        animationTicks++;
    }
    
    /**
     * Запрашивает синхронизацию заказов с сервером
     */
    private void requestSync() {
        if (!isLoading) {
            isLoading = true;
            refreshButton.active = false;
            
            PacketByteBuf buf = PacketByteBufs.create();
            ClientPlayNetworking.send(CourierNetworking.REQUEST_ORDERS_SYNC, buf);
        }
    }
    
    /**
     * Переключает вкладку
     */
    private void switchTab(OrderTab tab) {
        currentTab = tab;
        updateTabButtons();
        filterOrders();
    }
    
    /**
     * Обновляет состояние кнопок вкладок
     */
    private void updateTabButtons() {
        allOrdersTab.active = currentTab != OrderTab.ALL;
        openOrdersTab.active = currentTab != OrderTab.OPEN;
        activeOrdersTab.active = currentTab != OrderTab.ACTIVE;
        completedOrdersTab.active = currentTab != OrderTab.COMPLETED;
    }
    
    /**
     * Фильтрует заказы по текущей вкладке
     */
    private void filterOrders() {
        filteredOrders.clear();
        
        System.out.println("DEBUG: Filtering orders for tab: " + currentTab + ", total orders: " + allOrders.size());
        
        switch (currentTab) {
            case ALL:
                filteredOrders.addAll(allOrders);
                System.out.println("DEBUG: ALL tab - showing " + filteredOrders.size() + " orders");
                break;
            case OPEN:
                for (ClientOrder order : allOrders) {
                    // Открытые заказы: только со статусом OPEN
                    if (order.status == Order.Status.OPEN) {
                        filteredOrders.add(order);
                    }
                }
                System.out.println("DEBUG: OPEN tab - showing " + filteredOrders.size() + " orders");
                break;
            case ACTIVE:
                for (ClientOrder order : allOrders) {
                    System.out.println("DEBUG: Order " + order.id + " status: " + order.status);
                    // Принятые заказы: только принятые и выполняющиеся
                    if (order.status == Order.Status.ACCEPTED || 
                        order.status == Order.Status.IN_PROGRESS) {
                        filteredOrders.add(order);
                    }
                }
                System.out.println("DEBUG: ACTIVE tab - showing " + filteredOrders.size() + " orders");
                break;
            case COMPLETED:
                for (ClientOrder order : allOrders) {
                    // Завершенные заказы: выполненные + отклоненные + отмененные
                    if (order.status == Order.Status.COMPLETED || 
                        order.status == Order.Status.DECLINED || 
                        order.status == Order.Status.CANCELLED) {
                        filteredOrders.add(order);
                    }
                }
                System.out.println("DEBUG: COMPLETED tab - showing " + filteredOrders.size() + " orders");
                break;
        }
        
        // Обновляем список
        orderList.clearAllEntries();
        for (ClientOrder order : filteredOrders) {
            orderList.addOrderEntry(new OrderEntry(order));
        }
    }
    
    /**
     * Обновляет список заказов (вызывается из ClientOrderManager)
     */
    public void updateOrders(List<ClientOrder> newOrders) {
        allOrders.clear();
        allOrders.addAll(newOrders);
        
        // Обновляем вкладки и фильтруем заказы
        updateTabButtons();
        filterOrders();
        
        // Если нужно выделить конкретный заказ
        if (highlightOrderId != null) {
            for (ClientOrder order : allOrders) {
                if (order.id.equals(highlightOrderId)) {
                    MinecraftClient.getInstance().setScreen(new OrderDetailsScreen(order));
                    break;
                }
            }
        }
        
        isLoading = false;
        refreshButton.active = true;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим фон
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = CourierNetworking.ORDERS_LIST_WIDTH;
        int screenHeight = CourierNetworking.ORDERS_LIST_HEIGHT;
        
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
        
        // Информация о количестве заказов
        String orderInfo = String.format("%s: %d", currentTab.getDisplayName(), filteredOrders.size());
        context.drawText(this.textRenderer, orderInfo, left + 10, top + 50, CourierNetworking.COLOR_TEXT_SECONDARY, false);
        
        // Рендерим список заказов
        orderList.render(context, mouseX, mouseY, delta);
        
        // Рендерим кнопки
        super.render(context, mouseX, mouseY, delta);
        
        // Индикатор загрузки
        if (isLoading) {
            String loadingText = "Загрузка...";
            int loadingX = centerX - this.textRenderer.getWidth(loadingText) / 2;
            int loadingY = centerY;
            context.fill(loadingX - 5, loadingY - 5, loadingX + this.textRenderer.getWidth(loadingText) + 5, 
                        loadingY + 15, 0x80000000);
            context.drawText(this.textRenderer, loadingText, loadingX, loadingY, CourierNetworking.COLOR_ACCENT, false);
        }
        
        // Если заказов нет
        if (filteredOrders.isEmpty() && !isLoading) {
            String emptyText = "Нет доступных заказов";
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal(emptyText).formatted(Formatting.GRAY), 
                centerX, centerY, CourierNetworking.COLOR_TEXT_SECONDARY);
        }
    }
    
    @Override
    public void close() {
        // Уведомляем ClientOrderManager о закрытии экрана
        ClientOrderManager.getInstance().closeOrdersScreen();
        super.close();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        System.out.println("DEBUG: OrdersListScreen mouseClicked - coords: " + mouseX + "," + mouseY + ", button: " + button);
        
        // Сначала проверяем список заказов
        if (orderList.mouseClicked(mouseX, mouseY, button)) {
            System.out.println("DEBUG: Click handled by order list");
            return true;
        }
        
        System.out.println("DEBUG: Click not handled by order list, passing to super");
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC для закрытия
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            this.close();
            return true;
        }
        
        // F5 для обновления
        if (keyCode == 294) { // GLFW_KEY_F5
            requestSync();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Виджет списка заказов
     */
    private class OrderListWidget extends ElementListWidget<OrderEntry> {
        
        public OrderListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }
        
        @Override
        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 20;
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            System.out.println("DEBUG: OrderListWidget mouseClicked - coords: " + mouseX + "," + mouseY + ", button: " + button);
            
            // Проверяем каждый entry вручную
            for (int i = 0; i < this.children().size(); i++) {
                OrderEntry entry = this.children().get(i);
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    System.out.println("DEBUG: Entry " + i + " handled the click");
                    return true;
                }
            }
            
            boolean result = super.mouseClicked(mouseX, mouseY, button);
            System.out.println("DEBUG: OrderListWidget super result: " + result);
            return result;
        }
        
        public void clearAllEntries() {
            this.clearEntries();
        }
        
        public void addOrderEntry(OrderEntry entry) {
            this.addEntry(entry);
        }
    }
    
    /**
     * Элемент списка заказов
     */
    private class OrderEntry extends ElementListWidget.Entry<OrderEntry> {
        private final ClientOrder order;
        private int buttonX, buttonY, buttonWidth, buttonHeight;
        
        public OrderEntry(ClientOrder order) {
            this.order = order;
            this.buttonWidth = 60;
            this.buttonHeight = 18;
            // Инициализируем координаты кнопки значениями по умолчанию
            this.buttonX = 0;
            this.buttonY = 0;
        }
        
        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, 
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {
            
            // Фон при наведении
            if (hovered) {
                context.fill(x, y, x + entryWidth, y + entryHeight, 0x40FFFFFF);
            }
            
            // Статус заказа (цветная полоска слева)
            context.fill(x, y, x + 3, y + entryHeight, order.getStatusColor());
            
            // Информация о заказчике
            String ownerText = "От: " + order.ownerName;
            context.drawText(OrdersListScreen.this.textRenderer, ownerText, x + 8, y + 2, CourierNetworking.COLOR_TEXT, false);
            
            // Краткое описание
            String description = order.getShortDescription();
            context.drawText(OrdersListScreen.this.textRenderer, description, x + 8, y + 14, CourierNetworking.COLOR_TEXT_SECONDARY, false);
            
            // Статус
            String statusText = order.getLocalizedStatus();
            context.drawText(OrdersListScreen.this.textRenderer, statusText, x + 8, y + 26, order.getStatusColor(), false);
            
            // Время создания
            String timeText = CourierNetworking.formatOrderTime(order.createdTime);
            context.drawText(OrdersListScreen.this.textRenderer, timeText, x + 8, y + 38, CourierNetworking.COLOR_TEXT_SECONDARY, false);
            
            // Первый предмет запроса
            ItemStack firstItem = order.getFirstRequestItem();
            if (!firstItem.isEmpty()) {
                context.drawItem(firstItem, x + entryWidth - 80, y + 8);
                
                // Количество предметов
                String itemCountText = String.format("x%d предм.", order.getRequestItemsCount());
                context.drawText(OrdersListScreen.this.textRenderer, itemCountText, 
                    x + entryWidth - 80, y + 28, CourierNetworking.COLOR_TEXT_SECONDARY, false);
            }
            
            // Обновляем позицию кнопки
            updateButtonPosition(x, y, entryWidth);
            
            // Кнопка "Открыть" - рисуем вручную
            
            // Фон кнопки
            boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                               mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
            int buttonColor = isHovered ? 0x80FFFFFF : 0x80000000;
            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonColor);
            context.drawBorder(buttonX, buttonY, buttonWidth, buttonHeight, CourierNetworking.COLOR_PRIMARY);
            
            // Текст кнопки
            String buttonText = "Открыть";
            int textX = buttonX + (buttonWidth - OrdersListScreen.this.textRenderer.getWidth(buttonText)) / 2;
            int textY = buttonY + (buttonHeight - 8) / 2;
            context.drawText(OrdersListScreen.this.textRenderer, buttonText, textX, textY, CourierNetworking.COLOR_TEXT, false);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            System.out.println("DEBUG: OrderEntry mouseClicked - order: " + order.id + ", button: " + button + ", coords: " + mouseX + "," + mouseY);
            System.out.println("DEBUG: Button bounds: x=" + buttonX + ", y=" + buttonY + ", w=" + buttonWidth + ", h=" + buttonHeight);
            
            // Если координаты кнопки не установлены, пропускаем
            if (buttonX == 0 && buttonY == 0) {
                System.out.println("DEBUG: Button position not set, skipping click");
                return false;
            }
            
            // Проверяем, попал ли клик в область кнопки
            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                System.out.println("DEBUG: Click is within button bounds, opening order details for order: " + order.id);
                MinecraftClient.getInstance().setScreen(new OrderDetailsScreen(this.order));
                return true;
            }
            
            System.out.println("DEBUG: Click outside button bounds");
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
        
        private void updateButtonPosition(int x, int y, int entryWidth) {
            buttonX = x + entryWidth - 65;
            buttonY = y + 40;
        }
    }
}