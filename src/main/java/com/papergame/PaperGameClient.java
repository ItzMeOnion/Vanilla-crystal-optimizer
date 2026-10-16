
package com.papergame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PaperGameClient implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance()
            .getConfigDir().resolve("papergame.json");

    private static final String[] DEFAULT_ITEMS = {
            "minecraft:diamond",
            "minecraft:gold_ingot",
            "minecraft:emerald",
            "minecraft:paper",
            "minecraft:apple",
            "minecraft:iron_ingot",
            "minecraft:coal",
            "minecraft:book",
            "minecraft:amethyst_shard"
    };

    private static final String[] ITEMS = new String[9];
    private static boolean overlayVisible = true;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        loadConfig();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.papergame.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.papergame"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                overlayVisible = !overlayVisible;
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (!overlayVisible) return;

            int x = 8;
            int y = 8;

            drawContext.drawTextWithShadow(
                    net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                    Text.literal("Paper Game — Number Items"),
                    x, y, 0xFFFFFF
            );

            for (int i = 0; i < 9; i++) {
                String line = (i + 1) + ": " + ITEMS[i];
                drawContext.drawTextWithShadow(
                        net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                        Text.literal(line),
                        x, y + 14 + i * 12, 0xAADDFF
                );
            }
        });
    }

    private static void loadConfig() {
        JsonObject json;

        try {
            if (Files.exists(CONFIG)) {
                json = GSON.fromJson(Files.readString(CONFIG), JsonObject.class);
            } else {
                json = new JsonObject();
            }
        } catch (Exception e) {
            json = new JsonObject();
        }

        JsonObject numbers = json.has("numbers") && json.get("numbers").isJsonObject()
                ? json.getAsJsonObject("numbers")
                : new JsonObject();

        for (int i = 0; i < 9; i++) {
            String key = String.valueOf(i + 1);
            String value = numbers.has(key) && numbers.get(key).isJsonPrimitive()
                    ? numbers.get(key).getAsString()
                    : DEFAULT_ITEMS[i];

            if (!isValidItem(value)) {
                value = DEFAULT_ITEMS[i];
            }

            ITEMS[i] = value;
            numbers.addProperty(key, value);
        }

        json.add("numbers", numbers);

        try {
            Files.createDirectories(CONFIG.getParent());
            Files.writeString(CONFIG, GSON.toJson(json));
        } catch (IOException e) {
            System.err.println("[Paper Game] Could not save config: " + e.getMessage());
        }
    }

    private static boolean isValidItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) return false;

        Item item = Registries.ITEM.get(identifier);
        return item != null && Registries.ITEM.getId(item).equals(identifier);
    }
}
