package com.reelvoice.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.reelvoice.MainActivity
import com.reelvoice.R
import java.util.concurrent.TimeUnit

/**
 * Retention notifications (Phase 2 of the retention plan).
 *
 * All local — no Firebase needed:
 *  1. Day-1 nudge — fired ~20h after the user's first successful clean.
 *  2. Weekly quota-refresh reminder — repeating weekly after first session.
 *  3. "Audio ready" — posted when processing completes while the app is in
 *     the background, so users don't lose their cleaned audio.
 *
 * FCM server-side push can layer on top of this later; the channels and
 * permission handling are already here.
 */
object RetentionNotifications {

    const val CHANNEL_RETENTION = "retention"
    const val CHANNEL_DELIVERY = "delivery"

    private const val PREFS = "retention_prefs"
    private const val KEY_DAY1_SCHEDULED = "day1_scheduled"
    private const val KEY_WEEKLY_SCHEDULED = "weekly_scheduled"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RETENTION,
                "ReelVoice tips & reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Gentle nudges to keep creating with ReelVoice" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DELIVERY,
                "Finished audio",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts when your cleaned audio or reel is ready" }
        )
    }

    /** Can we post right now? (API 33+ needs the runtime notification permission.) */
    fun canPost(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    /**
     * Day-1 nudge: fires ~20h after the first successful clean, once ever.
     * Safe to call from any success path — de-dupes itself.
     */
    fun scheduleDay1Nudge(ctx: Context) {
        if (prefs(ctx).getBoolean(KEY_DAY1_SCHEDULED, false)) return
        prefs(ctx).edit().putBoolean(KEY_DAY1_SCHEDULED, true).apply()
        val request = OneTimeWorkRequestBuilder<Day1Worker>()
            .setInitialDelay(20, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork("retention_day1_nudge", ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Weekly quota-refresh reminder. Self-perpetuating one-time chain:
     * each [QuotaWorker] run re-enqueues itself 7 days later. Chained one-time
     * work (instead of PeriodicWork) guarantees the first fire is a full week
     * after the first session, never immediately on install.
     */
    fun scheduleWeeklyQuotaReminder(ctx: Context) {
        if (prefs(ctx).getBoolean(KEY_WEEKLY_SCHEDULED, false)) return
        prefs(ctx).edit().putBoolean(KEY_WEEKLY_SCHEDULED, true).apply()
        enqueueNextQuotaReminder(ctx)
    }

    /** Internal + used by [QuotaWorker] to re-arm next week's reminder. */
    fun enqueueNextQuotaReminder(ctx: Context) {
        val request = OneTimeWorkRequestBuilder<QuotaWorker>()
            .setInitialDelay(7, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork("retention_quota_weekly", ExistingWorkPolicy.REPLACE, request)
    }

    /** "Your clean audio is ready" — called when processing completes in background. */
    fun showAudioReady(ctx: Context, fileName: String) {
        if (!canPost(ctx)) return
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pi = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_DELIVERY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Done! Your clean audio is ready 🎧")
            .setContentText(fileName)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(1001, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and post — ignore.
        }
    }

    /** Day-1 re-engagement nudge, posted by [Day1Worker]. */
    fun showDay1Nudge(ctx: Context) {
        if (!canPost(ctx)) return
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pi = PendingIntent.getActivity(
            ctx, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_RETENTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Your 20 free minutes are waiting 🎙️")
            .setContentText("One tap to clean your voice and make a reel.")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(1002, notification)
        } catch (_: SecurityException) {
        }
    }

    /** Weekly quota refresh reminder, posted by [QuotaWorker]. */
    fun showQuotaRefresh(ctx: Context) {
        if (!canPost(ctx)) return
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pi = PendingIntent.getActivity(
            ctx, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_RETENTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Fresh week, fresh minutes ✅")
            .setContentText("20 free minutes reloaded — make this week's reel.")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(1003, notification)
        } catch (_: SecurityException) {
        }
    }
}

/** Fires the Day-1 re-engagement notification. */
class Day1Worker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        RetentionNotifications.showDay1Nudge(applicationContext)
        return Result.success()
    }
}

/** Fires the weekly quota-refresh notification. */
class QuotaWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        RetentionNotifications.showQuotaRefresh(applicationContext)
        // Re-arm next week's reminder.
        RetentionNotifications.enqueueNextQuotaReminder(applicationContext)
        return Result.success()
    }
}


