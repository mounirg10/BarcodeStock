package com.mounir.barcodestock.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.mounir.barcodestock.R
import com.mounir.barcodestock.data.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * يفحص المنتجات مرة يوميًا ويرسل إشعارًا لكل منتج تنتهي صلاحيته خلال يومين أو أقل.
 */
class ExpiryWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(context).productDao()
        val threshold = System.currentTimeMillis() + ALERT_DAYS * 24L * 60 * 60 * 1000
        val items = dao.expiringBefore(threshold)

        if (items.isNotEmpty()) {
            createChannel(context)

            val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

            if (allowed) {
                val manager = NotificationManagerCompat.from(context)
                items.forEach { p ->
                    val days = p.daysLeft
                    val text = when {
                        days < 0 -> "انتهت صلاحية المنتج"
                        days == 0L -> "تنتهي الصلاحية اليوم"
                        days == 1L -> "تنتهي الصلاحية غدًا"
                        else -> "تنتهي الصلاحية بعد يومين"
                    }
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("⚠️ ${p.name}")
                        .setContentText("$text — الباركود: ${p.barcode}")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                    manager.notify(p.id.toInt(), notification)
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "expiry_alerts"
        const val WORK_NAME = "daily_expiry_check"
        const val ALERT_DAYS = 2L

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات انتهاء الصلاحية",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "تنبيه قبل يومين من انتهاء صلاحية أي منتج" }
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        }

        /** جدولة الفحص اليومي عند الساعة 9 صباحًا. */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = next.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<ExpiryWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
            )
        }
    }
}
