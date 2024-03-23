package com.google.android.gms.location.sample.locationupdates.ui

import android.location.Location

data class LocationUiState(
    val lastLocation: Location? = null,
    val lastUpdateTime: String? = null,
    val isUpdating: Boolean = false
)