package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.hasAllVersionsLocationPermissions
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        _viewModel.showSnackBarInt.observe(viewLifecycleOwner, Observer {
            Snackbar.make(
                binding.root,
                it, Snackbar.LENGTH_LONG
            ).show()
        })

        _viewModel.showToast.observe(viewLifecycleOwner, Observer {
            Snackbar.make(
                binding.root,
                it, Snackbar.LENGTH_LONG
            ).show()
        })

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val id = _viewModel.selectedPOI.value?.placeId ?: ""

            val reminderItem = ReminderDataItem( title,
                                            description,
                                            location,
                                            latitude,
                                            longitude,
                                            id)

            if (_viewModel.validateEnteredData(reminderItem)) {
                createGeofenceRequest(reminderItem)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    private fun createGeofenceRequest(reminderItem: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminderItem.id)
            .setCircularRegion(
                reminderItem.latitude?:0.0,
                reminderItem.longitude?:0.0,
                100f
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (requireActivity().hasAllVersionsLocationPermissions()) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnFailureListener {
                    Log.v(TAG, "Failed adding... " + it.message)
                }
                addOnSuccessListener {
                    _viewModel.validateAndSaveReminder(reminderItem)
                    Log.v(TAG, "Added successfully... " + reminderItem.title)
                }
            }
        } else {
            showPermissionDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPermissionDialog(){
        val alertDialogBuilder = MaterialAlertDialogBuilder(activity)
        alertDialogBuilder.setTitle(getString(R.string.location_alert_title))
            .setMessage(getString( R.string.permission_denied_explanation))
            .setPositiveButton(getString(R.string.settings)) { dialog: DialogInterface, _: Int ->
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                dialog.dismiss()
            }.setNegativeButton(
                getString(R.string.Deny)
            ) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE_EVENT"
        const val TAG = "SaveReminderFragment"
    }
}
