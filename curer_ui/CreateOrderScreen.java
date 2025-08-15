package com.example.courier.client;

import com.example.courier.CourierNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;

public class CreateOrderScreen extends Screen {
    private final List<ItemStack> request = new ArrayList<>();
    private final List<TextFieldWidget> requestCounts = new ArrayList<>();
    private final List<ItemStack> rewards = new ArrayList<>();
    private final List<TextFieldWidget> rewardCounts = new ArrayList<>();
    private TextFieldWidget descriptionField;

    protected CreateOrderScreen() {
        super(Text.of("Создать заказ"));
    }

    @Override
    protected void init() {
        int y = 20;
        // Сделаем 5 слотов по-умолчанию (можно расширить до 10)
        for (int i=0;i<5;i++) {
            int idx = i;
            this.addDrawableChild(new ButtonWidget(10, y, 20, 20, Text.of("+"), b -> openPickerForRequest(idx)));
            TextFieldWidget tf = new TextFieldWidget(this.textRenderer, 40, y, 30, 20, Text.of("qty"));
            tf.setText("1");
            this.requestCounts.add(tf);
            this.children.add(tf);
            this.request.add(ItemStack.EMPTY);
            y += 24;
        }

        y += 6;
        // reward slots
        for (int i=0;i<5;i++) {
            int idx = i;
            this.addDrawableChild(new ButtonWidget(200, y - (i*24) - 120, 20, 20, Text.of("+"), b -> openPickerForReward(idx)));
            TextFieldWidget tf = new TextFieldWidget(this.textRenderer, 230, y - (i*24) - 120, 30, 20, Text.of("qty"));
            tf.setText("1");
            this.rewardCounts.add(tf);
            this.children.add(tf);
            this.rewards.add(ItemStack.EMPTY);
            y += 0;
        }

        descriptionField = new TextFieldWidget(this.textRenderer, 10, 200, 300, 20, Text.of("description"));
        descriptionField.setText("");
        this.children.add(descriptionField);

        this.addDrawableChild(new ButtonWidget(this.width/2 - 50, this.height - 30, 100, 20, Text.of("Отправить заказ"), b -> sendCreate()));
    }

    private void openPickerForRequest(int idx) {
        MinecraftClient.getInstance().openScreen(new ItemPickerScreen(stack -> {
            request.set(idx, stack.copy());
        }));
    }
    private void openPickerForReward(int idx) {
        MinecraftClient.getInstance().openScreen(new ItemPickerScreen(stack -> {
            rewards.set(idx, stack.copy());
        }));
    }

    private void sendCreate() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(descriptionField.getText());
        // request
        int reqCount = (int) request.stream().filter(s -> !s.isEmpty()).count();
        buf.writeInt(reqCount);
        for (int i=0;i<request.size();i++) {
            ItemStack s = request.get(i);
            if (!s.isEmpty()) {
                // установим count из поля
                int qty = 1;
                try { qty = Integer.parseInt(requestCounts.get(i).getText()); } catch (Exception ignored) {}
                ItemStack toSend = s.copy(); toSend.setCount(Math.max(1, Math.min(64, qty)));
                buf.writeItemStack(toSend);
            }
        }
        int rewCount = (int) rewards.stream().filter(s -> !s.isEmpty()).count();
        buf.writeInt(rewCount);
        for (int i=0;i<rewards.size();i++) {
            ItemStack s = rewards.get(i);
            if (!s.isEmpty()) {
                int qty = 1;
                try { qty = Integer.parseInt(rewardCounts.get(i).getText()); } catch (Exception ignored) {}
                ItemStack toSend = s.copy(); toSend.setCount(Math.max(1, Math.min(64, qty)));
                buf.writeItemStack(toSend);
            }
        }
        CourierNetworking.sendCreateOrder(buf);
        this.minecraft.player.sendMessage(Text.of("Заказ отправлен"), false);
        this.onClose();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width/2, 8, 0xFFFFFF);
        // отрисовка выбранных предметов (иконки) — можно наполнить
        super.render(matrices, mouseX, mouseY, delta);
    }
}
