package com.google.android.gms.location.sample.locationupdates.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.sample.locationupdates.ui.theme.LocationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocationViewModel : ViewModel(){

    val visiblePermissionDialogQueue = mutableStateListOf<String>()
    private val _uiState = MutableStateFlow(LocationUiState(null))
    val uiState : StateFlow<LocationUiState> = _uiState.asStateFlow()
    fun startUpdating() {
        _uiState.update { currentState ->
            currentState.copy(
                isUpdating = true,
            )
        }
        registerToLocationListener()
    }
    fun dismissDialog() {
        visiblePermissionDialogQueue.removeFirst()
    }

    fun stopUpdating() {
        _uiState.update { currentState ->
            currentState.copy(
                isUpdating = false,
            )
        }
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        if(!isGranted && !visiblePermissionDialogQueue.contains(permission)) {
            visiblePermissionDialogQueue.add(permission)
        }
    }
}