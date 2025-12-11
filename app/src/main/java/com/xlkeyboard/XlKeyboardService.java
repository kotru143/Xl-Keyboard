package com.xlkeyboard;

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;

public class XlKeyboardService extends InputMethodService {

    private static final String PREFS_NAME = "NumberPadPrefs";
    private static final String KEY_TAB_MODE = "tab_mode_enabled";
    private static final long REPEAT_DELAY = 50;
    private static final long DOUBLE_TAP_DELAY = 300;

    private SharedPreferences prefs;
    private boolean isTabModeEnabled = false;
    private boolean isQwertyMode = false;
    private boolean isShiftEnabled = false;
    private boolean isCapsLock = false;
    private long lastShiftPressTime = 0;
    private boolean autoCapitalizeNext = false;

    private Button toggleButton;
    private Button shiftButton;
    private Button[] letterButtons;

    private static final String[] LETTERS = {
            "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
            "a", "s", "d", "f", "g", "h", "j", "k", "l",
            "z", "x", "c", "v", "b", "n", "m"
    };

    private static final int[] LETTER_BUTTON_IDS = {
            R.id.btnQ, R.id.btnW, R.id.btnE, R.id.btnR, R.id.btnT,
            R.id.btnY, R.id.btnU, R.id.btnI, R.id.btnO, R.id.btnP,
            R.id.btnA, R.id.btnS, R.id.btnD, R.id.btnF, R.id.btnG,
            R.id.btnH, R.id.btnJ, R.id.btnK, R.id.btnL,
            R.id.btnZ, R.id.btnX, R.id.btnC, R.id.btnV, R.id.btnB,
            R.id.btnN, R.id.btnM
    };

    private static final int[] NUMBER_BUTTON_IDS = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
    };

    private static final int[] NUMBER_KEY_CODES = {
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9
    };

    private static final int[] SYMBOL_ROW1_IDS = {
            R.id.btnSym1, R.id.btnSym2, R.id.btnSym3, R.id.btnSym4, R.id.btnSym5,
            R.id.btnSym6, R.id.btnSym7, R.id.btnSym8, R.id.btnSym9, R.id.btnSym0
    };
    private static final String[] SYMBOL_ROW1_CHARS = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"
    };

    private static final int[] SYMBOL_ROW2_IDS = {
            R.id.btnSymAt, R.id.btnSymHash, R.id.btnSymDollar, R.id.btnSymPercent,
            R.id.btnSymAmpersand, R.id.btnSymMinus, R.id.btnSymPlus,
            R.id.btnSymParenLeft, R.id.btnSymParenRight, R.id.btnSymSlash
    };
    private static final String[] SYMBOL_ROW2_CHARS = {
            "@", "#", "₹", "%", "&", "-", "+", "(", ")", "/"
    };

    private static final int[] SYMBOL_ROW3_IDS = {
            R.id.btnSymEquals, R.id.btnSymAsterisk, R.id.btnSymQuoteDouble,
            R.id.btnSymQuoteSingle, R.id.btnSymColon, R.id.btnSymSemicolon,
            R.id.btnSymExclamation, R.id.btnSymQuestion
    };
    private static final String[] SYMBOL_ROW3_CHARS = {
            "=", "*", "\"", "'", ":", ";", "!", "?"
    };

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

    // ==================== View Creation ====================

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

        // Set click listeners for all number buttons
        for (int i = 0; i < NUMBER_BUTTON_IDS.length; i++) {
            setupButton(view, NUMBER_BUTTON_IDS[i], NUMBER_KEY_CODES[i]);
        }

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
        // Double-tap enables Caps Lock, single tap toggles shift
        shiftButton = view.findViewById(R.id.btnShift);
        updateShiftButton();
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

        // Setup all letter buttons using loop
        for (int i = 0; i < LETTER_BUTTON_IDS.length && i < LETTERS.length; i++) {
            setupLetterButton(view, LETTER_BUTTON_IDS[i], LETTERS[i]);
        }

        // Setup special buttons
        setupBackspaceButton(view, R.id.btnBackQwerty);
        setupTextButton(view, R.id.btnDotQwerty, ".");
        setupTextButton(view, R.id.btnSpaceQwerty, " ");
        setupTextButton(view, R.id.btnCommaQwerty, ",");
        setupButton(view, R.id.btnEnterQwerty, KeyEvent.KEYCODE_ENTER);

        return view;
    }

    // ==================== Button Setup Methods ====================

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

    /**
     * Helper method to send key events to the input connection.
     * 
     * @param keyCode   The key code to send
     * @param metaState The meta state (e.g., KeyEvent.META_SHIFT_ON)
     */
    private void sendKeyEvent(int keyCode, int metaState) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;

        long now = System.currentTimeMillis();
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState));
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState));
    }

    /**
     * Helper method to send key events without meta state.
     * 
     * @param keyCode The key code to send
     */
    private void sendKeyEvent(int keyCode) {
        sendKeyEvent(keyCode, 0);
    }

    private void setupButton(View parent, int id, int keyCode) {
        Button button = parent.findViewById(id);
        button.setOnClickListener(v -> sendKeyEvent(keyCode));
    }

    private void setupLetterButton(View parent, int id, String letter) {
        Button button = parent.findViewById(id);

        // Populate letterButtons array dynamically
        int index = -1;
        for (int i = 0; i < LETTERS.length; i++) {
            if (LETTERS[i].equalsIgnoreCase(letter)) {
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

    /**
     * Input goes to the display.
     */
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

            if (isTabModeEnabled) {
                // Tab mode: left = Shift+Tab, right = Tab
                int metaState = isLeft ? KeyEvent.META_SHIFT_ON : 0;
                sendKeyEvent(KeyEvent.KEYCODE_TAB, metaState);
            } else {
                // Arrow mode: normal arrow keys
                int keyCode = isLeft ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                sendKeyEvent(keyCode);
            }
        });
    }

    /**
     * Updates the case of letter buttons based on shift state and auto-capitalize
     * state.
     */
    private void updateLetterButtonsCase() {
        if (letterButtons == null || !isQwertyMode) {
            return;
        }

        boolean shouldShowUppercase = isShiftEnabled || autoCapitalizeNext;
        for (int i = 0; i < letterButtons.length && i < LETTERS.length; i++) {
            if (letterButtons[i] != null) {
                letterButtons[i].setText(shouldShowUppercase ? LETTERS[i].toUpperCase() : LETTERS[i]);
            }
        }
    }

    private View createSymbolView() {
        View view = getLayoutInflater().inflate(R.layout.symbol_keyboard_view, null);

        // Setup symbol buttons using loops
        for (int i = 0; i < SYMBOL_ROW1_IDS.length && i < SYMBOL_ROW1_CHARS.length; i++) {
            setupTextButton(view, SYMBOL_ROW1_IDS[i], SYMBOL_ROW1_CHARS[i]);
        }
        for (int i = 0; i < SYMBOL_ROW2_IDS.length && i < SYMBOL_ROW2_CHARS.length; i++) {
            setupTextButton(view, SYMBOL_ROW2_IDS[i], SYMBOL_ROW2_CHARS[i]);
        }
        for (int i = 0; i < SYMBOL_ROW3_IDS.length && i < SYMBOL_ROW3_CHARS.length; i++) {
            setupTextButton(view, SYMBOL_ROW3_IDS[i], SYMBOL_ROW3_CHARS[i]);
        }

        // Setup special buttons
        setupBackspaceButton(view, R.id.btnBackSym);

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
