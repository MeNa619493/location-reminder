package com.udacity.project4.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

fun Activity.hasLocationPermissions(): Boolean {
    return (ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
}

@TargetApi(Build.VERSION_CODES.Q)
private fun Activity.hasAndroidQLocationPermissions(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    return when (PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> { true }
        else -> false
    }
}

@TargetApi(Build.VERSION_CODES.R)
private fun Activity.hasAndroidRLocationPermissions(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
    return when {
        ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED -> { true }
        ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> { false }
        else -> false
    }
}

fun Activity.hasAllVersionsLocationPermissions(): Boolean {
    return hasLocationPermissions() && hasAndroidQLocationPermissions() && hasAndroidRLocationPermissions()
}