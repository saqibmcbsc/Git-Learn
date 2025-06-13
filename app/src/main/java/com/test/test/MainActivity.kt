package com.test.test

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.test.test.MapConstants.moveCar
import com.test.test.MapConstants.startLocationUpdates
import com.test.test.MapConstants.stopLocationUpdates

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val recordedPoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null
    private var isRecording = false

    private var carMarker: Marker? = null
    private var startTime: Long = 0
    private var endTime: Long = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnToggle = findViewById<Button>(R.id.btnToggleRecording)
        btnToggle.setOnClickListener {
            isRecording = !isRecording
            if (isRecording) {
                Toast.makeText(this, "Start Recording Route", Toast.LENGTH_SHORT).show()
                btnToggle.text = "Stop Recording"
                startTime = System.currentTimeMillis()
                recordedPoints.clear()
                startLocationUpdates(map, recordedPoints, fusedLocationClient, this)

            } else {
                Toast.makeText(this, "Stop Recording Route", Toast.LENGTH_SHORT).show()
                btnToggle.text = "Start Recording"
                stopLocationUpdates(fusedLocationClient)
                endTime = System.currentTimeMillis()
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    recordedPoints.add(currentLatLng)
                    moveCar(currentLatLng,map,this)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
                } else {
                    Toast.makeText(this, "Current location not found", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Re-try initializing map location when permission is granted
            onMapReady(map)
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun startLocationUpdates() {
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(result: LocationResult) {
//                for (location in result.locations) {
//                    val latLng = LatLng(location.latitude, location.longitude)
//                    recordedPoints.add(latLng)
//                    updatePolyline()
//                    moveCar(latLng)
//                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
//                }
//            }
//        }
//
//        val request = LocationRequest.create().apply {
//            interval = 2000
//            fastestInterval = 1000
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
//
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
//        }
//    }
//
//
//    private fun stopLocationUpdates() {
//        fusedLocationClient.removeLocationUpdates(locationCallback)
//    }
//
//    private fun updatePolyline() {
//        val smoothPoints = simplifyPath(recordedPoints, 0.0001)
//        polyline?.remove()
//        polyline = map.addPolyline(
//            PolylineOptions()
//                .color(Color.BLUE)
//                .width(20f)
//                .addAll(smoothPoints)
//        )
//    }
//
//    private fun moveCar(location: LatLng) {
//        val carBitmap = resizeMapIcons(R.drawable.car_icon2, 100, 100)
//
//        if (carMarker == null) {
//            carMarker = map.addMarker(
//                MarkerOptions()
//                    .position(location)
//                    .icon(carBitmap)
//                    .anchor(0.5f, 0.5f)
//                    .flat(true)
//            )
//        } else {
//            carMarker?.position = location
//        }
//    }
//
//
//    private fun resizeMapIcons(iconName: Int, width: Int, height: Int): BitmapDescriptor {
//        val imageBitmap = BitmapFactory.decodeResource(resources, iconName)
//        val resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false)
//        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
//    }
//
//    fun simplifyPath(points: List<LatLng>, tolerance: Double): List<LatLng> {
//        if (points.size < 3) return points
//
//        val result = mutableListOf<LatLng>()
//        val stack = mutableListOf<Pair<Int, Int>>()
//        stack.add(Pair(0, points.size - 1))
//        val marked = BooleanArray(points.size)
//
//        while (stack.isNotEmpty()) {
//            val (start, end) = stack.removeAt(stack.size - 1)
//            var maxDistance = 0.0
//            var index = start
//
//            for (i in start + 1 until end) {
//                val distance = perpendicularDistance(points[i], points[start], points[end])
//                if (distance > maxDistance) {
//                    index = i
//                    maxDistance = distance
//                }
//            }
//
//            if (maxDistance > tolerance) {
//                marked[index] = true
//                stack.add(Pair(start, index))
//                stack.add(Pair(index, end))
//            }
//        }
//
//        result.add(points[0])
//        for (i in 1 until points.size - 1) {
//            if (marked[i]) result.add(points[i])
//        }
//        result.add(points[points.size - 1])
//
//        return result
//    }
//
//    fun perpendicularDistance(p: LatLng, p1: LatLng, p2: LatLng): Double {
//        val x = p.longitude
//        val y = p.latitude
//        val x1 = p1.longitude
//        val y1 = p1.latitude
//        val x2 = p2.longitude
//        val y2 = p2.latitude
//
//        val num = Math.abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1)
//        val den = Math.sqrt(Math.pow((y2 - y1), 2.0) + Math.pow((x2 - x1), 2.0))
//        return if (den == 0.0) 0.0 else num / den
//    }


}