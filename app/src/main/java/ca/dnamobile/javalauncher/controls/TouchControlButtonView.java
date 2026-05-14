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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.lwjgl.glfw.CallbackBridge;

import ca.dnamobile.javalauncher.feature.log.Logging;

/** A single touch control button. */
@SuppressLint("ViewConstructor")
final class TouchControlButtonView extends TextView {
    interface Listener {
        void onChanged();
        void onMoveStarted(@NonNull TouchControlButtonView view, @NonNull TouchControlData data);
        void onMoveRequested(
                @NonNull TouchControlButtonView view,
                @NonNull TouchControlData data,
                float proposedX,
                float proposedY
        );
        void onEditRequested(@NonNull TouchControlButtonView view, @NonNull TouchControlData data);
        void onMenuRequested();
        void onToggleControlsRequested();
        void onKeySenderKeyboardRequested();
    }

    private static final String TAG = "TouchButton";

    private static final int GLFW_KEY_W = 87;
    private static final int GLFW_KEY_A = 65;
    private static final int GLFW_KEY_S = 83;
    private static final int GLFW_KEY_D = 68;
    private static final int GLFW_KEY_LEFT_CONTROL = 341;
    private static final int GLFW_KEY_T = 84;
    private static final int GLFW_KEY_SLASH = 47;

    private static final int GLFW_MOUSE_BUTTON_LEFT = 0;
    private static final int GLFW_MOUSE_BUTTON_RIGHT = 1;
    private static final int GLFW_MOUSE_BUTTON_MIDDLE = 2;

    private final TouchControlData data;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int touchSlop;

    private final Paint joystickBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint joystickStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint joystickKnobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint joystickGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean editMode;
    private boolean pressedState;
    private boolean editLongPressTriggered;
    private boolean editDragging;
    private float touchOffsetX;
    private float touchOffsetY;
    private float downRawX;
    private float downRawY;
    private Runnable editLongPressRunnable;

    private boolean joystickForwardLockDown;
    private boolean joystickWDown;
    private boolean joystickADown;
    private boolean joystickSDown;
    private boolean joystickDDown;
    private float joystickCenterX;
    private float joystickCenterY;
    private float joystickKnobX;
    private float joystickKnobY;

    TouchControlButtonView(@NonNull Context context, @NonNull TouchControlData data, @NonNull Listener listener) {
        super(context);
        this.data = data;
        this.listener = listener;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setGravity(Gravity.CENTER);
        setTextColor(Color.WHITE);
        setTextSize(13f);
        setIncludeFontPadding(false);
        setSingleLine(false);
        setAllCaps(false);
        setText(data.label);
        setBackground(makeBackground(false));
        setAlpha(resolvedDisplayAlpha());
        setLongClickable(true);
        setWillNotDraw(false);
        setupJoystickPaints();
        resetJoystickKnob();
    }

    void setEditMode(boolean editMode) {
        this.editMode = editMode;
        refreshVisualState();
    }

    void refreshVisualState() {
        setText(data.label);
        setBackground(makeBackground(editMode));
        setAlpha(resolvedDisplayAlpha());
        invalidate();
    }

    @NonNull
    TouchControlData getData() {
        return data;
    }

    private float resolvedDisplayAlpha() {
        float localOpacity = Math.max(0f, Math.min(1f, data.opacity));
        float globalOpacity = Math.max(0f, Math.min(1f, ControlsPreferences.getGlobalOpacity(getContext())));
        float alpha = localOpacity * globalOpacity;
        return editMode ? Math.max(0.25f, alpha) : alpha;
    }

    private void setupJoystickPaints() {
        joystickBasePaint.setColor(0x33000000);
        joystickBasePaint.setStyle(Paint.Style.FILL);
        joystickStrokePaint.setColor(0xAAFFFFFF);
        joystickStrokePaint.setStyle(Paint.Style.STROKE);
        joystickStrokePaint.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
        joystickKnobPaint.setColor(0x99FFFFFF);
        joystickKnobPaint.setStyle(Paint.Style.FILL);
        joystickGuidePaint.setColor(0x66FFFFFF);
        joystickGuidePaint.setStyle(Paint.Style.STROKE);
        joystickGuidePaint.setStrokeWidth(1.25f * getResources().getDisplayMetrics().density);
    }

