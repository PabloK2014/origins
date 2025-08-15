package com.example.courier.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import java.util.function.Consumer;

public class ItemPickerScreen extends Screen {
    private final Consumer<ItemStack> callback;

    protected ItemPickerScreen(Consumer<ItemStack> callback) {
        super(Text.of("Выберите предмет"));
        this.callback = callback;
    }

    @Override
    protected void init() {
        int y = 20;
        // Проходим по Registries.ITEM — может быть много, это упрощение (в идеале страница/поиск)
        int index = 0;
        for (var id : Registries.ITEM.getIds()) {
            if (index >= 50) break; // лимит в примере, в реале нужно копирование страницы/поиск
            var item = Registries.ITEM.get(id);
            ItemStack s = new ItemStack(item, 1);

            this.addDrawableChild(new ButtonWidget(10, y, 200, 20, Text.of(s.getName().getString()), b -> {
                callback.accept(s);
                this.onClose();
            }));
            y += 22;
            index++;
        }
        this.addDrawableChild(new ButtonWidget(this.width/2 - 50, this.height - 30, 100, 20, Text.of("Отмена"), b -> this.onClose()));
    }
}
