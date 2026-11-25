package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notification.DealNotificationManager

class TestDelayedNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val notificationManager = DealNotificationManager(applicationContext)
        notificationManager.sendDelayedBackgroundTestNotification()
        return Result.success()
    }
}
