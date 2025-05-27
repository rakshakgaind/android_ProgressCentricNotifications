package com.example.progresscentricnotifications

import androidx.annotation.FloatRange
import kotlin.time.Duration

sealed interface RideStatus {

    data object Requested : RideStatus

    data class DriverAssigned(
        val driverName: String,
        val vehicle: String,
    ) : RideStatus

    data class EnRoute(
        @get:FloatRange(from = 0.0, to = 1.0)
        val progress: Float,
        val eta: Duration,
        val distance: String,
        val driverName: String,
    ) : RideStatus

    data object Arrived : RideStatus

    data class InProgress(
        @get:FloatRange(from = 0.0, to = 1.0)
        val progress: Float,
        val eta: Duration,
        val distance: String,
        val driverName: String,
    ) : RideStatus

    data object Completed : RideStatus
}