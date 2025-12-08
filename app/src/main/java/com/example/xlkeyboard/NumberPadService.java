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
    private boolean isCapsLock = false;
    private long lastShiftPressTime = 0;
    private static final long DOUBLE_TAP_DELAY = 300; // milliseconds

    private boolean autoCapitalizeNext = false;
    private Button toggleButton;
    private Button shiftButton;
    private Button[] letterButtons;

    String[] letters = { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
            "a", "s", "d", "f", "g", "h", "j", "k", "l",
            "z", "x", "c", "v", "b", "n", "m" };

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

        // Setup SYM button
        Button btnSym = view.findViewById(R.id.btnsym);
        btnSym.setOnClickListener(v -> {
            setInputView(createSymbolView());
        });

        // Setup toggle button
        setupToggleButton(view, R.id.btnToggleMode);

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
        View view = getLayoutInflater().inflate(R.layout.qwerty_keyboard_view, null);

        // Setup SYM button for QWERTY view
        Button btnSym = view.findViewById(R.id.btnsym);
        btnSym.setOnClickListener(v -> {
            setInputView(createSymbolView());
        });

        // Setup toggle button for Tab mode
        setupToggleButton(view, R.id.btnToggleModeQwerty);

        // Initialize letter buttons array
        letterButtons = new Button[26];

        // Setup 123 button to switch back to number pad
        Button btn123 = view.findViewById(R.id.btn123);
        btn123.setOnClickListener(v -> {
            isQwertyMode = false;
            setInputView(onCreateInputView());
        });

        // Get all letter button references for dynamic case switching

        // Setup shift button with dynamic letter case switching
        shiftButton = view.findViewById(R.id.btnShift);
        updateShiftButton();
        // explaination: This function updates the shift button based on the shift
        // state.
        shiftButton.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShiftPressTime < DOUBLE_TAP_DELAY) {
                // Double tap detected - Enable Caps Lock
                isCapsLock = true;
                isShiftEnabled = true;
            } else {
                // Single tap
                if (isCapsLock) {
                    // Turn off Caps Lock
                    isCapsLock = false;
                    isShiftEnabled = false;
                } else {
                    // Toggle Shift
                    isShiftEnabled = !isShiftEnabled;
                }
            }
            lastShiftPressTime = currentTime;

            updateShiftButton();
            updateLetterButtonsCase();
        });
        // explaination: This function updates the shift button based on the shift
        // state.
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

    private void setupToggleButton(View parent, int id) {
        Button btn = parent.findViewById(id);
        updateToggleButtonState(btn);
        btn.setOnClickListener(v -> {
            isTabModeEnabled = !isTabModeEnabled;
            prefs.edit().putBoolean(KEY_TAB_MODE, isTabModeEnabled).apply();
            updateToggleButtonState(btn);

            // Sync the main toggle button reference if we are in NumberPad mode
            if (id == R.id.btnToggleMode) {
                updateToggleButtonState(toggleButton);
                // Actually this listener is attached to the view's button, which IS
                // toggleButton in this case.
                // So the above line is redundant but harmless.
            }
        });

        if (id == R.id.btnToggleMode) {
            this.toggleButton = btn;
        }
    }

    private void updateToggleButton() {
        if (toggleButton != null) {
            updateToggleButtonState(toggleButton);
        }
    }

    private void updateToggleButtonState(Button btn) {
        if (btn == null)
            return;
        if (isTabModeEnabled) {
            btn.setText(R.string.key_toggle_tab);
            btn.setBackgroundTintList(
                    getResources().getColorStateList(R.color.toggle_active, null));
        } else {
            btn.setText(R.string.key_toggle_arrow);
            btn.setBackgroundTintList(
                    getResources().getColorStateList(R.color.toggle_inactive, null));
        }
    }

    private void updateShiftButton() {
        if (shiftButton != null) {
            if (isShiftEnabled) {
                shiftButton.setBackgroundTintList(
                        getResources().getColorStateList(R.color.toggle_active, null));
                // Optional: Change icon or distinct color for Caps Lock
                if (isCapsLock) {
                    // Keep active color or maybe add a visual indicator if possible,
                    // for now relying on the active tint.
                }
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

        // Populate letterButtons array dynamically
        int index = -1;
        for (int i = 0; i < letters.length; i++) {
            if (letters[i].equalsIgnoreCase(letter)) {
                index = i;
                break;
            }
        }
        if (index != -1 && letterButtons != null) {
            letterButtons[index] = button;
        }

        button.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null)
                return;

            // Check if we should auto-capitalize
            boolean shouldCapitalize = isShiftEnabled || autoCapitalizeNext;
            String text = shouldCapitalize ? letter.toUpperCase() : letter;
            ic.commitText(text, 1);

            // Auto-disable shift and auto-capitalize after typing one character
            if (isShiftEnabled && !isCapsLock) {
                isShiftEnabled = false;
                updateShiftButton();
                updateLetterButtonsCase();
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

            } else if (!text.equals(" ")) {
                // Disable auto-capitalization for other non-space characters
                autoCapitalizeNext = false;

            }
            updateLetterButtonsCase();
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

    // explaination: This function updates the case of the letter buttons based on
    // the shift state and auto-capitalize state.
    private void updateLetterButtonsCase() {
        if (letterButtons == null || !isQwertyMode) {
            return;
        }

        boolean shouldShowUppercase = isShiftEnabled || autoCapitalizeNext;
        for (int i = 0; i < letterButtons.length && i < letters.length; i++) {
            if (letterButtons[i] != null) {
                letterButtons[i].setText(shouldShowUppercase ? letters[i].toUpperCase() : letters[i]);
            }
        }
    }

    private View createSymbolView() {
        View view = getLayoutInflater().inflate(R.layout.symbol_keyboard_view, null);

        // Row 1 buttons
        setupTextButton(view, R.id.btnSym1, "1");
        setupTextButton(view, R.id.btnSym2, "2");
        setupTextButton(view, R.id.btnSym3, "3");
        setupTextButton(view, R.id.btnSym4, "4");
        setupTextButton(view, R.id.btnSym5, "5");
        setupTextButton(view, R.id.btnSym6, "6");
        setupTextButton(view, R.id.btnSym7, "7");
        setupTextButton(view, R.id.btnSym8, "8");
        setupTextButton(view, R.id.btnSym9, "9");
        setupTextButton(view, R.id.btnSym0, "0");

        // Row 2 buttons
        setupTextButton(view, R.id.btnSymAt, "@");
        setupTextButton(view, R.id.btnSymHash, "#");
        setupTextButton(view, R.id.btnSymDollar, "$");
        setupTextButton(view, R.id.btnSymPercent, "%");
        setupTextButton(view, R.id.btnSymAmpersand, "&");
        setupTextButton(view, R.id.btnSymMinus, "-");
        setupTextButton(view, R.id.btnSymPlus, "+");
        setupTextButton(view, R.id.btnSymParenLeft, "(");
        setupTextButton(view, R.id.btnSymParenRight, ")");
        setupTextButton(view, R.id.btnSymSlash, "/");

        // Row 3 buttons
        setupTextButton(view, R.id.btnSymEquals, "=");
        setupTextButton(view, R.id.btnSymAsterisk, "*");
        setupTextButton(view, R.id.btnSymQuoteDouble, "\"");
        setupTextButton(view, R.id.btnSymQuoteSingle, "'");
        setupTextButton(view, R.id.btnSymColon, ":");
        setupTextButton(view, R.id.btnSymSemicolon, ";");
        setupTextButton(view, R.id.btnSymExclamation, "!");
        setupTextButton(view, R.id.btnSymQuestion, "?");
        setupBackspaceButton(view, R.id.btnBackSym);

        // Row 4 buttons
        Button btnABC = view.findViewById(R.id.btnABC_Sym);
        btnABC.setOnClickListener(v -> {
            isQwertyMode = true;
            setInputView(createQwertyView());
        });
        setupTextButton(view, R.id.btnSpaceSym, " ");
        setupTextButton(view, R.id.btnCommaSym, ",");
        setupButton(view, R.id.btnEnterSym, KeyEvent.KEYCODE_ENTER);

        return view;
    }
}
