package com.google.android.gms.location.sample.locationupdates

import android.location.Location
import android.provider.ContactsContract.Data
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date

class LocationViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel(

){
    //Si la ubicació està sent enregistrada
    val isUpdating = savedStateHandle.getStateFlow("isUpdating",false)
    private var loc = Location("")
    //La ubicació actual
    val location : StateFlow<Location> = savedStateHandle.getStateFlow("location",loc)
    //El temps en que s'ha enregistrat la ubicació
    val lastTimeUpdate  = savedStateHandle.getStateFlow("lastTimeUpdate","")




    fun startUpdating() {
        savedStateHandle["isUpdating"] = true

    }
    fun dismissDialog() {

        //val permissionsToDialog = savedStateHandle.get<ArrayList<String>>("permissionsToDialog")
        //permissionsToDialog?.removeFirst()
        //permissions.removeFirst()
        //savedStateHandle["permissionsToDialog"]= permissionsToDialog
    }

    fun stopUpdating() {
        savedStateHandle["isUpdating"] = false

    }



    fun updateLocation(lastLocation: Location?) {
        savedStateHandle["location"]=lastLocation


    }

    fun updateUpdateTime(time: String) {
        savedStateHandle["lastTimeUpdate"]=time
    }

    fun permissionsAccepted() {
        savedStateHandle["arePermissionsAccepted"] = true
    }
    fun permissionsNotAccepted() {
        savedStateHandle["arePermissionsAccepted"] = false
    }
}