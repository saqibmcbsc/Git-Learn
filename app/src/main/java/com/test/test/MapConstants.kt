package com.test.test

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("DEPRECATION")
object MapConstants {

    private lateinit var locationCallback: LocationCallback
    private var polyline: Polyline? = null
    private var carMarker: Marker? = null

    private fun simplifyPath(points: List<LatLng>, tolerance: Double): List<LatLng> {
        if (points.size < 3) return points

        val result = mutableListOf<LatLng>()
        val stack = mutableListOf<Pair<Int, Int>>()
        stack.add(Pair(0, points.size - 1))
        val marked = BooleanArray(points.size)

        while (stack.isNotEmpty()) {
            val (start, end) = stack.removeAt(stack.size - 1)
            var maxDistance = 0.0
            var index = start

            for (i in start + 1 until end) {
                val distance = perpendicularDistance(points[i], points[start], points[end])
                if (distance > maxDistance) {
                    index = i
                    maxDistance = distance
                }
            }

            if (maxDistance > tolerance) {
                marked[index] = true
                stack.add(Pair(start, index))
                stack.add(Pair(index, end))
            }
        }

        result.add(points[0])
        for (i in 1 until points.size - 1) {
            if (marked[i]) result.add(points[i])
        }
        result.add(points[points.size - 1])

        return result
    }

   private fun perpendicularDistance(p: LatLng, p1: LatLng, p2: LatLng): Double {
        val x = p.longitude
        val y = p.latitude
        val x1 = p1.longitude
        val y1 = p1.latitude
        val x2 = p2.longitude
        val y2 = p2.latitude

        val num = abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1)
        val den = sqrt((y2 - y1).pow(2.0) + (x2 - x1).pow(2.0))
        return if (den == 0.0) 0.0 else num / den
    }


    fun startLocationUpdates(map: GoogleMap, recordedPoints: MutableList<LatLng> = mutableListOf(), fusedLocationClient: FusedLocationProviderClient, context: Context) {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    recordedPoints.add(latLng)
                    updatePolyline(recordedPoints,map)
                    moveCar(latLng,map,context)
                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                }
            }
        }

        val request = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }


    fun stopLocationUpdates(fusedLocationClient: FusedLocationProviderClient) { fusedLocationClient.removeLocationUpdates(
        locationCallback
    ) }

    private fun updatePolyline(recordedPoints: MutableList<LatLng> = mutableListOf(), map: GoogleMap) {
        val smoothPoints = simplifyPath(recordedPoints, 0.0001)
        polyline?.remove()
        polyline = map.addPolyline(
            PolylineOptions()
                .color(Color.BLUE)
                .width(20f)
                .zIndex(10f)
                .addAll(smoothPoints)
        )
    }

    fun moveCar(location: LatLng,map: GoogleMap,context: Context) {
        val carBitmap = resizeMapIcons(R.drawable.car_icon2, 100, 100,context)

        if (carMarker == null) { carMarker = map.addMarker(
            MarkerOptions()
            .position(location)
            .icon(carBitmap)
            .anchor(0.5f, 0.5f)
            .flat(true))
        } else { carMarker!!.position = location }
    }


    private fun resizeMapIcons(iconName: Int, width: Int, height: Int,context: Context): BitmapDescriptor {
        val imageBitmap = BitmapFactory.decodeResource(context.resources, iconName)
        val resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }
}