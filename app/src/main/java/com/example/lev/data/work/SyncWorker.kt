package com.example.lev.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lev.LevApplication
import java.util.concurrent.TimeUnit

/**
 * Sync periódica em segundo plano (M7 §C): roda quando houver rede, sem
 * bloquear o app. O debounce de ~30 s após alterações vive no
 * SyncRepository (foreground); este Worker cobre o caso do app fechado.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LevApplication
        return try {
            app.container.syncRepository.syncIfAutoDue()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "lev_periodic_sync"

        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