    private void resetJoystickKnob() {
        joystickKnobX = getWidth() > 0 ? getWidth() / 2f : 0f;
        joystickKnobY = getHeight() > 0 ? getHeight() / 2f : 0f;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (TouchControlActions.JOYSTICK.equals(data.action) && !pressedState) {
            joystickKnobX = w / 2f;
            joystickKnobY = h / 2f;
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (TouchControlActions.JOYSTICK.equals(data.action)) drawJoystick(canvas);
        super.onDraw(canvas);
    }

    private void drawJoystick(@NonNull Canvas canvas) {
        float width = Math.max(1f, getWidth());
        float height = Math.max(1f, getHeight());
        float centerX = width / 2f;
        float centerY = height / 2f;
        float outerRadius = Math.min(width, height) * 0.48f;
        float guideRadius = Math.min(width, height) * 0.28f;
        float knobRadius = Math.max(10f * getResources().getDisplayMetrics().density, Math.min(width, height) * 0.18f);
        canvas.drawCircle(centerX, centerY, outerRadius, joystickBasePaint);
        canvas.drawCircle(centerX, centerY, outerRadius, joystickStrokePaint);
        canvas.drawCircle(centerX, centerY, guideRadius, joystickGuidePaint);
        float knobX = joystickKnobX > 0f ? joystickKnobX : centerX;
        float knobY = joystickKnobY > 0f ? joystickKnobY : centerY;
        canvas.drawCircle(knobX, knobY, knobRadius, joystickKnobPaint);
        canvas.drawCircle(knobX, knobY, knobRadius, joystickStrokePaint);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (editMode) return handleEditTouch(event);
        return handleGameTouch(event);
    }

    private boolean handleEditTouch(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                requestParentDisallowIntercept(true);
                setPressed(true);
                editLongPressTriggered = false;
                editDragging = false;
                touchOffsetX = event.getRawX() - getX();
                touchOffsetY = event.getRawY() - getY();
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                scheduleEditLongPress();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (editLongPressTriggered) return true;
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!editDragging && ((dx * dx) + (dy * dy)) > (touchSlop * touchSlop)) {
                    editDragging = true;
                    cancelEditLongPress();
                    listener.onMoveStarted(this, data);
                }
                if (editDragging) {
                    listener.onMoveRequested(this, data, Math.max(0f, event.getRawX() - touchOffsetX), Math.max(0f, event.getRawY() - touchOffsetY));
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelEditLongPress();
                setPressed(false);
                requestParentDisallowIntercept(false);
                if (editDragging && !editLongPressTriggered) {
                    listener.onChanged();
                }
                return true;
            case MotionEvent.ACTION_UP:
                cancelEditLongPress();
                setPressed(false);
                requestParentDisallowIntercept(false);
                if (!editLongPressTriggered) {
                    performClick();
                    listener.onChanged();
                }
                return true;
            default:
                return true;
        }
    }

    private void scheduleEditLongPress() {
        cancelEditLongPress();
        editLongPressRunnable = () -> {
            editLongPressRunnable = null;
            if (!isAttachedToWindow() || !editMode) return;
            editLongPressTriggered = true;
            setPressed(false);
            requestParentDisallowIntercept(false);
            listener.onEditRequested(this, data);
        };
        mainHandler.postDelayed(editLongPressRunnable, ViewConfiguration.getLongPressTimeout());
    }

    private void cancelEditLongPress() {
        if (editLongPressRunnable != null) {
            mainHandler.removeCallbacks(editLongPressRunnable);
            editLongPressRunnable = null;
        }
    }

