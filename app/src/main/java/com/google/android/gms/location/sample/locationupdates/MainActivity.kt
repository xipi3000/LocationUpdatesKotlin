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

import android.Manifest
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.sample.locationupdates.databinding.MainBinding
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.*

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
class MainActivity : AppCompatActivity() {
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
    private  var mRequestingLocationUpdates: Boolean = false

    private lateinit var binding: MainBinding

    private var settings = false


    /**
     * Time when the location was updated represented as a String.
     */
    private var mLastUpdateTime: String? = null
    //@RequiresApi(Build.VERSION_CODES.TIRAMISU)


    private var mCurrentLocation: Location? = null
        get() {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) return null

            mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                // Got last known location. In some rare situations this can be null.
                mLatitudeTextView.text = String.format(
                    Locale.ENGLISH, "%s: %f",
                    mLatitudeLabel,
                    location?.latitude
                )
                mLongitudeTextView.text = String.format(
                    Locale.ENGLISH, "%s: %f",
                    mLongitudeLabel,
                    location?.longitude
                )
                mCurrentLocation?.latitude = location?.latitude!!
                mCurrentLocation?.longitude = location.longitude
            }
                .addOnFailureListener {
                    showSnackbar("Failed on getting current location")
                }
            return field
        }

  public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        // Locate the UI widgets.
        mStartUpdatesButton = binding.startUpdatesButton
        mStopUpdatesButton = binding.stopUpdatesButton
        mLatitudeTextView = binding.latitudeText
        mLongitudeTextView = binding.longitudeText
        mLastUpdateTimeTextView = binding.lastUpdateTimeText

        // Set labels.
        mLatitudeLabel = resources.getString(R.string.latitude_label)
        mLongitudeLabel = resources.getString(R.string.longitude_label)
        mLastUpdateTimeLabel = resources.getString(R.string.last_update_time_label)
        mRequestingLocationUpdates = false
        mLastUpdateTime = ""

        //mCurrentLocation = Location()
       // mCurrentLocation.latitude = 41.6082
        //mCurrentLocation.longitude = 0.6231

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                    Log.i(TAG, "User agreed to make precise required location settings changes, updates requested, starting location updates.")
                    startLocationUpdates()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                    Log.i(TAG, "User agreed to make coarse required location settings changes, updates requested, starting location updates.")
                    startLocationUpdates()
                } else -> {
                // No location access granted.
                showSnackbar(
                    R.string.permission_denied_explanation,
                    R.string.settings
                ) { // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        //BuildConfig.APPLICATION_ID , null
                        packageName, null
                    )  // Amb la darrera API level deprecated. Ara és packageName
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                  }
                }
            }
        }

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    //@RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    KEY_REQUESTING_LOCATION_UPDATES
                )
            }
            //if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                //mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION)!!
            //}
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING)
            }
            updateUI()
        }
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     *
     *
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     *
     *
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
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

    /**
     * Creates a callback for receiving location events.
     */
    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                mCurrentLocation = locationResult.lastLocation!!
                mLastUpdateTime = DateFormat.getTimeInstance().format(Date())
                updateLocationUI()
            }
        }
    }

    /**
     * Uses a [com.google.android.gms.location.LocationSettingsRequest.Builder] to build
     * a [com.google.android.gms.location.LocationSettingsRequest] that is used for checking
     * if a device has the needed location settings.
     */
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()
    }


    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    fun startUpdatesButtonHandler(view: View?) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true
            setButtonsEnabledState()
            startLocationUpdates()
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates.
     */
    fun stopUpdatesButtonHandler(view: View?) {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        stopLocationUpdates()
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private fun startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this) {
                Log.i(TAG, "All location settings are satisfied.")
                if ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) || (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, 0, 0) ==
                            PackageManager.PERMISSION_GRANTED
                            )) mFusedLocationClient.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback, Looper.myLooper()!!
                )
                updateUI()
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
                        mRequestingLocationUpdates = false
                    }
                }
                updateUI()
            }
    }

    /**
     * Updates all UI fields.
     */
    private fun updateUI() {
        setButtonsEnabledState()
        updateLocationUI()
    }

    /**
     * Disables both buttons when functionality is disabled due to insuffucient location settings.
     * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
     * enabled if the user is not requesting location updates. The Stop Updates button is enabled
     * if the user is requesting location updates.
     */
    private fun setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.isEnabled = false
            mStopUpdatesButton.isEnabled = true
        } else {
            mStartUpdatesButton.isEnabled = true
            mStopUpdatesButton.isEnabled = false
        }
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private fun updateLocationUI() {
        mLatitudeTextView.text = mCurrentLocation?.let {
            String.format(
                Locale.ENGLISH, "%s: %f", mLatitudeLabel,
                it.latitude
            )
        }
        mLongitudeTextView.text = mCurrentLocation?.let {
            String.format(
                Locale.ENGLISH, "%s: %f", mLongitudeLabel,
                it.longitude
            )
        }
        mLastUpdateTimeTextView.text = String.format(
            Locale.ENGLISH, "%s: %s",
            mLastUpdateTimeLabel, mLastUpdateTime
        )
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.")
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        mRequestingLocationUpdates = false
        setButtonsEnabledState()
    }

    public override fun onResume() {
        super.onResume()
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates()
        } else if (!checkPermissions() && !settings) {
            requestPermissions()
        }
        updateUI()
    }

    override fun onPause() {
        super.onPause()

        // Remove location updates to save battery.
        stopLocationUpdates()
    }

    /**
     * Stores activity data in the Bundle.
     */
    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates)
        //savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation)
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime)
        super.onSaveInstanceState(savedInstanceState)
    }

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

    fun showSnackbar(text: String) {

        val container = findViewById<View>(R.id.main_activity_container)
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

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
}