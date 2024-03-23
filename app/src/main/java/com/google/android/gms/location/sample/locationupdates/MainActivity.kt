/*
  Copyright 2017 Google Inc. All Rights Reserved.
  <p>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.google.android.gms.location.sample.locationupdates

import androidx.compose.material3.Surface
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.sample.locationupdates.databinding.MainBinding
import com.google.android.gms.location.sample.locationupdates.ui.LocationViewModel
import com.google.android.gms.location.sample.locationupdates.ui.theme.CameraPermissionTextProvider
import com.google.android.gms.location.sample.locationupdates.ui.theme.PermissionDialog
import com.google.android.gms.location.sample.locationupdates.ui.theme.RecordAudioPermissionTextProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Using location settings.
 *
 *
 * Uses the [com.google.android.gms.location.SettingsApi] to ensure that the device's system
 * settings are properly configured for the app's location needs. When making a request to
 * Location services, the device's system settings may be in a state that prevents the app from
 * obtaining the location data that it needs. For example, GPS or Wi-Fi scanning may be switched
 * off. The `SettingsApi` makes it possible to determine if a device's system settings are
 * adequate for the location request, and to optionally invoke a dialog that allows the user to
 * enable the necessary settings.
 *
 *
 * This sample allows the user to request location updates using the ACCESS_FINE_LOCATION setting
 * (as specified in AndroidManifest.xml).
 */


class MainActivity : ComponentActivity() {


    /**
     * Provides access to the Fused Location Provider API.
     */
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    /**
     * Provides access to the Location Settings API.
     */
    private lateinit var mSettingsClient: SettingsClient

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private lateinit var mLocationRequest: LocationRequest


    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest

    /**
     * Callback for Location events.
     */
    private lateinit var mLocationCallback: LocationCallback

    /**
     * Represents a geographical location.
     */
    companion object {
        private val TAG = "goofy"

        /**
         * Constant used in the location settings dialog.
         */
        private const val REQUEST_CHECK_SETTINGS = 0x1

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 100  // 10000

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2

        // Keys for storing activity state in the Bundle.
        private const val KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates"
        private const val KEY_LOCATION = "location"
        private const val KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string"
    }

    // UI Widgets.
    private lateinit var mStartUpdatesButton: Button
    private lateinit var mStopUpdatesButton: Button
    private lateinit var mLastUpdateTimeTextView: TextView
    private lateinit var mLatitudeTextView: TextView
    private lateinit var mLongitudeTextView: TextView

    // Labels.
    private lateinit var mLatitudeLabel: String
    private lateinit var mLongitudeLabel: String
    private lateinit var mLastUpdateTimeLabel: String

    //lateinit var startForResult: ActivityResultLauncher<String[]>

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>


    private lateinit var binding: MainBinding

    private var settings = false


    /**
     * Time when the location was updated represented as a String.
     */
    private var mLastUpdateTime: String? = null
    //@RequiresApi(Build.VERSION_CODES.TIRAMISU)


