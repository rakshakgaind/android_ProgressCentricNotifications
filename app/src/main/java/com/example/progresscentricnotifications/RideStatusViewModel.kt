package com.example.progresscentricnotifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RideStatusViewModel(
    private val notificationBuilder: NotificationBuilder,
) : ViewModel() {

    private val _rideStatus = MutableStateFlow<RideStatus?>(null)
    val rideStatus: StateFlow<RideStatus?> = _rideStatus.asStateFlow()

    private var animateJob: Job? = null

    fun animate() {
        animateJob?.cancel()
        animateJob = viewModelScope.launch {
            // Start with Requested
            update(RideStatus.Requested)
            delay(2.seconds) // Increased delay

            // Move to Driver Assigned
            update(RideStatus.DriverAssigned(driverName = "Viktor", vehicle = "Toyota Camry"))
            delay(2.seconds) // Increased delay

            // Simulate EnRoute progress (driver approaching)
            repeat(150) { index ->
                // Calculate progress from 0.0 to 1.0
                val progress = (index + 1) / 150f
                val etaMinutes = (15 - (index * 0.1)).coerceAtLeast(0.0) // 0.1 = 15 / 150
                val eta = if (etaMinutes > 0.1667) { // 0.1667 min = 10 seconds
                    etaMinutes.minutes
                } else {
                    10.seconds // Final ETA is 10 seconds
                }
                update(
                    RideStatus.EnRoute(
                        progress = progress,
                        eta = eta,
                        distance = "${5000 - (index * 5000 / 150)} m", // Smoother distance decrease
                        driverName = "Viktor",
                    )
                )
                delay(100.milliseconds) // 150 * 100ms = 15 seconds total
            }

            // Pause at Arrived
            update(RideStatus.Arrived)
            delay(10.seconds) // Longer pause to simulate driver waiting

            // Simulate InProgress (ride to destination)
            repeat(100) { index ->
                update(
                    RideStatus.InProgress(
                        progress = (index + 1) / 100f,
                        eta = (30 - (index / 3)).minutes,
                        distance = "${10000 - (index * 100)} m",
                        driverName = "Viktor",
                    )
                )
                delay(100.milliseconds) // Slower progress update
            }

            // Move to Completed
            delay(1.seconds) // Increased delay
            update(RideStatus.Completed)
        }
    }

    fun set(rideStatus: RideStatus) {
        animateJob?.cancel()
        update(rideStatus)
    }

    private fun update(rideStatus: RideStatus) {
        println("Updating ride status: $rideStatus") // Debug log
        _rideStatus.update { rideStatus }
        notificationBuilder.show(rideStatus)
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                return RideStatusViewModel(
                    notificationBuilder = NotificationBuilderImpl(context = application)
                ) as T
            }
        }
    }
}