package com.google.android.gms.location.sample.locationupdates.ui.theme

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.Context
import androidx.compose.runtime.Composable
//import androidx.compose.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.sample.locationupdates.R
import com.google.android.gms.location.sample.locationupdates.showSnackbar


@Composable
fun GetPermisionsLocation( gameViewModel : LocationViewModel,
                           context : Context){
    val snackbarHostState = remember { SnackbarHostState() }

    val launcher  = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(), onResult ={
        permissions ->
        when{
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION,false) -> {
                    Log.i(TAG, "User agreed to make precise required location settings changes, updates requested, starting location updates.")
                    gameViewModel.startUpdating()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                    Log.i(
                        TAG,
                        "User agreed to make coarse required location settings changes, updates requested, starting location updates."
                    )
                    gameViewModel.startUpdating()
                }
                else -> {

                    scaffoldState.snackbarHostState.showSnackbar(
                        message = "Error message",
                        actionLabel = "Retry message"
                    )
                    showSnackbar(
                        R.string.permission_denied_explanation,
                        R.string.settings
                    ) { // Build intent that displays the App settings screen.

                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS

                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK


                        context.startActivity(intent)
                    }

            }
        }
    } )
}
private val permissionsToRequest = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
@Composable
fun MainScreen (

    locationViewModel : LocationViewModel = viewModel()
){

    val locationUiState by locationViewModel.uiState.collectAsState()

    val multiplePermissionsResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {perms ->
            permissionsToRequest.forEach{ permission ->
                locationViewModel.onPermissionResult(
                    permission = permission,
                    isGranted = perms[permission] == true

                )

            }
        }
    )
    val dialogQueue = locationViewModel.visiblePermissionDialogQueue
    dialogQueue
        .reversed()
        .forEach { permission ->
            PermissionDialog(
                permissionTextProvider = when (permission) {
                    Manifest.permission.ACCESS_FINE_LOCATION -> {
                        CameraPermissionTextProvider()
                    }
                    Manifest.permission.ACCESS_COARSE_LOCATION -> {
                        RecordAudioPermissionTextProvider()
                    }
                    else -> return@forEach
                },
                isPermanentlyDeclined = !shouldShowRequestPermissionRationale(
                    permission
                ),
                onDismiss = locationViewModel::dismissDialog,
                onOkClick = {
                    locationViewModel.dismissDialog()
                    multiplePermissionsResultLauncher.launch(
                        arrayOf(permission)
                    )
                },
                onGoToAppSettingsClick = ::openAppSettings
            )
        }

    Column {
        Row {
            Button(onClick = { locationViewModel.startUpdating()}) {
                Text(text = "Start")
            }
            Button(onClick = { locationViewModel.stopUpdating() }) {
                Text(text = "Stop")

            }
        }

        Text(text = locationUiState.currentLocation?.latitude.toString())
        Text(text = locationUiState.currentLocation?.longitude.toString())
    }

}
