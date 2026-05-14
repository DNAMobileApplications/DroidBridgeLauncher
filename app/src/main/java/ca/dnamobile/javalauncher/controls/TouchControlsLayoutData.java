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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Serializable JavaLauncher touch-control layout. */
public final class TouchControlsLayoutData {
    public static final String UNIT_DP = "dp";
    public static final String UNIT_PX = "px";

    private static final float DEFAULT_IMPORTED_SOURCE_WIDTH = 854f;
    private static final float DEFAULT_IMPORTED_SOURCE_HEIGHT = 480f;

    public int version = 4;
    @NonNull public String name = "Touch Controls";
    public float preferredScale = 100f;
    @NonNull public String coordinateUnit = UNIT_DP;
    public float sourceWidth = 0f;
    public float sourceHeight = 0f;

    @NonNull public final List<TouchControlData> controls = new ArrayList<>();

    @NonNull
    public JSONObject toJson() throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", "JavaLauncherTouchControls");
        root.put("version", version);
        root.put("name", name);
        root.put("preferredScale", preferredScale);
        root.put("coordinateUnit", normalizeCoordinateUnit(coordinateUnit));
        if (sourceWidth > 0f) root.put("sourceWidth", sourceWidth);
        if (sourceHeight > 0f) root.put("sourceHeight", sourceHeight);
        JSONArray array = new JSONArray();
        for (TouchControlData control : controls) {
            array.put(control.toJson());
        }
        root.put("controls", array);
        return root;
    }

    @NonNull
    public static TouchControlsLayoutData fromJson(@NonNull JSONObject root) throws Exception {
        if (root.has("controls")) {
            TouchControlsLayoutData data = new TouchControlsLayoutData();
            data.version = root.optInt("version", 1);
            data.name = root.optString("name", "Touch Controls");
            data.preferredScale = (float) root.optDouble("preferredScale", root.optDouble("scaledAt", 100d));
            boolean hasExplicitUnit = hasCoordinateUnit(root);
            data.coordinateUnit = readCoordinateUnit(root, UNIT_DP);
            readSourceCanvas(root, data);
            JSONArray controls = root.optJSONArray("controls");
            if (controls != null) {
                for (int i = 0; i < controls.length(); i++) {
                    JSONObject object = controls.optJSONObject(i);
                    if (object != null) data.controls.add(TouchControlData.fromJson(object));
                }
            }
            boolean legacyImportedCanvas = looksLikeImportedCanvasLayout(data);
            if (legacyImportedCanvas && (!hasExplicitUnit || data.version < 3)) {
                data.coordinateUnit = UNIT_PX;
                inferPixelSourceCanvasIfNeeded(data, true);
            } else if (!hasExplicitUnit && looksLikePixelAuthoredLayout(data)) {
                data.coordinateUnit = UNIT_PX;
                inferPixelSourceCanvasIfNeeded(data, true);
            } else {
                inferPixelSourceCanvasIfNeeded(data, false);
            }
            if (data.usesPixelCoordinates() && legacyImportedCanvas) {
                data.version = Math.max(data.version, 3);
            }
            normalizeSuspiciousImportedSourceCanvas(data);
            normalizeOutOfBoundsPixelCanvas(data);
            return data;
        }

        // Pojav/Zalith/Mojo/Amethyst derived layouts normally carry mControlDataList.
        if (root.has("mControlDataList") || root.has("mJoystickDataList") || root.has("mDrawerDataList")) {
            return fromPojavLikeJson(root);
        }

        // Some launchers export a flat buttons array. Treat it as best-effort.
        if (root.has("buttons")) {
            TouchControlsLayoutData data = new TouchControlsLayoutData();
            data.name = root.optString("name", "Imported Controls");
            data.preferredScale = (float) root.optDouble("preferredScale", root.optDouble("scaledAt", 100d));
            boolean hasExplicitUnit = hasCoordinateUnit(root);
            data.coordinateUnit = readCoordinateUnit(root, hasSourceCanvas(root) ? UNIT_PX : UNIT_DP);
            readSourceCanvas(root, data);
            JSONArray buttons = root.optJSONArray("buttons");
            if (buttons != null) {
                for (int i = 0; i < buttons.length(); i++) {
                    JSONObject object = buttons.optJSONObject(i);
                    if (object != null) data.controls.add(TouchControlData.fromJson(object));
                }
            }
            boolean legacyImportedCanvas = looksLikeImportedCanvasLayout(data);
            if (legacyImportedCanvas && (!hasExplicitUnit || data.version < 3)) {
                data.coordinateUnit = UNIT_PX;
                inferPixelSourceCanvasIfNeeded(data, true);
            } else if (!hasExplicitUnit && looksLikePixelAuthoredLayout(data)) {
                data.coordinateUnit = UNIT_PX;
                inferPixelSourceCanvasIfNeeded(data, true);
            } else {
                inferPixelSourceCanvasIfNeeded(data, false);
            }
            if (data.usesPixelCoordinates() && legacyImportedCanvas) {
                data.version = Math.max(data.version, 3);
            }
            normalizeSuspiciousImportedSourceCanvas(data);
            normalizeOutOfBoundsPixelCanvas(data);
            return data;
        }

        throw new IllegalArgumentException("Unsupported touch control layout format.");
    }

    @NonNull
    private static TouchControlsLayoutData fromPojavLikeJson(@NonNull JSONObject root) {
        TouchControlsLayoutData data = new TouchControlsLayoutData();
        data.name = root.optString("name", "Imported Pojav/Zalith Controls");
        data.preferredScale = (float) root.optDouble("scaledAt", root.optDouble("preferredScale", 100d));
        data.coordinateUnit = UNIT_PX;
        readSourceCanvas(root, data);

        JSONArray controls = root.optJSONArray("mControlDataList");
        if (controls != null) {
            for (int i = 0; i < controls.length(); i++) {
                JSONObject object = controls.optJSONObject(i);
                if (object != null) data.controls.add(TouchControlData.fromPojavControl(object));
            }
        }
        JSONArray joysticks = root.optJSONArray("mJoystickDataList");
        if (joysticks != null) {
            for (int i = 0; i < joysticks.length(); i++) {
                JSONObject object = joysticks.optJSONObject(i);
                if (object != null) data.controls.add(TouchControlData.fromPojavJoystick(object));
            }
        }

        JSONArray drawers = root.optJSONArray("mDrawerDataList");
        if (drawers != null) {
            for (int i = 0; i < drawers.length(); i++) {
                JSONObject drawer = drawers.optJSONObject(i);
                if (drawer == null) continue;
                JSONObject properties = drawer.optJSONObject("properties");
                if (properties != null) data.controls.add(TouchControlData.fromPojavControl(properties));
                JSONArray subButtons = drawer.optJSONArray("buttonProperties");
                if (subButtons != null) {
                    for (int j = 0; j < subButtons.length(); j++) {
                        JSONObject sub = subButtons.optJSONObject(j);
                        if (sub != null) data.controls.add(TouchControlData.fromPojavControl(sub));
                    }
                }
            }
        }

        // Old default_touch.json files usually store Android logical layout units
        // from a 1920x1080-class phone, which is about an 854x480 canvas at xxhdpi.
        // Use that as the fallback, and grow only when the coordinates prove it must.
        inferPixelSourceCanvasIfNeeded(data, true);
        normalizeSuspiciousImportedSourceCanvas(data);
        normalizeOutOfBoundsPixelCanvas(data);
        return data;
    }

    public boolean usesPixelCoordinates() {
        return UNIT_PX.equals(normalizeCoordinateUnit(coordinateUnit));
    }

    public float resolvedSourceWidth(float fallback) {
        return sourceWidth > 0f ? sourceWidth : Math.max(1f, fallback);
    }

    public float resolvedSourceHeight(float fallback) {
        return sourceHeight > 0f ? sourceHeight : Math.max(1f, fallback);
    }

    @NonNull
    private static String readCoordinateUnit(@NonNull JSONObject root, @NonNull String fallback) {
        String value = firstString(root,
                "coordinateUnit",
                "coordinate_unit",
                "layoutUnit",
                "layout_unit",
                "units",
                "unit"
        );
        if (value.trim().isEmpty()) return normalizeCoordinateUnit(fallback);
        return normalizeCoordinateUnit(value);
    }

    private static boolean hasCoordinateUnit(@NonNull JSONObject root) {
        return root.has("coordinateUnit")
                || root.has("coordinate_unit")
                || root.has("layoutUnit")
                || root.has("layout_unit")
                || root.has("units")
                || root.has("unit");
    }

    @NonNull
    private static String normalizeCoordinateUnit(@NonNull String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("pixel".equals(normalized) || "pixels".equals(normalized) || "px".equals(normalized)) return UNIT_PX;
        return UNIT_DP;
    }

    private static void readSourceCanvas(@NonNull JSONObject root, @NonNull TouchControlsLayoutData data) {
        data.sourceWidth = firstPositiveFloat(root,
                "sourceWidth",
                "source_width",
                "baseWidth",
                "base_width",
                "canvasWidth",
                "canvas_width",
                "layoutWidth",
                "layout_width",
                "screenWidth",
                "screen_width",
                "displayWidth",
                "display_width",
                "deviceWidth",
                "device_width",
                "physicalWidth",
                "physical_width",
                "width"
        );
        data.sourceHeight = firstPositiveFloat(root,
                "sourceHeight",
                "source_height",
                "baseHeight",
                "base_height",
                "canvasHeight",
                "canvas_height",
                "layoutHeight",
                "layout_height",
                "screenHeight",
                "screen_height",
                "displayHeight",
                "display_height",
                "deviceHeight",
                "device_height",
                "physicalHeight",
                "physical_height",
                "height"
        );
    }

    private static boolean hasSourceCanvas(@NonNull JSONObject root) {
        return firstPositiveFloat(root,
                "sourceWidth", "source_width", "baseWidth", "base_width", "canvasWidth", "canvas_width",
                "layoutWidth", "layout_width", "screenWidth", "screen_width", "displayWidth", "display_width",
                "deviceWidth", "device_width", "physicalWidth", "physical_width", "width"
        ) > 0f && firstPositiveFloat(root,
                "sourceHeight", "source_height", "baseHeight", "base_height", "canvasHeight", "canvas_height",
                "layoutHeight", "layout_height", "screenHeight", "screen_height", "displayHeight", "display_height",
                "deviceHeight", "device_height", "physicalHeight", "physical_height", "height"
        ) > 0f;
    }

    private static float firstPositiveFloat(@NonNull JSONObject root, @NonNull String... keys) {
        for (String key : keys) {
            if (!root.has(key) || root.isNull(key)) continue;
            double value = root.optDouble(key, 0d);
            if (value > 0d && !Double.isNaN(value) && !Double.isInfinite(value)) return (float) value;
        }
        return 0f;
    }

    @NonNull
    private static String firstString(@NonNull JSONObject root, @NonNull String... keys) {
        for (String key : keys) {
            if (!root.has(key) || root.isNull(key)) continue;
            String value = root.optString(key, "");
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static boolean looksLikePixelAuthoredLayout(@NonNull TouchControlsLayoutData data) {
        float maxRight = 0f;
        float maxBottom = 0f;
        float maxWidth = 0f;
        float maxHeight = 0f;

        for (TouchControlData control : data.controls) {
            maxWidth = Math.max(maxWidth, control.width);
            maxHeight = Math.max(maxHeight, control.height);
            if (control.rawX == null) maxRight = Math.max(maxRight, control.x + Math.max(1f, control.width));
            if (control.rawY == null) maxBottom = Math.max(maxBottom, control.y + Math.max(1f, control.height));
        }

        return maxRight > 1000f
                || maxBottom > 700f
                || maxWidth > 260f
                || maxHeight > 180f;
    }

    private static boolean looksLikeImportedCanvasLayout(@NonNull TouchControlsLayoutData data) {
        String name = data.name == null ? "" : data.name.trim().toLowerCase(Locale.ROOT);
        boolean importedName = name.contains("pojav")
                || name.contains("zalith")
                || name.contains("mojo")
                || name.contains("amethyst");
        if (!importedName) return false;

        float maxRight = 0f;
        float maxBottom = 0f;
        for (TouchControlData control : data.controls) {
            if (control.rawX == null) maxRight = Math.max(maxRight, control.x + Math.max(1f, control.width));
            if (control.rawY == null) maxBottom = Math.max(maxBottom, control.y + Math.max(1f, control.height));
        }

        // This catches the already-converted JavaLauncher JSON you pasted: right edge
        // around 833 and bottom edge around 407. Those values are not normal modern
        // Android pixels; they are a source canvas that must be scaled to the current view.
        return maxRight >= 500f || maxBottom >= 250f;
    }

    private static void normalizeSuspiciousImportedSourceCanvas(@NonNull TouchControlsLayoutData data) {
        if (!data.usesPixelCoordinates() || !looksLikeImportedCanvasLayout(data)) return;

        float maxRight = 0f;
        float maxBottom = 0f;
        for (TouchControlData control : data.controls) {
            if (control.rawX == null) maxRight = Math.max(maxRight, control.x + Math.max(1f, control.width));
            if (control.rawY == null) maxBottom = Math.max(maxBottom, control.y + Math.max(1f, control.height));
        }

        if (maxRight > 1f && maxRight <= 960f && data.sourceWidth >= 1000f) {
            data.sourceWidth = inferCanvasAxis(maxRight, DEFAULT_IMPORTED_SOURCE_WIDTH, true, true);
        }
        if (maxBottom > 1f && maxBottom <= 540f && data.sourceHeight >= 700f) {
            data.sourceHeight = inferCanvasAxis(maxBottom, DEFAULT_IMPORTED_SOURCE_HEIGHT, true, false);
        }
    }


    private static void normalizeOutOfBoundsPixelCanvas(@NonNull TouchControlsLayoutData data) {
        if (!data.usesPixelCoordinates() || data.controls.isEmpty()) return;

        float minLeft = Float.MAX_VALUE;
        float minTop = Float.MAX_VALUE;
        float maxRight = 0f;
        float maxBottom = 0f;
        boolean hasAbsoluteCoordinate = false;

        for (TouchControlData control : data.controls) {
            if (control.rawX == null) {
                float width = Math.max(1f, control.width);
                minLeft = Math.min(minLeft, control.x);
                maxRight = Math.max(maxRight, control.x + width);
                hasAbsoluteCoordinate = true;
            }
            if (control.rawY == null) {
                float height = Math.max(1f, control.height);
                minTop = Math.min(minTop, control.y);
                maxBottom = Math.max(maxBottom, control.y + height);
                hasAbsoluteCoordinate = true;
            }
        }

        if (!hasAbsoluteCoordinate) return;

        float originalWidth = data.sourceWidth > 0f ? data.sourceWidth : Math.max(DEFAULT_IMPORTED_SOURCE_WIDTH, maxRight);
        float originalHeight = data.sourceHeight > 0f ? data.sourceHeight : Math.max(DEFAULT_IMPORTED_SOURCE_HEIGHT, maxBottom);
        float shiftX = minLeft < 0f ? -minLeft : 0f;
        float shiftY = minTop < 0f ? -minTop : 0f;
        float normalizedWidth = Math.max(originalWidth + shiftX, maxRight + shiftX);
        float normalizedHeight = Math.max(originalHeight + shiftY, maxBottom + shiftY);

        boolean needsNormalize = shiftX > 0f
                || shiftY > 0f
                || maxRight > originalWidth
                || maxBottom > originalHeight;
        if (!needsNormalize) return;

        if (shiftX > 0f || shiftY > 0f) {
            for (TouchControlData control : data.controls) {
                if (shiftX > 0f && control.rawX == null) control.x += shiftX;
                if (shiftY > 0f && control.rawY == null) control.y += shiftY;
            }
        }

        data.sourceWidth = Math.max(1f, normalizedWidth);
        data.sourceHeight = Math.max(1f, normalizedHeight);
        data.version = Math.max(data.version, 4);
    }

    private static void inferPixelSourceCanvasIfNeeded(@NonNull TouchControlsLayoutData data, boolean importedPojavLike) {
        if (!data.usesPixelCoordinates()) return;

        float maxRight = 0f;
        float maxBottom = 0f;
        for (TouchControlData control : data.controls) {
            if (control.rawX == null) maxRight = Math.max(maxRight, control.x + Math.max(1f, control.width));
            if (control.rawY == null) maxBottom = Math.max(maxBottom, control.y + Math.max(1f, control.height));
        }

        if (data.sourceWidth <= 0f) {
            data.sourceWidth = inferCanvasAxis(maxRight, DEFAULT_IMPORTED_SOURCE_WIDTH, importedPojavLike, true);
        }
        if (data.sourceHeight <= 0f) {
            data.sourceHeight = inferCanvasAxis(maxBottom, DEFAULT_IMPORTED_SOURCE_HEIGHT, importedPojavLike, false);
        }
    }

    private static float inferCanvasAxis(float maxExtent, float fallback, boolean preferFallback, boolean horizontal) {
        if (maxExtent <= 1f) return preferFallback ? fallback : 0f;

        // If this was a legacy Pojav/Zalith style layout and the file does not say
        // otherwise, use the common logical canvas for a 1920x1080-class xxhdpi phone
        // (about 854x480). If coordinates prove a larger source, grow to the nearest
        // common canvas size instead.
        if (preferFallback && maxExtent <= fallback) return fallback;

        float padded = maxExtent + 16f;
        float[] common = horizontal
                ? new float[]{720f, 854f, 960f, 1024f, 1280f, 1366f, 1440f, 1600f, 1920f, 2160f, 2340f, 2400f, 2560f, 2800f, 3200f, 3840f}
                : new float[]{480f, 540f, 600f, 720f, 768f, 800f, 900f, 1080f, 1200f, 1440f, 1600f, 1800f, 2160f};
        for (float candidate : common) {
            if (candidate >= padded) return candidate;
        }
        return Math.max(padded, fallback);
    }

    @NonNull
    public static TouchControlsLayoutData defaultLayout() {
        TouchControlsLayoutData data = new TouchControlsLayoutData();
        data.name = "Default Touch Controls";
        data.coordinateUnit = UNIT_DP;

        // Defaults use dynamic formulas so they stay sane on phones, tablets, and
        // different display densities. Width/height are JavaLauncher layout units;
        // rawX/rawY formulas resolve to final Android pixels at draw time.
        TouchControlData keyboard = new TouchControlData();
        keyboard.label = "Keyboard";
        keyboard.action = TouchControlActions.KEYBOARD;
        keyboard.width = 80;
        keyboard.height = 30;
        keyboard.rawX = "${margin} * 3 + ${width} * 2";
        keyboard.rawY = "${margin}";
        data.controls.add(keyboard);

        TouchControlData chat = TouchControlData.key("Chat", 84, 0, 0, 80, 30);
        chat.rawX = "${margin} * 2 + ${width}";
        chat.rawY = "${margin}";
        data.controls.add(chat);

        TouchControlData debug = TouchControlData.key("Debug", 292, 0, 0, 80, 30);
        debug.rawX = "${margin}";
        debug.rawY = "${margin} * 2 + ${height}";
        data.controls.add(debug);

        TouchControlData perspective = TouchControlData.key("3rd", 294, 0, 0, 80, 30);
        perspective.rawX = "${margin} * 2 + ${width}";
        perspective.rawY = "${margin} * 2 + ${height}";
        data.controls.add(perspective);

        TouchControlData esc = TouchControlData.key("Esc", 256, 0, 0, 80, 30);
        esc.rawX = "${margin}";
        esc.rawY = "${margin}";
        data.controls.add(esc);

        TouchControlData w = TouchControlData.key("▲", 87, 0, 0, 50, 50);
        w.rawX = "${margin} * 2 + ${width}";
        w.rawY = "${bottom} - ${margin} * 3 - ${height} * 2";
        data.controls.add(w);

        TouchControlData a = TouchControlData.key("◀", 65, 0, 0, 50, 50);
        a.rawX = "${margin}";
        a.rawY = "${bottom} - ${margin} * 2 - ${height}";
        data.controls.add(a);

        TouchControlData s = TouchControlData.key("▼", 83, 0, 0, 50, 50);
        s.rawX = "${margin} * 2 + ${width}";
        s.rawY = "${bottom} - ${margin}";
        data.controls.add(s);

        TouchControlData d = TouchControlData.key("▶", 68, 0, 0, 50, 50);
        d.rawX = "${margin} * 3 + ${width} * 2";
        d.rawY = "${bottom} - ${margin} * 2 - ${height}";
        data.controls.add(d);

        TouchControlData sneak = TouchControlData.key("◇", 340, 0, 0, 50, 50);
        sneak.toggle = true;
        sneak.rawX = "${margin} * 2 + ${width}";
        sneak.rawY = "${bottom} - ${margin} * 4 - ${height} * 3";
        data.controls.add(sneak);

        TouchControlData jump = TouchControlData.key("⬛", 32, 0, 0, 50, 50);
        jump.rawX = "${right} - ${margin} * 2 - ${width}";
        jump.rawY = "${bottom} - ${margin} * 2 - ${height}";
        data.controls.add(jump);

        TouchControlData inventory = TouchControlData.key("Inv", 69, 0, 0, 50, 50);
        inventory.rawX = "${right} - ${margin}";
        inventory.rawY = "${bottom} - ${margin}";
        data.controls.add(inventory);

        TouchControlData hit = TouchControlData.mouse("Hit", 0, 0, 0);
        hit.rawX = "${right} - ${margin} * 3 - ${width} * 2";
        hit.rawY = "${bottom} - ${margin} * 4 - ${height} * 3";
        data.controls.add(hit);

        TouchControlData use = TouchControlData.mouse("Use", 1, 0, 0);
        use.rawX = "${right} - ${margin}";
        use.rawY = "${bottom} - ${margin} * 4 - ${height} * 3";
        data.controls.add(use);

        TouchControlData mouse = new TouchControlData();
        mouse.label = "Mouse";
        mouse.action = TouchControlActions.VIRTUAL_MOUSE;
        mouse.width = 80;
        mouse.height = 30;
        mouse.rawX = "${right}";
        mouse.rawY = "${margin}";
        data.controls.add(mouse);

        return data;
    }
}
