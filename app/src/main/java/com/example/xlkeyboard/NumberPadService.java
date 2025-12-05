package com.example.xlkeyboard;

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;

public class NumberPadService extends InputMethodService {

    private static final String PREFS_NAME = "NumberPadPrefs";
    private static final String KEY_TAB_MODE = "tab_mode_enabled";
    private static final long REPEAT_DELAY = 50; // milliseconds between deletions

    private SharedPreferences prefs;
    private boolean isTabModeEnabled = false;
    private boolean isQwertyMode = false;
    private boolean isShiftEnabled = false;
    private boolean autoCapitalizeNext = false;
    private Button toggleButton;
    private Button shiftButton;
    private Button[] letterButtons;

    private final Handler deleteHandler = new Handler();
    private final Runnable deleteRunnable = new Runnable() {
        @Override
        public void run() {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.deleteSurroundingText(1, 0);
                deleteHandler.postDelayed(this, REPEAT_DELAY);
            }
        }
    };

    @Override
    public View onCreateInputView() {
        if (isQwertyMode) {
            return createQwertyView();
        } else {
            return createNumberPadView();
        }
    }

    private View createNumberPadView() {
        View view = getLayoutInflater().inflate(R.layout.keyboard_view, null);

        // Load saved preference
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isTabModeEnabled = prefs.getBoolean(KEY_TAB_MODE, false);

        // Setup toggle button
        toggleButton = view.findViewById(R.id.btnToggleMode);
        updateToggleButton();
        toggleButton.setOnClickListener(v -> {
            isTabModeEnabled = !isTabModeEnabled;
            prefs.edit().putBoolean(KEY_TAB_MODE, isTabModeEnabled).apply();
            updateToggleButton();
        });

        // Setup ABC button to switch to QWERTY
        Button abcButton = view.findViewById(R.id.btnABC);
        abcButton.setOnClickListener(v -> {
            isQwertyMode = true;
            setInputView(onCreateInputView());
        });

        // Set click listeners for all buttons
        setupButton(view, R.id.btn0, KeyEvent.KEYCODE_0);
        setupButton(view, R.id.btn1, KeyEvent.KEYCODE_1);
        setupButton(view, R.id.btn2, KeyEvent.KEYCODE_2);
        setupButton(view, R.id.btn3, KeyEvent.KEYCODE_3);
        setupButton(view, R.id.btn4, KeyEvent.KEYCODE_4);
        setupButton(view, R.id.btn5, KeyEvent.KEYCODE_5);
        setupButton(view, R.id.btn6, KeyEvent.KEYCODE_6);
        setupButton(view, R.id.btn7, KeyEvent.KEYCODE_7);
        setupButton(view, R.id.btn8, KeyEvent.KEYCODE_8);
        setupButton(view, R.id.btn9, KeyEvent.KEYCODE_9);

        setupButton(view, R.id.btnUp, KeyEvent.KEYCODE_DPAD_UP);
        setupButton(view, R.id.btnEnter, KeyEvent.KEYCODE_ENTER);

        // Special handling for left and right arrows
        setupArrowButton(view, R.id.btnLeft, true); // true = left arrow
        setupArrowButton(view, R.id.btnRight, false); // false = right arrow

        setupBackspaceButton(view, R.id.btnBack);
        setupButton(view, R.id.btnDot, KeyEvent.KEYCODE_PERIOD);

        return view;
    }

    private View createQwertyView() {
        isShiftEnabled = true;
        View view = getLayoutInflater().inflate(R.layout.qwerty_keyboard_view, null);

        // Setup toggle button for Tab mode (same as number pad)
        Button toggleButtonQwerty = view.findViewById(R.id.btnToggleModeQwerty);
        if (isTabModeEnabled) {
            toggleButtonQwerty.setText(R.string.key_toggle_tab);
            toggleButtonQwerty.setBackgroundTintList(
                    getResources().getColorStateList(R.color.toggle_active, null));
        } else {
            toggleButtonQwerty.setText(R.string.key_toggle_arrow);
            toggleButtonQwerty.setBackgroundTintList(
                    getResources().getColorStateList(R.color.toggle_inactive, null));
        }
        toggleButtonQwerty.setOnClickListener(v -> {
            isTabModeEnabled = !isTabModeEnabled;
            prefs.edit().putBoolean(KEY_TAB_MODE, isTabModeEnabled).apply();
            if (isTabModeEnabled) {
                toggleButtonQwerty.setText(R.string.key_toggle_tab);
                toggleButtonQwerty.setBackgroundTintList(
                        getResources().getColorStateList(R.color.toggle_active, null));
            } else {
                toggleButtonQwerty.setText(R.string.key_toggle_arrow);
                toggleButtonQwerty.setBackgroundTintList(
                        getResources().getColorStateList(R.color.toggle_inactive, null));
            }
        });

        // Setup 123 button to switch back to number pad
        Button btn123 = view.findViewById(R.id.btn123);
        btn123.setOnClickListener(v -> {
            isQwertyMode = false;
            setInputView(onCreateInputView());
        });

        // Get all letter button references for dynamic case switching
        letterButtons = new Button[26];
        letterButtons[0] = view.findViewById(R.id.btnQ);
        letterButtons[1] = view.findViewById(R.id.btnW);
        letterButtons[2] = view.findViewById(R.id.btnE);
        letterButtons[3] = view.findViewById(R.id.btnR);
        letterButtons[4] = view.findViewById(R.id.btnT);
        letterButtons[5] = view.findViewById(R.id.btnY);
        letterButtons[6] = view.findViewById(R.id.btnU);
        letterButtons[7] = view.findViewById(R.id.btnI);
        letterButtons[8] = view.findViewById(R.id.btnO);
        letterButtons[9] = view.findViewById(R.id.btnP);
        letterButtons[10] = view.findViewById(R.id.btnA);
        letterButtons[11] = view.findViewById(R.id.btnS);
        letterButtons[12] = view.findViewById(R.id.btnD);
        letterButtons[13] = view.findViewById(R.id.btnF);
        letterButtons[14] = view.findViewById(R.id.btnG);
        letterButtons[15] = view.findViewById(R.id.btnH);
        letterButtons[16] = view.findViewById(R.id.btnJ);
        letterButtons[17] = view.findViewById(R.id.btnK);
        letterButtons[18] = view.findViewById(R.id.btnL);
        letterButtons[19] = view.findViewById(R.id.btnZ);
        letterButtons[20] = view.findViewById(R.id.btnX);
        letterButtons[21] = view.findViewById(R.id.btnC);
        letterButtons[22] = view.findViewById(R.id.btnV);
        letterButtons[23] = view.findViewById(R.id.btnB);
        letterButtons[24] = view.findViewById(R.id.btnN);
        letterButtons[25] = view.findViewById(R.id.btnM);

        String[] letters = { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
                "a", "s", "d", "f", "g", "h", "j", "k", "l",
                "z", "x", "c", "v", "b", "n", "m" };

        // Setup shift button with dynamic letter case switching
        shiftButton = view.findViewById(R.id.btnShift);
        updateShiftButton();
        shiftButton.setOnClickListener(v -> {
            isShiftEnabled = !isShiftEnabled;
            updateShiftButton();
            // Update all letter button texts
            for (int i = 0; i < letterButtons.length; i++) {
                letterButtons[i].setText(isShiftEnabled ? letters[i].toUpperCase() : letters[i]);
            }
        });

        // Setup letter buttons (QWERTY row 1)
        setupLetterButton(view, R.id.btnQ, "q");
        setupLetterButton(view, R.id.btnW, "w");
        setupLetterButton(view, R.id.btnE, "e");
        setupLetterButton(view, R.id.btnR, "r");
        setupLetterButton(view, R.id.btnT, "t");
        setupLetterButton(view, R.id.btnY, "y");
        setupLetterButton(view, R.id.btnU, "u");
        setupLetterButton(view, R.id.btnI, "i");
        setupLetterButton(view, R.id.btnO, "o");
        setupLetterButton(view, R.id.btnP, "p");

        // Setup letter buttons (QWERTY row 2)
        setupLetterButton(view, R.id.btnA, "a");
        setupLetterButton(view, R.id.btnS, "s");
        setupLetterButton(view, R.id.btnD, "d");
        setupLetterButton(view, R.id.btnF, "f");
        setupLetterButton(view, R.id.btnG, "g");
        setupLetterButton(view, R.id.btnH, "h");
        setupLetterButton(view, R.id.btnJ, "j");
        setupLetterButton(view, R.id.btnK, "k");
        setupLetterButton(view, R.id.btnL, "l");

        // Setup letter buttons (QWERTY row 3)
        setupLetterButton(view, R.id.btnZ, "z");
        setupLetterButton(view, R.id.btnX, "x");
        setupLetterButton(view, R.id.btnC, "c");
        setupLetterButton(view, R.id.btnV, "v");
        setupLetterButton(view, R.id.btnB, "b");
        setupLetterButton(view, R.id.btnN, "n");
        setupLetterButton(view, R.id.btnM, "m");

        // Setup special buttons
        setupBackspaceButton(view, R.id.btnBackQwerty);
        setupTextButton(view, R.id.btnDotQwerty, ".");
        setupTextButton(view, R.id.btnSpaceQwerty, " ");
        setupTextButton(view, R.id.btnCommaQwerty, ",");
        setupButton(view, R.id.btnEnterQwerty, KeyEvent.KEYCODE_ENTER);

        return view;
    }

    private void updateToggleButton() {
        if (toggleButton != null) {
            if (isTabModeEnabled) {
                toggleButton.setText(R.string.key_toggle_tab);
                toggleButton.setBackgroundTintList(
                        getResources().getColorStateList(R.color.toggle_active, null));
            } else {
                toggleButton.setText(R.string.key_toggle_arrow);
                toggleButton.setBackgroundTintList(
                        getResources().getColorStateList(R.color.toggle_inactive, null));
            }
        }
    }

    private void updateShiftButton() {
        if (shiftButton != null) {
            if (isShiftEnabled) {
                shiftButton.setBackgroundTintList(
                        getResources().getColorStateList(R.color.toggle_active, null));
            } else {
                shiftButton.setBackgroundTintList(
                        getResources().getColorStateList(R.color.toggle_inactive, null));
            }
        }
    }

    private void setupBackspaceButton(View parent, int id) {
        Button button = parent.findViewById(id);
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Single delete on press
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.deleteSurroundingText(1, 0);
                    }
                    // Start repeating after initial delay
                    deleteHandler.postDelayed(deleteRunnable, 500);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Stop repeating
                    deleteHandler.removeCallbacks(deleteRunnable);
                    return true;
            }
            return false;
        });
    }

    private void setupButton(View parent, int id, int keyCode) {
        Button button = parent.findViewById(id);
        button.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null)
                return;

            long now = System.currentTimeMillis();
            ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
            ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
        });
    }

    private void setupLetterButton(View parent, int id, String letter) {
        Button button = parent.findViewById(id);
        button.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null)
                return;

            // Check if we should auto-capitalize
            boolean shouldCapitalize = isShiftEnabled || autoCapitalizeNext;
            String text = shouldCapitalize ? letter.toUpperCase() : letter;
            ic.commitText(text, 1);

            // Auto-disable shift and auto-capitalize after typing one character
            if (isShiftEnabled) {
                isShiftEnabled = false;
                updateShiftButton();
            }
            if (autoCapitalizeNext) {
                autoCapitalizeNext = false;
                updateLetterButtonsCase();
            }
        });
    }

    private void setupTextButton(View parent, int id, String text) {
        Button button = parent.findViewById(id);
        button.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null)
                return;
            ic.commitText(text, 1);

            // Enable auto-capitalization after period
            if (text.equals(".")) {
                autoCapitalizeNext = true;
                updateLetterButtonsCase();
            } else if (!text.equals(" ")) {
                // Disable auto-capitalization for other non-space characters
                autoCapitalizeNext = false;
                updateLetterButtonsCase();
            }
        });
    }

    private void setupArrowButton(View parent, int id, boolean isLeft) {
        Button button = parent.findViewById(id);
        button.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null)
                return;

            long now = System.currentTimeMillis();

            if (isTabModeEnabled) {
                // Tab mode: left = Shift+Tab, right = Tab
                if (isLeft) {
                    // Shift+Tab
                    ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_TAB, 0, KeyEvent.META_SHIFT_ON));
                    ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_TAB, 0, KeyEvent.META_SHIFT_ON));
                } else {
                    // Tab
                    ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_TAB, 0));
                    ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_TAB, 0));
                }
            } else {
                // Arrow mode: normal arrow keys
                int keyCode = isLeft ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
                ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
            }
        });
    }

    private void updateLetterButtonsCase() {
        if (letterButtons == null || !isQwertyMode) {
            return;
        }

        String[] letters = { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
                "a", "s", "d", "f", "g", "h", "j", "k", "l",
                "z", "x", "c", "v", "b", "n", "m" };

        boolean shouldShowUppercase = isShiftEnabled || autoCapitalizeNext;
        for (int i = 0; i < letterButtons.length && i < letters.length; i++) {
            if (letterButtons[i] != null) {
                letterButtons[i].setText(shouldShowUppercase ? letters[i].toUpperCase() : letters[i]);
            }
        }
    }
}
