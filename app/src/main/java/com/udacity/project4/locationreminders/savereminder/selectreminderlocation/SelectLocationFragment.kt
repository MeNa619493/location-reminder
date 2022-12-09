package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.EspressoIdlingResource
import com.udacity.project4.utils.hasLocationPermissions
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment() , OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var selectedMarker: Marker
    private lateinit var selectedPointOfInterest: PointOfInterest

    private var hasUserEnabledDeviceLocation = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnSave.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {
        if (this::selectedPointOfInterest.isInitialized){
            _viewModel.selectedPOI.value = selectedPointOfInterest
            _viewModel.reminderSelectedLocationStr.value = selectedPointOfInterest.name
            _viewModel.latitude.value = selectedPointOfInterest.latLng.latitude
            _viewModel.longitude.value = selectedPointOfInterest.latLng.longitude
        }
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapStyle(map)
        setOnPoiClick(map)
        setOnMapClick(map)
        setOnMapLongClick(map)
        hasUserEnabledDeviceLocation = requireActivity().hasLocationPermissions()
        if (hasUserEnabledDeviceLocation){
            startDetectUserLocation()
        } else {
            showPermissionDialog()
        }
    }

    private fun setOnMapClick(map: GoogleMap){
        map.setOnMapClickListener {latLng ->
            map.clear()

            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude
            )

            selectedPointOfInterest = PointOfInterest(latLng, snippet, snippet)

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.reminder_location))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            selectedMarker.showInfoWindow()
        }
    }

    private fun setOnMapLongClick(map: GoogleMap){
        map.setOnMapLongClickListener {latLng ->
            map.clear()

            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude
            )

            selectedPointOfInterest = PointOfInterest(latLng, snippet, snippet)

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.reminder_location))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            selectedMarker.showInfoWindow()
        }
    }

    private fun setOnPoiClick(map: GoogleMap){
        map.setOnPoiClickListener { poi ->
            map.clear()

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            selectedPointOfInterest = poi
            selectedMarker.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPermissionDialog(){
        val alertDialogBuilder = MaterialAlertDialogBuilder(activity)
        alertDialogBuilder.setTitle(getString(R.string.location_alert_title))
            .setMessage(getString( R.string.permission_denied_explanation))
            .setPositiveButton(getString(R.string.grant)) { dialog: DialogInterface, _: Int ->
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    && shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)){
                    showLocationPermissionSnackBar(requireView())
                    if(requireActivity().hasLocationPermissions()){
                        startDetectUserLocation()
                    }
                }else{
                    requestLocationPermissions()
                }
                dialog.dismiss()
            }.setNegativeButton(
                getString(R.string.Deny)
            ) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                _viewModel.navigationCommand.value = NavigationCommand.Back
            }.show()
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        ActivityCompat.requestPermissions(requireActivity(),
            permissions.toTypedArray(), REQUEST_LOCATION_PERMISSION)
    }

    private fun showLocationPermissionSnackBar(view: View){
        Snackbar.make(
            view,
            R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    private fun getLocationRequestTask(resolve: Boolean = true): Task<LocationSettingsResponse> {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(requireActivity(), 1)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("LocSettingsResponse", "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                getLocationRequestTask()
            }
        }
        return locationSettingsResponseTask
    }

    @SuppressLint("MissingPermission")
    private fun checkIfDeviceLocationEnabled() {
        getLocationRequestTask().addOnSuccessListener {
            hasUserEnabledDeviceLocation = true
        }
        map.isMyLocationEnabled = true;
    }

    @SuppressLint("MissingPermission")
    fun startDetectUserLocation(){
        checkIfDeviceLocationEnabled()
        val mLocationRequest = LocationRequest.create()
        mLocationRequest.interval = 60000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val mLocationCallback: LocationCallback = object : LocationCallback() {
            var i =1
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult.locations ) {
                    i++
                    if (location != null && i==2) {
                        setUserLocation()
                    }
                }
            }
        }

        LocationServices.getFusedLocationProviderClient(context!!)
            .requestLocationUpdates(mLocationRequest, mLocationCallback, null)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    private fun setUserLocation(){
        LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation?.addOnSuccessListener {

            val snippet = activity?.let { it1 ->
                String.format(
                    Locale.getDefault(),
                    it1.getString(R.string.lat_long_snippet),
                    it.latitude,
                    it.longitude
                )
            }
            val latlng = LatLng(it.latitude, it.longitude)
            Log.e(TAG,"Location is "+it.latitude)

            selectedPointOfInterest = PointOfInterest(latlng, snippet, "Current Location")

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(latlng)
                    .title(activity!!.getString(R.string.reminder_location))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )

            val zoom = 10f
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom))
            selectedMarker.showInfoWindow()
        }
    }


    companion object{
        const val TAG = "SelectLocationFragment"
        const val REQUEST_LOCATION_PERMISSION = 1
    }
}
