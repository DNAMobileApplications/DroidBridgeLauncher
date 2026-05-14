/*
 * Copyright (c) 2026 DNA Mobile Applications.
 * All rights reserved.
 *
 * This file is DroidBridge project code.
 * It is not part of Minecraft and does not grant rights to Minecraft,
 * Mojang, Microsoft, PojavLauncher, Zalith Launcher, or any third-party project.
 *
 * Files written entirely by DNA Mobile Applications are proprietary unless
 * a file header or separate license notice states otherwise.
 */

package ca.dnamobile.javalauncher.controls;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

/** One visible on-screen control button. */
public final class TouchControlData {
    public static final int SPECIAL_KEYBOARD = -1;
    public static final int SPECIAL_TOGGLE_CONTROLS = -2;
    public static final int SPECIAL_MOUSE_LEFT = -3;
    public static final int SPECIAL_MOUSE_RIGHT = -4;
    public static final int SPECIAL_VIRTUAL_MOUSE = -5;
    public static final int SPECIAL_MOUSE_MIDDLE = -6;
    public static final int SPECIAL_SCROLL_UP = -7;
    public static final int SPECIAL_SCROLL_DOWN = -8;
    public static final int SPECIAL_MENU = -9;
    public static final int SPECIAL_KEY_SENDER_KEYBOARD = -10;
    public static final int MAX_ACTION_SLOTS = 4;

    @NonNull public String id = UUID.randomUUID().toString();
    @NonNull public String label = "Button";
    @NonNull public String action = TouchControlActions.KEY;
    public int keyCode = 32;
    @NonNull public int[] keyCodes = new int[0];
    @NonNull public int[] keySlots = new int[0];
    public int mouseButton = 0;
    public int scrollY = 0;
    public float x = 32f;
    public float y = 32f;
    public float width = 64f;
    public float height = 48f;
    public float sizePercent = 100f;
    public float opacity = 0.72f;
    public float cornerRadius = 16f;
    public float strokeWidth = 2f;
    public int strokeColor = 0x99FFFFFF;
    public int backgroundColor = 0x66000000;
    public boolean toggle;
    public boolean visibleInGame = true;
    public boolean visibleInMenu = true;
    /**
     * When true, this control remains visible and clickable after the
     * Show / hide touch controls action hides the normal on-screen buttons.
     */
    public boolean visibleWhenControlsHidden;
    public boolean joystickAbsolute;
    public boolean joystickForwardLock;

    @Nullable public String rawX;
    @Nullable public String rawY;

    @NonNull
    public static TouchControlData key(@NonNull String label, int keyCode, float x, float y, float width, float height) {
        TouchControlData data = new TouchControlData();
        data.label = label;
        data.action = TouchControlActions.KEY;
        data.keyCode = keyCode;
        data.setKeyCodes(new int[]{keyCode});
        data.x = x;
        data.y = y;
        data.width = width;
        data.height = height;
        return data;
    }

    @NonNull
    public static TouchControlData mouse(@NonNull String label, int mouseButton, float x, float y) {
        TouchControlData data = new TouchControlData();
        data.label = label;
        data.action = TouchControlActions.MOUSE;
        data.mouseButton = mouseButton;
        data.x = x;
        data.y = y;
        data.width = 58f;
        data.height = 58f;
        return data;
    }

    @NonNull
    public static TouchControlData joystick(@NonNull String label, float x, float y, float width, float height) {
        TouchControlData data = new TouchControlData();
        data.label = label;
        data.action = TouchControlActions.JOYSTICK;
        data.x = x;
        data.y = y;
        data.width = width;
        data.height = height;
        data.opacity = 0.55f;
        data.visibleInGame = true;
        data.visibleInMenu = false;
        data.cornerRadius = 999f;
        return data;
    }

    @NonNull
    public TouchControlData copy() {
        TouchControlData copy = new TouchControlData();
        copy.id = UUID.randomUUID().toString();
        copy.label = label;
        copy.action = action;
        copy.keyCode = keyCode;
        copy.keyCodes = keyCodes != null ? keyCodes.clone() : new int[0];
        copy.keySlots = keySlots != null ? keySlots.clone() : new int[0];
        copy.mouseButton = mouseButton;
        copy.scrollY = scrollY;
        copy.x = x;
        copy.y = y;
        copy.width = width;
        copy.height = height;
        copy.sizePercent = sizePercent;
        copy.opacity = opacity;
        copy.cornerRadius = cornerRadius;
        copy.strokeWidth = strokeWidth;
        copy.strokeColor = strokeColor;
        copy.backgroundColor = backgroundColor;
        copy.toggle = toggle;
        copy.visibleInGame = visibleInGame;
        copy.visibleInMenu = visibleInMenu;
        copy.visibleWhenControlsHidden = visibleWhenControlsHidden;
        copy.joystickAbsolute = joystickAbsolute;
        copy.joystickForwardLock = joystickForwardLock;
        copy.rawX = rawX;
        copy.rawY = rawY;
        return copy;
    }

