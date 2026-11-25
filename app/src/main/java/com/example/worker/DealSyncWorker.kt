package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.notification.DealNotificationManager
import com.example.repository.DealRepository

class DealSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "DealSyncWorker background check started")
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val preferencesManager = PreferencesManager(applicationContext)
            val notificationManager = DealNotificationManager(applicationContext)

            val repository = DealRepository(
                dealDao = db.dealDao(),
                preferencesManager = preferencesManager,
                notificationManager = notificationManager
            )

            val result = repository.refreshDeals(force = true, isBackground = true)
            Log.d(TAG, "DealSyncWorker completed: success=${result.success}, newSteam=${result.newSteamDealsCount}, newEpic=${result.newEpicDealsCount}")
            if (result.success) {
                Result.success()
            } else {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in DealSyncWorker", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "DealSyncWorker"
    }
}
