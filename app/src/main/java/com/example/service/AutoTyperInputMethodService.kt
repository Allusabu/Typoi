package com.example.service

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.SettingsManager
import com.example.data.SnippetRepository
import com.example.engine.TypingEngine
import com.example.keyboard.AutoTyperKeyboardScreen
import com.example.ui.theme.MyApplicationTheme

class AutoTyperInputMethodService : InputMethodService() {

    private val lifecycleOwner = ServiceLifecycleOwner()
    private lateinit var settingsManager: SettingsManager
    private lateinit var snippetRepository: SnippetRepository

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        TypingEngine.init(applicationContext)
        settingsManager = SettingsManager(this)
        snippetRepository = SnippetRepository(this)
        TypingEngine.setInputConnectionSupplier { currentInputConnection }
        TypingEngine.setHapticFeedback(settingsManager.settings.value.hapticFeedback)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleOwner)
            )
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)

            setContent {
                MyApplicationTheme(darkTheme = true) {
                    AutoTyperKeyboardScreen(
                        settingsManager = settingsManager,
                        snippetRepository = snippetRepository,
                        onSwitchKeyboard = {
                            handleSwitchKeyboard()
                        },
                        onBackspace = {
                            currentInputConnection?.let { ic ->
                                val selectedText = ic.getSelectedText(0)
                                if (selectedText.isNullOrEmpty()) {
                                    ic.deleteSurroundingText(1, 0)
                                } else {
                                    ic.commitText("", 1)
                                }
                            } ?: sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                        },
                        onEnter = {
                            sendKeyChar('\n')
                        },
                        onSpace = {
                            currentInputConnection?.commitText(" ", 1)
                        },
                        onHideKeyboard = {
                            requestHideSelf(0)
                        }
                    )
                }
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleOwner.onStart()
        TypingEngine.setInputConnectionSupplier { currentInputConnection }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleOwner.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.onDestroy()
        TypingEngine.setInputConnectionSupplier(null)
    }

    private fun handleSwitchKeyboard() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod()
            } else {
                @Suppress("DEPRECATION")
                val switched = switchToNextInputMethod(false)
                if (!switched) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showInputMethodPicker()
                }
            }
        } catch (_: Exception) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }
    }
}
