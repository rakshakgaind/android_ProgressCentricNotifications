package com.example.progresscentricnotifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap

interface NotificationBuilder {
    fun show(rideStatus: RideStatus)
}

class NotificationBuilderImpl(
    private val context: Context,
) : NotificationBuilder {

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun show(rideStatus: RideStatus) = show { builder ->
        println("Notification Ride Status $rideStatus")
        // Two segments: in route and in progress
        val enRouteSegment = Notification.ProgressStyle.Segment(100)
            .setColor(context.getColor(R.color.ride_status_en_route))
        val inProgressSegment = Notification.ProgressStyle.Segment(100)
            .setColor(context.getColor(R.color.ride_status_in_progress))
        val progressSegments = listOf(
            enRouteSegment,
            inProgressSegment,
        )

        val progress = (when (rideStatus) {
            is RideStatus.Requested -> 0
            is RideStatus.DriverAssigned -> 0
            is RideStatus.EnRoute -> (rideStatus.progress * 100).toInt()
            is RideStatus.Arrived -> enRouteSegment.length
            is RideStatus.InProgress -> enRouteSegment.length + (rideStatus.progress * 100).toInt()
            is RideStatus.Completed -> enRouteSegment.length + inProgressSegment.length
        }).coerceIn(minimumValue = 0, maximumValue = progressSegments.sumOf { it.length })

        val style = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(progress)
            .setProgressSegments(progressSegments)
            .setProgressPoints(
                listOf(
                    Notification.ProgressStyle.Point(enRouteSegment.length)
                        .setColor(context.getColor(R.color.ride_status_arrived))
                )
            )

        // Change icon based on the status
        when (rideStatus) {
            is RideStatus.Requested -> {
                builder.setSmallIcon(R.drawable.ic_notification_requested)
            }

            is RideStatus.DriverAssigned -> {
                builder.setSmallIcon(R.drawable.ic_notification_driver_assigned)
            }

            is RideStatus.EnRoute -> {
                builder.setSmallIcon(R.drawable.ic_notification_in_progress)
                style.progressTrackerIcon =
                    Icon.createWithResource(context, R.drawable.ic_notification_en_route)
                builder.style = style
            }

            is RideStatus.Arrived -> {
                builder.setSmallIcon(R.drawable.ic_notification_arrived)
                style.progressTrackerIcon =
                    Icon.createWithResource(context, R.drawable.ic_notification_arrived)
                builder.style = style
            }

            is RideStatus.InProgress -> {
                builder.setSmallIcon(R.drawable.ic_notification_in_progress)
                style.progressTrackerIcon = Icon.createWithResource(context, R.drawable.ic_notification_en_route)
                builder.style = style
            }

            is RideStatus.Completed -> {
                builder.setSmallIcon(R.drawable.ic_notification_completed)
            }
        }

        val title = when (rideStatus) {
            is RideStatus.Requested -> "Ride requested"
            is RideStatus.DriverAssigned -> "Driver assigned"
            is RideStatus.EnRoute -> "Driver en route"
            is RideStatus.Arrived -> "Driver has arrived"
            is RideStatus.InProgress -> "Ride in progress"
            is RideStatus.Completed -> "Ride completed"
        }

        val text = when (rideStatus) {
            is RideStatus.Requested -> "Waiting for a driver"
            is RideStatus.DriverAssigned -> "${rideStatus.driverName} is assigned with ${rideStatus.vehicle}"
            is RideStatus.EnRoute -> "${rideStatus.driverName} is on the way, arriving in ${rideStatus.eta}"
            is RideStatus.Arrived -> "Your driver is waiting at the pickup location"
            is RideStatus.InProgress -> "${rideStatus.driverName} is driving, ${rideStatus.distance} to destination"
            is RideStatus.Completed -> "Thank you for riding!"
        }

        builder.setContentTitle(title)
               .setContentText(text)
               .setAutoCancel(false)
               .build()
    }

    private inline fun show(block: (Notification.Builder) -> Unit) {
        val notificationManager = context.getSystemService<NotificationManager>()
            ?: error("Can't find NotificationManager")

        val channelId = "progress.centric"
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_title),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = Notification.Builder(context, channelId)
            .apply(block)
            .build()



        notificationManager.notify(1, notification)
    }
}