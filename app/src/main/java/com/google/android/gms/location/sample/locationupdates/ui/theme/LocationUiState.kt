package com.google.android.gms.location.sample.locationupdates.ui.theme

import android.location.Location

data class LocationUiState(
    val currentLocation: Location?,
    val isUpdating: Boolean = false
)