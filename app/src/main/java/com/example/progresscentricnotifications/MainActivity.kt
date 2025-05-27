package com.example.progresscentricnotifications

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.progresscentricnotifications.databinding.ActivityPermissionDeniedBinding
import com.example.progresscentricnotifications.databinding.ActivityRideStatusBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: RideStatusViewModel
    private val PERMISSION_REQUEST_CODE = 1001
    private var rideStatusBinding: ActivityRideStatusBinding? = null
    private var permissionDeniedBinding: ActivityPermissionDeniedBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, RideStatusViewModel.Factory)[RideStatusViewModel::class.java]

        // Check notification permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            rideStatusBinding = ActivityRideStatusBinding.inflate(layoutInflater)
            setContentView(rideStatusBinding!!.root)
            setupRideStatusScreen()
        } else {
            permissionDeniedBinding = ActivityPermissionDeniedBinding.inflate(layoutInflater)
            setContentView(permissionDeniedBinding!!.root)
            setupPermissionDeniedScreen()
        }
    }

    private fun setupPermissionDeniedScreen() {
        permissionDeniedBinding?.requestPermissionButton?.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_REQUEST_CODE
            )
        }
    }



    private fun setupRideStatusScreen() {
        rideStatusBinding?.let { binding ->
            // Observe ride status changes
            lifecycleScope.launch {
                viewModel.rideStatus.collectLatest { rideStatus ->
                    binding.rideStatusText.text = "Ride status: $rideStatus"
                    // Apply status-specific color
                    val colorRes = when (rideStatus) {
                        is RideStatus.Requested -> R.color.ride_status_requested
                        is RideStatus.DriverAssigned -> R.color.ride_status_driver_assigned
                        is RideStatus.EnRoute -> R.color.blue
                        is RideStatus.Arrived -> R.color.ride_status_arrived
                        is RideStatus.InProgress -> R.color.ride_status_in_progress
                        is RideStatus.Completed -> R.color.ride_status_completed
                        null -> android.R.color.darker_gray // Default color
                    }
                    binding.rideStatusText.setTextColor(ContextCompat.getColor(this@MainActivity, colorRes))
                }
            }

            // Set button click listeners
            binding.animateButton.setOnClickListener { viewModel.animate() }
            binding.requestedButton.setOnClickListener { viewModel.set(RideStatus.Requested) }
            binding.driverAssignedButton.setOnClickListener {
                viewModel.set(RideStatus.DriverAssigned(driverName = "Viktor", vehicle = "Toyota Camry"))
            }
            binding.enRouteButton.setOnClickListener {
                viewModel.set(
                    RideStatus.EnRoute(
                        progress = 0.33f,
                        eta = 15.minutes,
                        distance = "5000 m",
                        driverName = "Viktor"
                    )
                )
            }
            binding.arrivedButton.setOnClickListener { viewModel.set(RideStatus.Arrived) }
            binding.inProgressButton.setOnClickListener {
                viewModel.set(
                    RideStatus.InProgress(
                        progress = 0.33f,
                        eta = 30.minutes,
                        distance = "10000 m",
                        driverName = "Viktor"
                    )
                )
            }
            binding.completedButton.setOnClickListener { viewModel.set(RideStatus.Completed) }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                rideStatusBinding = ActivityRideStatusBinding.inflate(layoutInflater)
                setContentView(rideStatusBinding!!.root)
                permissionDeniedBinding = null
                setupRideStatusScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rideStatusBinding = null
        permissionDeniedBinding = null
    }
}