    private void requestParentDisallowIntercept(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    private boolean handleGameTouch(@NonNull MotionEvent event) {
        if (TouchControlActions.JOYSTICK.equals(data.action)) return handleJoystickTouch(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (TouchControlActions.MENU.equals(data.action)) {
                    listener.onMenuRequested();
                    performClick();
                    return true;
                }
                if (TouchControlActions.TOGGLE_CONTROLS.equals(data.action)) {
                    listener.onToggleControlsRequested();
                    performClick();
                    return true;
                }
                if (TouchControlActions.VIRTUAL_MOUSE.equals(data.action)) {
                    toggleVirtualMouse();
                    performClick();
                    return true;
                }
                if (TouchControlActions.KEY_SENDER_KEYBOARD.equals(data.action)) {
                    listener.onKeySenderKeyboardRequested();
                    performClick();
                    return true;
                }
                if (data.toggle) {
                    pressedState = !pressedState;
                    send(pressedState);
                } else {
                    pressedState = true;
                    send(true);
                }
                setActivated(pressedState);
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!data.toggle && pressedState) {
                    pressedState = false;
                    send(false);
                    setActivated(false);
                }
                performClick();
                return true;
            default:
                return true;
        }
    }

    private boolean handleJoystickTouch(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedState = true;
                setActivated(true);
                joystickCenterX = data.joystickAbsolute ? event.getX() : getWidth() / 2f;
                joystickCenterY = data.joystickAbsolute ? event.getY() : getHeight() / 2f;
                updateJoystick(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                updateJoystick(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                releaseJoystick();
                pressedState = false;
                setActivated(false);
                performClick();
                return true;
            default:
                return true;
        }
    }

    private void updateJoystick(float x, float y) {
        float dx = x - joystickCenterX;
        float dy = y - joystickCenterY;
        float size = Math.max(1f, Math.min(getWidth(), getHeight()));
        float maxKnobTravel = Math.max(1f, size * 0.43f);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float unitX = distance > 0f ? dx / distance : 0f;
        float unitY = distance > 0f ? dy / distance : 0f;
        float knobDistance = Math.min(distance, maxKnobTravel);
        float clampedDx = unitX * knobDistance;
        float clampedDy = unitY * knobDistance;
        joystickKnobX = joystickCenterX + clampedDx;
        joystickKnobY = joystickCenterY + clampedDy;
        invalidate();
        float deadzone = Math.max(touchSlop, maxKnobTravel * 0.16f);
        setJoystickKeyStates(clampedDy < -deadzone, clampedDx < -deadzone, clampedDy > deadzone, clampedDx > deadzone);
        boolean shouldForwardLock = data.joystickForwardLock && clampedDy < -deadzone && distance > (maxKnobTravel * 0.88f);
        if (shouldForwardLock != joystickForwardLockDown) {
            joystickForwardLockDown = shouldForwardLock;
            sendKey(GLFW_KEY_LEFT_CONTROL, shouldForwardLock);
        }
    }

    private void setJoystickKeyStates(boolean wDown, boolean aDown, boolean sDown, boolean dDown) {
        if (joystickWDown != wDown) { joystickWDown = wDown; sendKey(GLFW_KEY_W, wDown); }
        if (joystickADown != aDown) { joystickADown = aDown; sendKey(GLFW_KEY_A, aDown); }
        if (joystickSDown != sDown) { joystickSDown = sDown; sendKey(GLFW_KEY_S, sDown); }
        if (joystickDDown != dDown) { joystickDDown = dDown; sendKey(GLFW_KEY_D, dDown); }
    }

    private void releaseJoystick() {
        setJoystickKeyStates(false, false, false, false);
        resetJoystickKnob();
        if (joystickForwardLockDown) {
            joystickForwardLockDown = false;
            sendKey(GLFW_KEY_LEFT_CONTROL, false);
        }
    }

    void releaseInputState() {
        cancelEditLongPress();
        releaseJoystick();
        if (pressedState) {
            pressedState = false;
            send(false);
        }
        editLongPressTriggered = false;
        editDragging = false;
        setPressed(false);
        setActivated(false);
    }

    private void send(boolean down) {
        try {
            CallbackBridge.setInputReady(true);
            if (TouchControlActions.KEY.equals(data.action)) {
                for (int binding : data.normalizedKeyCodes()) {
                    sendSlotBinding(binding, down);
                }
                return;
            }
            if (TouchControlActions.MOUSE.equals(data.action)) {
                CallbackBridge.sendMouseButton(data.mouseButton, down);
                return;
            }
            if (TouchControlActions.SCROLL.equals(data.action)) {
                if (!down) CallbackBridge.sendScroll(0d, data.scrollY);
                return;
            }
            if (TouchControlActions.KEYBOARD.equals(data.action)) {
                if (down) TouchKeyboardHelper.showKeyboard(this);
                return;
            }
            if (TouchControlActions.KEY_SENDER_KEYBOARD.equals(data.action)) {
                if (down) listener.onKeySenderKeyboardRequested();
            }
        } catch (Throwable throwable) {
            Logging.e(TAG, "Unable to send touch control input", throwable);
        }
    }

    private void sendSlotBinding(int binding, boolean down) {
        if (binding == 0) return;

        switch (binding) {
            case TouchControlData.SPECIAL_MOUSE_LEFT:
                CallbackBridge.sendMouseButton(GLFW_MOUSE_BUTTON_LEFT, down);
                return;
            case TouchControlData.SPECIAL_MOUSE_RIGHT:
                CallbackBridge.sendMouseButton(GLFW_MOUSE_BUTTON_RIGHT, down);
                return;
            case TouchControlData.SPECIAL_MOUSE_MIDDLE:
                CallbackBridge.sendMouseButton(GLFW_MOUSE_BUTTON_MIDDLE, down);
                return;
            case TouchControlData.SPECIAL_SCROLL_UP:
                if (!down) CallbackBridge.sendScroll(0d, 1d);
                return;
            case TouchControlData.SPECIAL_SCROLL_DOWN:
                if (!down) CallbackBridge.sendScroll(0d, -1d);
                return;
            case TouchControlData.SPECIAL_KEYBOARD:
                if (down) TouchKeyboardHelper.showKeyboard(this);
                return;
            case TouchControlData.SPECIAL_KEY_SENDER_KEYBOARD:
                if (down) listener.onKeySenderKeyboardRequested();
                return;
            case TouchControlData.SPECIAL_MENU:
                if (down) listener.onMenuRequested();
                return;
            case TouchControlData.SPECIAL_TOGGLE_CONTROLS:
                if (down) listener.onToggleControlsRequested();
                return;
            case TouchControlData.SPECIAL_VIRTUAL_MOUSE:
                if (down) toggleVirtualMouse();
                return;
            default:
                if (binding > 0) sendKey(binding, down);
        }
    }

    private void toggleVirtualMouse() {
        boolean enabled = !ControlsPreferences.isVirtualMouseEnabled(getContext());
        ControlsPreferences.setVirtualMouseEnabled(getContext(), enabled);
        Toast.makeText(getContext(), enabled ? "Virtual cursor shown" : "Virtual cursor hidden", Toast.LENGTH_SHORT).show();
    }

    private void sendKey(int keyCode, boolean down) {
        CallbackBridge.setInputReady(true);
        if (down && isChatOpenKey(keyCode)) {
            TouchKeyboardHelper.markChatKeyPressed();
        }
        CallbackBridge.sendKeyPress(keyCode, CallbackBridge.getCurrentMods(), down);
        CallbackBridge.setModifiers(keyCode, down);
    }

    private static boolean isChatOpenKey(int keyCode) {
        return keyCode == GLFW_KEY_T || keyCode == GLFW_KEY_SLASH;
    }

    private GradientDrawable makeBackground(boolean editing) {
        GradientDrawable drawable = new GradientDrawable();
        boolean joystick = TouchControlActions.JOYSTICK.equals(data.action);
        drawable.setShape(joystick ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
        drawable.setColor(editing ? 0x663F51B5 : data.backgroundColor);
        int strokePx = Math.max(editing ? 3 : 0, Math.round(Math.max(0f, data.strokeWidth) * getResources().getDisplayMetrics().density));
        int strokeColor = editing ? 0xFFFFFFFF : data.strokeColor;
        if (strokePx > 0) drawable.setStroke(strokePx, strokeColor);
        float radius = joystick ? 9999f : Math.max(0f, data.cornerRadius) * getResources().getDisplayMetrics().density;
        drawable.setCornerRadius(radius);
        return drawable;
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseInputState();
        super.onDetachedFromWindow();
    }
}
