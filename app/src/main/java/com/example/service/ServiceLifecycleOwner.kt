package com.example.service

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val appViewModelStore = ViewModelStore()
    private var isRestored = false

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    fun onCreate() {
        if (!isRestored) {
            try {
                savedStateRegistryController.performAttach()
                savedStateRegistryController.performRestore(Bundle())
            } catch (_: Exception) {
            }
            isRestored = true
        }
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onStart() {
        onCreate()
        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }
    }

    fun onStop() {
        if (lifecycleRegistry.currentState > Lifecycle.State.STARTED) {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        appViewModelStore.clear()
    }
}

