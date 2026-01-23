package com.xlkeyboard;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.text.TextUtils;
import android.util.Log;

public class XlKeyboardService extends InputMethodService {

    private static final String PREFS_NAME = "NumberPadPrefs";
    private static final String KEY_TAB_MODE = "tab_mode_enabled";
    private static final String KEY_AUTO_MODE = "auto_mode_enabled";
    private static final long REPEAT_DELAY = 50;
    private static final long DOUBLE_TAP_DELAY = 300;

    private SharedPreferences prefs;
    private boolean isTabModeEnabled = false;
    private boolean isAutoModeEnabled = false;
    private boolean isQwertyMode = false;
    private boolean isShiftEnabled = false;
    private boolean isCapsLock = false;
    private long lastShiftPressTime = 0;
    private boolean autoCapitalizeNext = false;

    private Button toggleButton;
    private Button autoButton;
    private Button shiftButton;
    private Button[] letterButtons;
    private static final String FILE = "XlKeyboardService";

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

    // For Row 1 .
    private static final int[] SYMBOL_ROW1_IDS = {
            R.id.btnSym1, R.id.btnSym2, R.id.btnSym3, R.id.btnSym4, R.id.btnSym5,
            R.id.btnSym6, R.id.btnSym7, R.id.btnSym8, R.id.btnSym9, R.id.btnSym0
    };
    private static final String[] SYMBOL_ROW1_CHARS = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"
    };

    // For Row 2 ...
    private static final int[] SYMBOL_ROW2_IDS = {
            R.id.btnSymAt, R.id.btnSymHash, R.id.btnSymDollar, R.id.btnSymPercent,
            R.id.btnSymAmpersand, R.id.btnSymMinus, R.id.btnSymPlus,
            R.id.btnSymParenLeft, R.id.btnSymParenRight, R.id.btnSymSlash
    };
    private static final String[] SYMBOL_ROW2_CHARS = {
            "@", "#", "₹", "%", "&", "-", "+", "(", ")", "/"
    };

    // For Row 3 ...
    private static final int[] SYMBOL_ROW3_IDS = {
            R.id.btnSymEquals, R.id.btnSymAsterisk, R.id.btnSymQuoteDouble,
            R.id.btnSymQuoteSingle, R.id.btnSymColon, R.id.btnSymSemicolon,
            R.id.btnSymExclamation, R.id.btnSymQuestion, R.id.btnLessThan, R.id.btnGreatThan
    };
    private static final String[] SYMBOL_ROW3_CHARS = {
            "=", "*", "\"", "'", ":", ";", "!", "?", "<", ">"
    };

    private String getLocation() {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        return String.format("[%s:%d]", ste.getMethodName(), ste.getLineNumber());
    }

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
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);

        // Check the input type to decide which layout to show
        int inputType = info.inputType & InputType.TYPE_MASK_CLASS;
        // false for numbers
        isQwertyMode = inputType != InputType.TYPE_CLASS_NUMBER &&
                inputType != InputType.TYPE_CLASS_PHONE &&
                inputType != InputType.TYPE_CLASS_DATETIME;

        if (isAutoModeEnabled) {
            // Apply the determined view

            setInputView(onCreateInputView());

            // Reset shift state when view is reset
            isShiftEnabled = false;
            isCapsLock = false;
        }
    }

    @Override
    public View onCreateInputView() {
        Log.d(FILE, getLocation() + "isQwertyMode:" + isQwertyMode);
        if (isQwertyMode) {
            return createQwertyView();
        } else {
            return createNumberPadView();
        }
    }

    private View createNumberPadView() {
        Log.d(FILE, getLocation() + "isQwertyMode:" + isQwertyMode);
        View view = getLayoutInflater().inflate(R.layout.keyboard_view, null);

        // Load saved preference
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isTabModeEnabled = prefs.getBoolean(KEY_TAB_MODE, false);

        // Toggle auto button
        setupAutoButton(view, R.id.btnAutoNumber);

        // Setup SYM button
        Button btnSym = view.findViewById(R.id.btnSym);
        btnSym.setOnClickListener(v -> setInputView(createSymbolView()));

        // Setup toggle button
        setupToggleButton(view, R.id.btnToggleMode);

        // Setup ABC button to switch to QWERTY
        Button abcButton = view.findViewById(R.id.btnABC);
        abcButton.setOnClickListener(v -> {
            isQwertyMode = true;
            // Safe to reset shift state when switching modes
            isShiftEnabled = false;
            isCapsLock = false;
            Log.w(FILE, getLocation() + "abcButton listener, isQwertyMode:" + isQwertyMode);
            setInputView(onCreateInputView());

        });

        // Setup number buttons
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
        Log.d(FILE, getLocation() + "started");

        // Setup SYM button for QWERTY view
        Button btnSym = view.findViewById(R.id.btnSym);
        btnSym.setOnClickListener(v -> setInputView(createSymbolView()));

        // Toggle auto button
        setupAutoButton(view, R.id.btnAutoQwerty);
        Log.d(FILE, getLocation() + "Auto Button set");

        // Setup toggle button for Tab mode
        setupToggleButton(view, R.id.btnToggleModeQwerty);
        Log.d(FILE, getLocation() + "Toggle Button set");

        // Initialize letter buttons array
        letterButtons = new Button[26];

        // Setup 123 button to switch back to number pad
        Button btn123 = view.findViewById(R.id.btn123);
        btn123.setOnClickListener(v -> {
            isQwertyMode = false;
            setInputView(onCreateInputView());
        });

        // Setup shift button with dynamic letter case switching
        shiftButton = view.findViewById(R.id.btnShift);
        Log.d(FILE, getLocation() + "Shift Button State Accessed");

        updateShiftButton();

        shiftButton.setOnClickListener(v -> {
            Log.d(FILE, getLocation() + "Shift Button listener, isCapsLock:" + isCapsLock + ", isShiftEnabled:"
                    + isShiftEnabled);
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShiftPressTime < DOUBLE_TAP_DELAY) {
                // Double tap detected - Enable Caps Lock
                isCapsLock = true;
                isShiftEnabled = true;
            } else {
                // Single tap
                if (isCapsLock) {
                    isCapsLock = false;
                    isShiftEnabled = false;
                } else {
                    isShiftEnabled = !isShiftEnabled;
                }
            }

            Log.d(FILE, getLocation() + "Shift Button listener, isCapsLock:" + isCapsLock + ", isShiftEnabled:"
                    + isShiftEnabled);
            lastShiftPressTime = currentTime;
            updateShiftButton();
            updateLetterButtonsCase();
        });

        // Setup all letter buttons using loop
        for (int i = 0; i < LETTER_BUTTON_IDS.length; i++) {
            // Optimization: Pass index 'i' directly to avoid lookup
            setupLetterButton(view, LETTER_BUTTON_IDS[i], LETTERS[i], i);
        }
        Log.d(FILE, getLocation() + "setup letter buttons done");

        // Setup special buttons
        setupBackspaceButton(view, R.id.btnBackQwerty);
        setupTextButton(view, R.id.btnDotQwerty, ".");
        setupTextButton(view, R.id.btnSpaceQwerty, " ");
        setupTextButton(view, R.id.btnCommaQwerty, ",");
        setupButton(view, R.id.btnEnterQwerty, KeyEvent.KEYCODE_ENTER);
        Log.d(FILE, getLocation() + "setup QWERTY layout done");
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

            if (id == R.id.btnToggleMode) {
                updateToggleButtonState(toggleButton); // Sync main reference
            }
        });

        if (id == R.id.btnToggleMode) {
            this.toggleButton = btn;
        }
    }

    private void setupAutoButton(View parent, int id) {
        Button btn = parent.findViewById(id);
        updateAutoButtonState(btn);
        btn.setOnClickListener(v -> {
            isAutoModeEnabled = !isAutoModeEnabled;
            prefs.edit().putBoolean(KEY_AUTO_MODE, isAutoModeEnabled).apply();
            updateAutoButtonState(btn);

            if (id == R.id.btnAutoNumber) {
                updateAutoButtonState(autoButton); // Sync main reference
            }
        });

        if (id == R.id.btnAutoNumber) {
            this.autoButton = btn;
        }
    }

    private void updateToggleButtonState(Button btn) {
        if (btn == null)
            return;
        boolean active = isTabModeEnabled;
        btn.setText(active ? R.string.key_toggle_tab : R.string.key_toggle_arrow);
        btn.setBackgroundTintList(getResources().getColorStateList(
                active ? R.color.toggle_active : R.color.toggle_inactive, null));
    }

    private void updateAutoButtonState(Button btn) {
        if (btn == null)
            return;
        btn.setBackgroundTintList(getResources().getColorStateList(
                isAutoModeEnabled ? R.color.toggle_active : R.color.toggle_inactive, null));
    }

    private void updateShiftButton() {
        if (shiftButton != null) {
            shiftButton.setBackgroundTintList(getResources().getColorStateList(
                    isShiftEnabled ? R.color.toggle_active : R.color.toggle_inactive, null));
        }
        Log.w(FILE, getLocation() + " isShiftEnabled:" + isShiftEnabled);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupBackspaceButton(View parent, int id) {
        Button button = parent.findViewById(id);
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        CharSequence selectedText = ic.getSelectedText(0);
                        if (TextUtils.isEmpty(selectedText)) {
                            ic.deleteSurroundingText(1, 0);
                        } else {
                            ic.commitText("", 1);
                        }
                    }
                    deleteHandler.postDelayed(deleteRunnable, 500);
                    return true;
                case MotionEvent.ACTION_UP:
                    v.performClick(); // Accessibility support
                case MotionEvent.ACTION_CANCEL:
                    deleteHandler.removeCallbacks(deleteRunnable);
                    return true;
            }
            return false;
        });
    }

    private void sendKeyEvent(int keyCode, int metaState) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;
        long now = System.currentTimeMillis();
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState));
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState));
    }

    private void sendKeyEvent(int keyCode) {
        sendKeyEvent(keyCode, 0);
    }

    private void setupButton(View parent, int id, int keyCode) {
        Button button = parent.findViewById(id);
        button.setOnClickListener(v -> sendKeyEvent(keyCode));
    }

    // Optimized: Now takes index directly
    private void setupLetterButton(View parent, int id, String letter, int index) {
        Button button = parent.findViewById(id);

        if (letterButtons != null && index >= 0 && index < letterButtons.length) {
            letterButtons[index] = button;
        }

        button.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null)
                return;

            boolean shouldCapitalize = isShiftEnabled || autoCapitalizeNext;
            String text = shouldCapitalize ? letter.toUpperCase() : letter;
            ic.commitText(text, 1);

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

            if (text.equals(" ")) {
                CharSequence textBefore = ic.getTextBeforeCursor(2, 0);
                autoCapitalizeNext = textBefore != null && textBefore.length() >= 2
                        && textBefore.charAt(textBefore.length() - 2) == '.';
            } else {
                autoCapitalizeNext = false;
            }
            updateLetterButtonsCase();
        });
    }

    // Helper to setup multiple text buttons at once
    private void setupBatchTextButtons(View parent, int[] ids, String[] chars) {
        for (int i = 0; i < ids.length && i < chars.length; i++) {
            setupTextButton(parent, ids[i], chars[i]);
        }
    }

    private void setupArrowButton(View parent, int id, boolean isLeft) {
        Button button = parent.findViewById(id);
        button.setOnClickListener(v -> {
            if (isTabModeEnabled) {
                int metaState = isLeft ? KeyEvent.META_SHIFT_ON : 0;
                sendKeyEvent(KeyEvent.KEYCODE_TAB, metaState);
            } else {
                int keyCode = isLeft ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                sendKeyEvent(keyCode);
            }
        });
    }

    private void updateLetterButtonsCase() {
        if (letterButtons == null || !isQwertyMode)
            return;
        boolean shouldShowUppercase = isShiftEnabled || autoCapitalizeNext;

        for (int i = 0; i < letterButtons.length && i < LETTERS.length; i++) {
            if (letterButtons[i] != null) {
                letterButtons[i].setText(shouldShowUppercase ? LETTERS[i].toUpperCase() : LETTERS[i]);
            }
        }

        Log.d(FILE, getLocation() + " isShiftEnabled:" + isShiftEnabled + " autoCapitalizeNext:" + autoCapitalizeNext
                + " shouldShowUppercase:" + shouldShowUppercase);
    }

    private View createSymbolView() {
        Log.d(FILE, getLocation());
        View view = getLayoutInflater().inflate(R.layout.symbol_keyboard_view, null);

        setupToggleButton(view, R.id.btnToggleMode);
        setupButton(view, R.id.btnUp, KeyEvent.KEYCODE_DPAD_UP);
        setupButton(view, R.id.btnDown, KeyEvent.KEYCODE_ENTER);

        setupArrowButton(view, R.id.btnLeft, true);
        setupArrowButton(view, R.id.btnRight, false);

        // Optimized batch setup
        setupBatchTextButtons(view, SYMBOL_ROW1_IDS, SYMBOL_ROW1_CHARS);
        setupBatchTextButtons(view, SYMBOL_ROW2_IDS, SYMBOL_ROW2_CHARS);
        setupBatchTextButtons(view, SYMBOL_ROW3_IDS, SYMBOL_ROW3_CHARS);

        setupBackspaceButton(view, R.id.btnBackSym);

        Button btnABC = view.findViewById(R.id.btnABC_Sym);
        btnABC.setOnClickListener(v -> {
            isQwertyMode = true;
            setInputView(createQwertyView());
        });

        setupTextButton(view, R.id.btnDot, ".");
        setupTextButton(view, R.id.btnSpaceSym, " ");
        setupTextButton(view, R.id.btnCommaSym, ",");
        setupButton(view, R.id.btnEnterSym, KeyEvent.KEYCODE_ENTER);

        return view;
    }
}
