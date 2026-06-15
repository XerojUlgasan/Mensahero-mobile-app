package com.example.mensahero_mobile_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class KeysState(
    val apiKey: String = "",
    val savedSuccess: Boolean = false,
    val errorMessage: String? = null
)

class KeysViewModel(context: Context) : ViewModel() {
    
    private val preferencesManager = PreferencesManager(context)
    
    private val _state = MutableStateFlow(KeysState())
    val state: StateFlow<KeysState> = _state

    init {
        loadApiKey()
    }

    private fun loadApiKey() {
        viewModelScope.launch {
            preferencesManager.apiKey.collect { key ->
                _state.value = _state.value.copy(apiKey = key ?: "")
            }
        }
    }

    fun onApiKeyChange(key: String) {
        _state.value = _state.value.copy(
            apiKey = key,
            savedSuccess = false,
            errorMessage = null
        )
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            try {
                preferencesManager.saveApiKey(key)
                _state.value = _state.value.copy(
                    savedSuccess = true,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    savedSuccess = false,
                    errorMessage = "Failed to save API key"
                )
            }
        }
    }
}
