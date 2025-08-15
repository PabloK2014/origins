package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.courier.ClientOrder;
import io.github.apace100.origins.courier.CourierNetworking;
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
    
    private final List<ClientOrder> orders = new ArrayList<>();
    private final UUID highlightOrderId;
    private boolean isLoading = false;
    private int animationTicks = 0;
    
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
        
        // Список заказов
        orderList = new OrderListWidget(this.client, screenWidth - 20, screenHeight - 80, 
                                       top + 30, top + screenHeight - 50, 60);
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
     * Обновляет список заказов (вызывается из ClientOrderManager)
     */
    public void updateOrders(List<ClientOrder> newOrders) {
        orders.clear();
        orders.addAll(newOrders);
        
        orderList.clearAllEntries();
        for (ClientOrder order : orders) {
            orderList.addOrderEntry(new OrderEntry(order));
        }
        
        // Если нужно выделить конкретный заказ
        if (highlightOrderId != null) {
            for (ClientOrder order : orders) {
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
        String orderInfo = String.format("Заказов: %d", orders.size());
        context.drawText(this.textRenderer, orderInfo, left + 10, top + 20, CourierNetworking.COLOR_TEXT_SECONDARY, false);
        
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
        if (orders.isEmpty() && !isLoading) {
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
        private final ButtonWidget openButton;
        
        public OrderEntry(ClientOrder order) {
            this.order = order;
            this.openButton = ButtonWidget.builder(Text.literal("Открыть"), 
                button -> MinecraftClient.getInstance().setScreen(new OrderDetailsScreen(order)))
                .dimensions(0, 0, 60, 18)
                .build();
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
            
            // Кнопка "Открыть"
            openButton.setX(x + entryWidth - 65);
            openButton.setY(y + 40);
            openButton.render(context, mouseX, mouseY, tickDelta);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return openButton.mouseClicked(mouseX, mouseY, button);
        }
        
        @Override
        public List<? extends net.minecraft.client.gui.Element> children() {
            return List.of(openButton);
        }
        
        @Override
        public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
            return List.of(openButton);
        }
    }
}