    @NonNull
    public JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("label", label);
        json.put("action", action);
        json.put("keyCode", keyCode);
        JSONArray keys = new JSONArray();
        for (int code : normalizedKeyCodes()) keys.put(code);
        json.put("keyCodes", keys);
        JSONArray slots = new JSONArray();
        for (int code : normalizedKeySlots()) slots.put(code);
        json.put("keySlots", slots);
        json.put("mouseButton", mouseButton);
        json.put("scrollY", scrollY);
        json.put("x", x);
        json.put("y", y);
        json.put("width", width);
        json.put("height", height);
        json.put("sizePercent", sizePercent);
        json.put("opacity", opacity);
        json.put("cornerRadius", cornerRadius);
        json.put("strokeWidth", strokeWidth);
        json.put("strokeColor", strokeColor);
        json.put("backgroundColor", backgroundColor);
        json.put("toggle", toggle);
        json.put("visibleInGame", visibleInGame);
        json.put("visibleInMenu", visibleInMenu);
        json.put("visibleWhenControlsHidden", visibleWhenControlsHidden);
        json.put("joystickAbsolute", joystickAbsolute);
        json.put("joystickForwardLock", joystickForwardLock);
        if (rawX != null) json.put("rawX", rawX);
        if (rawY != null) json.put("rawY", rawY);
        return json;
    }

    @NonNull
    public static TouchControlData fromJson(@NonNull JSONObject json) {
        TouchControlData data = new TouchControlData();
        data.id = sanitizeId(json.optString("id", data.id));
        data.label = json.optString("label", json.optString("name", data.label));
        data.action = json.optString("action", data.action);
        data.keyCode = json.optInt("keyCode", data.keyCode);
        data.keySlots = readKeySlots(json.optJSONArray("keySlots"), json.optJSONArray("keyCodes"), data.keyCode);
        data.keyCodes = activeCodesFromSlots(data.keySlots);
        data.keyCode = firstUsableKey(data.keyCodes, 0);
        data.mouseButton = json.optInt("mouseButton", data.mouseButton);
        data.scrollY = json.optInt("scrollY", data.scrollY);
        data.x = (float) json.optDouble("x", data.x);
        data.y = (float) json.optDouble("y", data.y);
        data.width = (float) json.optDouble("width", data.width);
        data.height = (float) json.optDouble("height", data.height);
        data.sizePercent = clampSizePercent((float) json.optDouble("sizePercent", data.sizePercent));
        data.opacity = (float) json.optDouble("opacity", data.opacity);
        data.cornerRadius = (float) json.optDouble("cornerRadius", data.cornerRadius);
        data.strokeWidth = (float) json.optDouble("strokeWidth", data.strokeWidth);
        data.strokeColor = json.optInt("strokeColor", data.strokeColor);
        data.backgroundColor = json.optInt("backgroundColor", json.optInt("bgColor", data.backgroundColor));
        data.toggle = json.optBoolean("toggle", json.optBoolean("isToggle", data.toggle));
        data.visibleInGame = json.optBoolean("visibleInGame", json.optBoolean("displayInGame", data.visibleInGame));
        data.visibleInMenu = json.optBoolean("visibleInMenu", json.optBoolean("displayInMenu", data.visibleInMenu));
        data.visibleWhenControlsHidden = readVisibleWhenControlsHidden(json, data.action);
        data.joystickAbsolute = json.optBoolean("joystickAbsolute", json.optBoolean("absolute", data.joystickAbsolute));
        data.joystickForwardLock = json.optBoolean("joystickForwardLock", json.optBoolean("forwardLock", data.joystickForwardLock));
        data.rawX = optNullableString(json, "rawX", optNullableString(json, "dynamicX", null));
        data.rawY = optNullableString(json, "rawY", optNullableString(json, "dynamicY", null));
        return data;
    }

    @NonNull
    public static TouchControlData fromPojavControl(@NonNull JSONObject json) {
        TouchControlData data = new TouchControlData();
        data.id = sanitizeId(json.optString("id", data.id));
        data.label = json.optString("name", json.optString("label", "Button"));
        data.width = (float) json.optDouble("width", 64d);
        data.height = (float) json.optDouble("height", 48d);
        data.opacity = (float) json.optDouble("opacity", 0.72d);
        data.cornerRadius = (float) json.optDouble("cornerRadius", data.cornerRadius);
        data.strokeWidth = (float) json.optDouble("strokeWidth", data.strokeWidth);
        data.strokeColor = json.optInt("strokeColor", data.strokeColor);
        data.backgroundColor = json.optInt("bgColor", json.optInt("backgroundColor", data.backgroundColor));
        data.toggle = json.optBoolean("isToggle", json.optBoolean("toggle", false));
        data.visibleInGame = json.optBoolean("displayInGame", json.optBoolean("visibleInGame", true));
        data.visibleInMenu = json.optBoolean("displayInMenu", json.optBoolean("visibleInMenu", true));
        data.rawX = optNullableString(json, "dynamicX", optNullableString(json, "rawX", null));
        data.rawY = optNullableString(json, "dynamicY", optNullableString(json, "rawY", null));

        int[] importedKeys = readKeyCodes(json.optJSONArray("keycodes"), 32);
        int firstKey = firstUsableKey(importedKeys, 32);
        applyImportedKey(data, firstKey, importedKeys);
        data.visibleWhenControlsHidden = readVisibleWhenControlsHidden(json, data.action);
        data.x = (float) json.optDouble("x", data.x);
        data.y = (float) json.optDouble("y", data.y);
        return data;
    }

    @NonNull
    public static TouchControlData fromPojavJoystick(@NonNull JSONObject json) {
        TouchControlData data = joystick(
                json.optString("name", json.optString("label", "Joystick")),
                (float) json.optDouble("x", 32d),
                (float) json.optDouble("y", 360d),
                (float) json.optDouble("width", 120d),
                (float) json.optDouble("height", 120d)
        );
        data.id = sanitizeId(json.optString("id", data.id));
        data.opacity = (float) json.optDouble("opacity", data.opacity);
        data.cornerRadius = (float) json.optDouble("cornerRadius", data.cornerRadius);
        data.strokeWidth = (float) json.optDouble("strokeWidth", data.strokeWidth);
        data.strokeColor = json.optInt("strokeColor", data.strokeColor);
        data.backgroundColor = json.optInt("bgColor", json.optInt("backgroundColor", data.backgroundColor));
        data.visibleInGame = json.optBoolean("displayInGame", true);
        data.visibleInMenu = json.optBoolean("displayInMenu", false);
        data.visibleWhenControlsHidden = readVisibleWhenControlsHidden(json, data.action);
        data.rawX = optNullableString(json, "dynamicX", null);
        data.rawY = optNullableString(json, "dynamicY", null);
        data.joystickAbsolute = json.optBoolean("absolute", false);
        data.joystickForwardLock = json.optBoolean("forwardLock", false);
        return data;
    }


    private static boolean readVisibleWhenControlsHidden(@NonNull JSONObject json, @NonNull String action) {
        boolean fallback = shouldStayVisibleWhenControlsHiddenByDefault(action);
        return json.optBoolean(
                "visibleWhenControlsHidden",
                json.optBoolean(
                        "keepVisibleWhenControlsHidden",
                        json.optBoolean(
                                "keepVisibleWhenHidden",
                                json.optBoolean(
                                        "displayWhenHidden",
                                        json.optBoolean("alwaysVisible", fallback)
                                )
                        )
                )
        );
    }

    /**
     * The control that hides the overlay must always survive its own hide action.
     * Other controls, including the launcher GUI/menu button, only stay visible when
     * the layout explicitly enables visibleWhenControlsHidden.
     */
    public static boolean shouldStayVisibleWhenControlsHiddenByDefault(@Nullable String action) {
        return TouchControlActions.TOGGLE_CONTROLS.equals(action);
    }

    @NonNull
    public int[] normalizedKeyCodes() {
        if (keySlots != null && keySlots.length > 0) {
            return activeCodesFromSlots(toFixedSlots(keySlots, 0));
        }
        if (keyCodes != null && keyCodes.length > 0) return activeCodesFromSlots(toFixedSlots(keyCodes, keyCode));
        return keyCode == 0 ? new int[0] : new int[]{keyCode};
    }

    @NonNull
    public int[] normalizedKeySlots() {
        if (keySlots != null && keySlots.length > 0) return toFixedSlots(keySlots, 0);
        if (keyCodes != null && keyCodes.length > 0) return toFixedSlots(keyCodes, keyCode);
        return toFixedSlots(keyCode == 0 ? new int[0] : new int[]{keyCode}, keyCode);
    }

    public int getKeySlot(int slot) {
        int safeSlot = Math.max(0, Math.min(MAX_ACTION_SLOTS - 1, slot));
        int[] slots = normalizedKeySlots();
        return safeSlot < slots.length ? slots[safeSlot] : 0;
    }

    public void setKeySlot(int slot, int keyCodeValue) {
        int safeSlot = Math.max(0, Math.min(MAX_ACTION_SLOTS - 1, slot));
        int[] slots = normalizedKeySlots();
        slots[safeSlot] = keyCodeValue;
        setKeySlots(slots);
    }

    public void clearKeySlot(int slot) {
        setKeySlot(slot, 0);
    }

    public void setKeyCodes(@NonNull int[] codes) {
        setKeySlots(toFixedSlots(codes, 0));
    }

    public void setKeySlots(@NonNull int[] slots) {
        keySlots = toFixedSlots(slots, 0);
        keyCodes = activeCodesFromSlots(keySlots);
        keyCode = firstUsableKey(keyCodes, 0);
    }

    @NonNull
    private static int[] readKeySlots(@Nullable JSONArray slotArray, @Nullable JSONArray legacyArray, int fallback) {
        if (slotArray != null && slotArray.length() > 0) {
            return toFixedSlots(readRawCodes(slotArray), fallback);
        }
        return toFixedSlots(readKeyCodes(legacyArray, fallback), fallback);
    }

    @NonNull
    private static int[] readKeyCodes(@Nullable JSONArray array, int fallback) {
        int[] raw = readRawCodes(array);
        if (raw.length == 0) return fallback == 0 ? new int[0] : new int[]{fallback};
        return activeCodesFromSlots(raw);
    }

    @NonNull
    private static int[] readRawCodes(@Nullable JSONArray array) {
        if (array == null || array.length() == 0) return new int[0];
        ArrayList<Integer> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            int value = array.optInt(i, Integer.MIN_VALUE);
            if (value == Integer.MIN_VALUE) continue;
            values.add(value);
        }
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
    }

    @NonNull
    private static int[] toFixedSlots(@NonNull int[] codes, int fallback) {
        int[] result = new int[MAX_ACTION_SLOTS];
        int write = 0;
        if (codes.length == MAX_ACTION_SLOTS) {
            for (int i = 0; i < MAX_ACTION_SLOTS; i++) result[i] = codes[i];
            return result;
        }
        for (int code : codes) {
            if (write >= MAX_ACTION_SLOTS) break;
            if (code == 0) continue;
            result[write++] = code;
        }
        if (write == 0 && fallback > 0) result[0] = fallback;
        return result;
    }

    @NonNull
    private static int[] activeCodesFromSlots(@NonNull int[] slots) {
        ArrayList<Integer> values = new ArrayList<>();
        for (int slot = 0; slot < Math.min(MAX_ACTION_SLOTS, slots.length); slot++) {
            int value = slots[slot];
            if (value != 0) values.add(value);
        }
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
    }

    private static int firstUsableKey(@NonNull int[] keycodes, int fallback) {
        for (int key : keycodes) {
            if (key != 0) return key;
        }
        return fallback;
    }

    @Nullable
    private static String optNullableString(@NonNull JSONObject json, @NonNull String key, @Nullable String fallback) {
        if (!json.has(key) || json.isNull(key)) return fallback;
        String value = json.optString(key, null);
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim()) ? fallback : value;
    }

    @NonNull
    private static String sanitizeId(@Nullable String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return UUID.randomUUID().toString();
        }
        return value.trim();
    }

    private static float clampSizePercent(float value) {
        return Math.max(30f, Math.min(250f, value));
    }

    private static void applyImportedKey(@NonNull TouchControlData data, int key, @NonNull int[] allKeys) {
        switch (key) {
            case SPECIAL_TOGGLE_CONTROLS:
                data.action = TouchControlActions.TOGGLE_CONTROLS;
                return;
            case SPECIAL_MOUSE_LEFT:
                data.action = TouchControlActions.MOUSE;
                data.mouseButton = 0;
                return;
            case SPECIAL_MOUSE_RIGHT:
                data.action = TouchControlActions.MOUSE;
                data.mouseButton = 1;
                return;
            case SPECIAL_MOUSE_MIDDLE:
                data.action = TouchControlActions.MOUSE;
                data.mouseButton = 2;
                return;
            case SPECIAL_SCROLL_UP:
                data.action = TouchControlActions.SCROLL;
                data.scrollY = 1;
                return;
            case SPECIAL_SCROLL_DOWN:
                data.action = TouchControlActions.SCROLL;
                data.scrollY = -1;
                return;
            case SPECIAL_MENU:
                data.action = TouchControlActions.MENU;
                return;
            case SPECIAL_KEYBOARD:
                data.action = TouchControlActions.KEYBOARD;
                return;
            case SPECIAL_KEY_SENDER_KEYBOARD:
                data.action = TouchControlActions.KEY_SENDER_KEYBOARD;
                return;
            case SPECIAL_VIRTUAL_MOUSE:
                data.action = TouchControlActions.VIRTUAL_MOUSE;
                return;
            default:
                data.action = TouchControlActions.KEY;
                data.keyCode = key;
                data.setKeyCodes(allKeys);
        }
    }
}
