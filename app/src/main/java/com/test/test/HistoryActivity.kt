package com.test.test

import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val startAddressView = findViewById<TextView>(R.id.start_address)
        val endAddressView = findViewById<TextView>(R.id.end_address)
        val distanceView = findViewById<TextView>(R.id.distance)
        val durationView = findViewById<TextView>(R.id.duration)


        val points = intent.getParcelableArrayListExtra<LatLng>("points") ?: emptyList()
        val startTime = intent.getLongExtra("startTime", 0)
        val endTime = intent.getLongExtra("endTime", 0)
        val startLatLng = intent.getParcelableExtra<LatLng>("startLatLng")
        val endLatLng = intent.getParcelableExtra<LatLng>("endLatLng")


        val distance = if (points.size >= 2) calculateDistance(points) else 0f
        val duration = if (startTime > 0 && endTime > startTime) (endTime - startTime) / 1000 else 0

        distanceView.text = "Distance: %.2f meters".format(distance)
        durationView.text = "Duration: $duration seconds"

        if (startLatLng != null && endLatLng != null) {
            getAddressFromLatLng(startLatLng) { startAddress ->
                getAddressFromLatLng(endLatLng) { endAddress ->
                    startAddressView.text = "Start Address:\n$startAddress"
                    endAddressView.text = "End Address:\n$endAddress"
                }
            }
        } else {
            startAddressView.text = "Start Address: Not available"
            endAddressView.text = "End Address: Not available"
        }


//        val startLatLng = points.firstOrNull()
//        val endLatLng = points.lastOrNull()

//        if (startLatLng != null && endLatLng != null) {
//            getAddressFromLatLng(startLatLng) { startAddress ->
//                getAddressFromLatLng(endLatLng) { endAddress ->
//                    startAddressView.text = "Start Address:\n$startAddress"
//                    endAddressView.text = "End Address:\n$endAddress"
//                    // Yahan pe tu TextView me show kar sakta hai
//                    Log.d("History", "Start: $startAddress\nEnd: $endAddress")
//                }
//            }
//        }
//
//        Log.d("History", "Distance: $distance meters, Duration: $duration seconds")
    }

    private fun calculateDistance(points: List<LatLng>): Float {
        var total = 0f
        for (i in 0 until points.size - 1) {
            val result = FloatArray(1)
            Location.distanceBetween(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude, result
            )
            total += result[0]
        }
        return total
    }

    private fun getAddressFromLatLng(latLng: LatLng, callback: (String) -> Unit) {
        val geocoder = Geocoder(this, Locale.getDefault())
        Thread {
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown"
                runOnUiThread { callback(address) }
            } catch (e: Exception) {
                runOnUiThread { callback("Error: ${e.message}") }
            }
        }.start()
    }
}