    private val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    fun openAppSettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        activity.startActivity(intent)
    }

    private val locationViewModel by viewModels<LocationViewModel>()
    fun showSnackBar(
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,

        message: String,
        actionLabel: String,
        action: () -> Unit,

        ) {
        scope.launch {
            val result = snackbarHostState
                .showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    // Defaults to SnackbarDuration.Short
                    duration = SnackbarDuration.Indefinite
                )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    /* Handle snackbar action performed */

                    action()
                }

                SnackbarResult.Dismissed -> {
                    /* Handle snackbar dismissed */
                    Log.i("asd", "b")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val rationaleMessage = stringResource(id = R.string.permission_rationale)
            val rationaleLabel = stringResource(id = android.R.string.ok)
            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                modifier = Modifier.fillMaxSize(),

                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },

                ) { innerPadding ->
                val currentActivity = LocalContext.current as Activity


                val scope = rememberCoroutineScope()

                val settingLabel = stringResource(id = R.string.settings)
                val settingMessage = stringResource(R.string.permission_denied_explanation)
                val multiplePermissionsResultLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { perms ->
                        when {
                            perms.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, true) -> {
                                // Precise location access granted.
                                Log.i(
                                    TAG,
                                    "User agreed to make precise required location settings changes, updates requested, starting location updates."
                                )
                                startLocationUpdates()
                            }

                            perms.getOrDefault(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                true
                            ) -> {
                                // Only approximate location access granted.
                                Log.i(
                                    TAG,
                                    "User agreed to make coarse required location settings changes, updates requested, starting location updates."
                                )
                                startLocationUpdates()
                            }

                            else -> {
                                locationViewModel.stopUpdating()
                                showSnackBar(
                                    scope = scope,
                                    snackbarHostState = snackbarHostState,
                                    actionLabel = settingLabel,
                                    message = settingMessage,
                                    action = { openAppSettings(currentActivity) }
                                )
                            }
                        }
                    }
                )

                val updating by locationViewModel.isUpdating.collectAsState()
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(key1 = lifecycleOwner,
                    effect = {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                if (updating && permissionsGranted()) {
                                    startLocationUpdates()
                                } else if (updating && !permissionsGranted() && !settings) {
                                    if(shouldShowRationaleLocation())
                                        showSnackBar(
                                            snackbarHostState = snackbarHostState,
                                            scope = scope,
                                            message = rationaleMessage,
                                            actionLabel = rationaleLabel,
                                            action = {
                                                multiplePermissionsResultLauncher.launch(
                                                    permissionsToRequest
                                                )
                                            }
                                        )
                                    multiplePermissionsResultLauncher.launch(
                                        permissionsToRequest
                                    )


                                }
                            }
                            if (event == Lifecycle.Event.ON_PAUSE) {

                                //stopLocationUpdates(updating)
                            }
                        }

                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                )


                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {




                    Row {

                        Button(
                            enabled = !updating,
                            onClick = {
                                if (!permissionsGranted()) {
                                    if (shouldShowRationaleLocation()) {
                                        showSnackBar(
                                            snackbarHostState = snackbarHostState,
                                            scope = scope,
                                            message = rationaleMessage,
                                            actionLabel = rationaleLabel,
                                            action = {
                                                multiplePermissionsResultLauncher.launch(
                                                    permissionsToRequest
                                                )
                                            }
                                        )
                                    } else {
                                        multiplePermissionsResultLauncher.launch(
                                            permissionsToRequest
                                        )
                                    }


                                }
                                else {
                                    startLocationUpdates()
                                }
                            }

                        ) {
                            Text(text = "Start")
                        }

                        Button(
                            enabled = updating,
                            onClick = {

                                stopLocationUpdates(updating)

                            }) {
                            Text(text = "Stop")

                        }

                    }
                    val location by locationViewModel.location.collectAsState()
                    val lastTimeUpdate by locationViewModel.lastTimeUpdate.collectAsState()
                    Text(
                        text = "Location: ${location.latitude} , ${location.latitude} ",

                    )
                    Text(text = "Time: $lastTimeUpdate",
                    )
                }



                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                mSettingsClient = LocationServices.getSettingsClient(this)

                // Kick off the process of building the LocationCallback, LocationRequest, and
                // LocationSettingsRequest objects.
                createLocationCallback(locationViewModel)
                createLocationRequest()
                buildLocationSettingsRequest()


            }

        }
        /*
        message = if (shouldShowRationaleLocation(currentActivity)) stringResource(R.string.permission_rationale) else stringResource(R.string.permission_denied_explanation),

                        actionLabel = if (shouldShowRationaleLocation(currentActivity)) android.R.string.ok else   R.string.settings ,

         */



        @Composable
        fun CreateLocationCallback(
            locationViewModel: LocationViewModel

        ) {
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationViewModel.updateLocation(locationResult.lastLocation)
                    locationViewModel.updateUpdateTime(DateFormat.getTimeInstance().format(Date()))
//                updateLocationUI()
                }
            }
        }
    }


    private fun shouldShowRationaleLocation(): Boolean {
        return !ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || !ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest.Builder(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
            .setMaxUpdateDelayMillis(UPDATE_INTERVAL_IN_MILLISECONDS)
            .build()


        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.


        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
    }

    private fun permissionsGranted(): Boolean {
        val permissionFineState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val permissionCoarseState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        return ((permissionFineState == PackageManager.PERMISSION_GRANTED) || (permissionCoarseState == PackageManager.PERMISSION_GRANTED))
    }

    private fun createLocationCallback(
        locationViewModel: LocationViewModel

    ) {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Log.e("goofy", locationResult.lastLocation.toString())
                locationViewModel.updateLocation(locationResult.lastLocation)

                locationViewModel.updateUpdateTime(DateFormat.getTimeInstance().format(Date()))
                //                updateLocationUI()
            }
        }
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)

        mLocationSettingsRequest = builder.build()
    }

    private fun stopLocationUpdates(
        updating: Boolean
    ) {

        if (!updating) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.")
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        locationViewModel.stopUpdating()
    }

    private fun startLocationUpdates() {

        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this) {
                Log.i(TAG, "All location settings are satisfied.")
                //TODO:Això es podria treure crec i no entenc el ||
                if ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                            ) || (checkPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        0,
                        0
                    ) ==
                            PackageManager.PERMISSION_GRANTED
                            )
                ) {
                    locationViewModel.startUpdating()
                    mFusedLocationClient.requestLocationUpdates(
                        mLocationRequest,
                        mLocationCallback, Looper.myLooper()!!
                    )
                }
            }
            .addOnFailureListener(this) { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(
                            TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                    "location settings "
                        )
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                        } catch (sie: SendIntentException) {
                            Log.i(TAG, "PendingIntent unable to execute request.")
                        }
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = "Location settings are inadequate, and cannot be " +
                                "fixed here. Fix in Settings."
                        Log.e(TAG, errorMessage)
                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                        locationViewModel.stopUpdating()
                    }
                }
            }
    }


    /*

        public override fun onResume() {
            super.onResume()
            // Within {@code onPause()}, we remove location updates. Here, we resume receiving
            // location updates if the user has requested them.

            //TODO: sol fa falta al principi

            if (locationViewModel.uiState.value.isUpdating && checkPermissions()) {
                startLocationUpdates()
            } else if (!checkPermissions() && !settings) {
                requestPermissions()
            }


        }

        override fun onPause() {
            super.onPause()

            // Remove location updates to save battery.
            if (locationViewModel.uiState.value.isUpdating)
                stopLocationUpdates()
            //TODO: faltaria aixo?? locationPermissionLauncher.unregister()
        }

        /**
         * Stores activity data in the Bundle.
         */

        /**
         * Shows a [Snackbar].
         *
         * @param mainTextStringId The id for the string resource for the Snackbar text.
         * @param actionStringId   The text of the action item.
         * @param listener         The listener associated with the Snackbar action.
         */
        private fun showSnackbar(
            mainTextStringId: Int, actionStringId: Int,
            listener: View.OnClickListener
        ) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(getString(actionStringId), listener).show()
        }

        /**
         * Return the current state of the permissions needed.
         */
        private fun checkPermissions(): Boolean {
            val permissionFineState = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val permissionCoarseState = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            return ((permissionFineState == PackageManager.PERMISSION_GRANTED) || (permissionCoarseState == PackageManager.PERMISSION_GRANTED))
        }

        private fun requestPermissions() {
            val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            // Provide an additional rationale to the user. This would happen if the user denied the
            // request previously, but didn't check the "Don't ask again" checkbox.
            if (shouldProvideRationale) {
                Log.i(TAG, "Displaying permission rationale to provide additional context.")
                showSnackbar(
                    R.string.permission_rationale,
                    android.R.string.ok
                ) { // Request permission
                    locationPermissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                settings = true
            } else {
                Log.i(TAG, "Requesting permission")
                // Request permission. It's possible this can be auto answered if device policy
                // sets the permission in a given state or the user denied the permission
                // previously and checked "Never ask again".

                locationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }

        fun showSnackbar(text: Int, settings: Int, function: () -> Unit) {

            val container = findViewById<View>(R.id.main_activity_container)
            if (container != null) {
                Snackbar.make(container, text, Snackbar.LENGTH_LONG).show()
            }
        }
    */

}
