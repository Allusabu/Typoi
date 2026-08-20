package com.example.service

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
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
import com.example.util.AppLogger

class AutoTyperInputMethodService : InputMethodService() {

    private val lifecycleOwner = ServiceLifecycleOwner()
    private lateinit var settingsManager: SettingsManager
    private lateinit var snippetRepository: SnippetRepository
    private var composeInputView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.ime("AutoTyperService", "onCreate() called")
        try {
            lifecycleOwner.onCreate()
            TypingEngine.init(applicationContext)
            settingsManager = SettingsManager(this)
            snippetRepository = SnippetRepository(this)
            TypingEngine.setInputConnectionSupplier { currentInputConnection }
            TypingEngine.setHapticFeedback(settingsManager.settings.value.hapticFeedback)
            AppLogger.ime("AutoTyperService", "Service successfully initialized")
        } catch (e: Exception) {
            AppLogger.e("AutoTyperService", "Error during onCreate: ${e.message}", e)
        }
    }

    private fun attachViewTreeOwners(view: View?) {
        view?.let {
            try {
                it.setViewTreeLifecycleOwner(lifecycleOwner)
                it.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                it.setViewTreeViewModelStoreOwner(lifecycleOwner)
            } catch (e: Exception) {
                AppLogger.w("AutoTyperService", "Could not attach ViewTreeOwners: ${e.message}")
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        // Never go into fullscreen extract mode so our custom controls remain visible
        return false
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        // Always show our keyboard view when input starts
        return true
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        AppLogger.ime("AutoTyperService", "onShowInputRequested(flags=$flags, configChange=$configChange)")
        return true
    }

    override fun onCreateInputView(): View {
        AppLogger.ime("AutoTyperService", "onCreateInputView() called")
        try {
            window?.window?.decorView?.let { attachViewTreeOwners(it) }
        } catch (e: Exception) {
            AppLogger.w("AutoTyperService", "decorView attach error: ${e.message}")
        }

        val composeView = ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleOwner)
            )
            attachViewTreeOwners(this)

            setContent {
                MyApplicationTheme(darkTheme = true) {
                    AutoTyperKeyboardScreen(
                        settingsManager = settingsManager,
                        snippetRepository = snippetRepository,
                        onKeyTyped = { text ->
                            AppLogger.d("AutoTyperService", "Key typed: '$text'")
                            val ic = currentInputConnection
                            if (ic == null) {
                                AppLogger.w("AutoTyperService", "Cannot type: InputConnection is null")
                            }
                            ic?.commitText(text, 1)
                        },
                        onSwitchKeyboard = {
                            AppLogger.ime("AutoTyperService", "Switch keyboard requested")
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
                            val ic = currentInputConnection
                            val editorInfo = currentInputEditorInfo
                            if (editorInfo != null && (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE && (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_UNSPECIFIED) {
                                val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
                                ic?.performEditorAction(action)
                            } else {
                                sendKeyChar('\n')
                            }
                        },
                        onSpace = {
                            currentInputConnection?.commitText(" ", 1)
                        },
                        onHideKeyboard = {
                            AppLogger.ime("AutoTyperService", "Hide keyboard requested")
                            requestHideSelf(0)
                        }
                    )
                }
            }
        }
        composeInputView = composeView
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        AppLogger.ime("AutoTyperService", "onStartInputView(inputType=${info?.inputType}, actionId=${info?.actionId}, restarting=$restarting)")
        try {
            window?.window?.decorView?.let { attachViewTreeOwners(it) }
        } catch (e: Exception) {
            AppLogger.w("AutoTyperService", "decorView attach error on start: ${e.message}")
        }
        lifecycleOwner.onStart()
        TypingEngine.setInputConnectionSupplier { currentInputConnection }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        AppLogger.ime("AutoTyperService", "onFinishInputView(finishingInput=$finishingInput)")
        lifecycleOwner.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLogger.d("AutoTyperService", "onConfigurationChanged: orientation=${newConfig.orientation}")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.ime("AutoTyperService", "onDestroy() called")
        lifecycleOwner.onDestroy()
        TypingEngine.setInputConnectionSupplier(null)
        composeInputView = null
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
        } catch (e: Exception) {
            AppLogger.w("AutoTyperService", "handleSwitchKeyboard fallback: ${e.message}")
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }
    }
}


