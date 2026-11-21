package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.FinanceUiState
import com.example.domain.RideCalculator
import java.util.Calendar

data class InAppReminderPrompt(
    val slotKey: String,
    val slotLabel: String,
    val title: String,
    val message: String
)

object RideReminderScheduler {

    const val CHANNEL_ID = "ride_entry_reminders"
    const val CHANNEL_NAME = "Daily Ride Entry Reminders"
    const val CHANNEL_ID_FINANCE = "finance_due_reminders"
    const val CHANNEL_NAME_FINANCE = "Finance Payment Reminders"

    const val ACTION_DAILY_RIDE_REMINDER = "com.example.ACTION_DAILY_RIDE_REMINDER"
    const val EXTRA_REMINDER_SLOT = "extra_reminder_slot"
    const val EXTRA_OPEN_ADD_RIDE = "extra_open_add_ride"
    const val EXTRA_OPEN_FINANCE = "extra_open_finance"

    const val SLOT_EVENING_4PM = "4pm"
    const val SLOT_MORNING_4AM = "4am"

    private const val REQUEST_CODE_4PM = 4016
    private const val REQUEST_CODE_4AM = 4004

    private const val NOTIFICATION_ID_4PM = 5016
    private const val NOTIFICATION_ID_4AM = 5004
    private const val NOTIFICATION_ID_TEST = 5099

    private const val NOTIFICATION_ID_FINANCE_4PM = 5116
    private const val NOTIFICATION_ID_FINANCE_4AM = 5104
    private const val NOTIFICATION_ID_FINANCE_TEST = 5199

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appContext = context.applicationContext ?: context
            runCatching {
                val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val rideChannel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Periodic daily reminders at 4:00 PM and 4:00 AM to log your rides."
                        enableVibration(true)
                    }
                    manager.createNotificationChannel(rideChannel)
                }
                if (manager.getNotificationChannel(CHANNEL_ID_FINANCE) == null) {
                    val financeChannel = NotificationChannel(
                        CHANNEL_ID_FINANCE,
                        CHANNEL_NAME_FINANCE,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Daily reminders at 4:00 PM and 4:00 AM to submit pending due payments to Finance."
                        enableVibration(true)
                    }
                    manager.createNotificationChannel(financeChannel)
                }
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val appContext = context.applicationContext ?: context
        val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        } else {
            true
        }
        val systemEnabled = runCatching {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }.getOrDefault(true)
        return permissionGranted && systemEnabled
    }

    fun openAppNotificationSettings(context: Context): Boolean {
        val appSettingsAttempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        } else {
            false
        }
        if (appSettingsAttempt) return true

        return runCatching {
            val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(detailsIntent)
            true
        }.getOrDefault(false)
    }

    fun nextDailyTriggerMillis(
        nowMillis: Long,
        hourOfDay: Int,
        minute: Int = 0
    ): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= nowMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }

    fun activeInAppReminder(nowMillis: Long = System.currentTimeMillis()): InAppReminderPrompt? {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dateIso = RideCalculator.toIsoDate(nowMillis)
        return when {
            hour in 16..19 -> InAppReminderPrompt(
                slotKey = "${dateIso}_16",
                slotLabel = "4:00 PM Reminder",
                title = "4:00 PM Daily Ride Reminder",
                message = "Time to log your shift rides in RideLedger so your daily income stays up to date."
            )
            hour in 4..7 -> InAppReminderPrompt(
                slotKey = "${dateIso}_04",
                slotLabel = "4:00 AM Reminder",
                title = "4:00 AM Shift Wrap-Up Reminder",
                message = "Night shift wrapping up? Remember to enter any remaining rides before finishing."
            )
            else -> null
        }
    }

    fun activeFinanceInAppReminder(
        nowMillis: Long = System.currentTimeMillis(),
        financeState: FinanceUiState,
        currencyCode: String
    ): InAppReminderPrompt? {
        if (financeState.pendingShifts.isEmpty()) return null
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dateIso = RideCalculator.toIsoDate(nowMillis)
        val totalText = RideCalculator.formatMoney(financeState.totalPendingDueGross, currencyCode)
        val cashText = RideCalculator.formatMoney(financeState.cashOnHandGross, currencyCode)
        val otherText = RideCalculator.formatMoney(financeState.otherPaymentDueGross, currencyCode)
        val detail = "${financeState.pendingShifts.size} shift(s) pending ($totalText) — Cash: $cashText, Other Due: $otherText. Tap Submit Finance to settle."

        return when {
            hour in 16..19 -> InAppReminderPrompt(
                slotKey = "${dateIso}_16_fin",
                slotLabel = "4:00 PM Finance Due",
                title = "4:00 PM Finance Payment Reminder",
                message = detail
            )
            hour in 4..7 -> InAppReminderPrompt(
                slotKey = "${dateIso}_04_fin",
                slotLabel = "4:00 AM Finance Due",
                title = "4:00 AM Finance Payment Reminder",
                message = detail
            )
            else -> null
        }
    }

    fun scheduleDailyReminders(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext ?: context
        ensureNotificationChannel(appContext)
        scheduleSlot(
            context = appContext,
            slot = SLOT_EVENING_4PM,
            requestCode = REQUEST_CODE_4PM,
            triggerAtMillis = nextDailyTriggerMillis(nowMillis, hourOfDay = 16, minute = 0)
        )
        scheduleSlot(
            context = appContext,
            slot = SLOT_MORNING_4AM,
            requestCode = REQUEST_CODE_4AM,
            triggerAtMillis = nextDailyTriggerMillis(nowMillis, hourOfDay = 4, minute = 0)
        )
    }

    fun cancelDailyReminders(context: Context) {
        val appContext = context.applicationContext ?: context
        runCatching {
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            alarmManager.cancel(buildAlarmPendingIntent(appContext, SLOT_EVENING_4PM, REQUEST_CODE_4PM))
            alarmManager.cancel(buildAlarmPendingIntent(appContext, SLOT_MORNING_4AM, REQUEST_CODE_4AM))
        }
    }

    @SuppressLint("MissingPermission")
    fun showRideReminderNotification(
        context: Context,
        isEveningSlot: Boolean,
        isTest: Boolean = false
    ): Boolean {
        val appContext = context.applicationContext ?: context
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) {
            return false
        }

        val title = when {
            isTest -> "RideLedger 4 PM & 4 AM Ride Reminder Ready"
            isEveningSlot -> "4:00 PM Daily Ride Reminder"
            else -> "4:00 AM Shift Wrap-Up Reminder"
        }
        val body = when {
            isTest -> "Daily ride entry reminders are active for 4:00 PM and 4:00 AM. Tap to log a ride."
            isEveningSlot -> "Time to enter today's rides in RideLedger so your daily income and platform breakdown stay accurate."
            else -> "Night shift wrapping up? Log any remaining rides before finishing your shift."
        }

        val notificationId = when {
            isTest -> NOTIFICATION_ID_TEST
            isEveningSlot -> NOTIFICATION_ID_4PM
            else -> NOTIFICATION_ID_4AM
        }

        return runCatching {
            val tapIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_ADD_RIDE, true)
            }
            val tapPendingIntent = PendingIntent.getActivity(
                appContext,
                notificationId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_ride)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(tapPendingIntent)
                .addAction(R.drawable.ic_notification_ride, "New Ride", tapPendingIntent)
                .build()

            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun showFinanceReminderNotification(
        context: Context,
        isEveningSlot: Boolean,
        pendingShiftCount: Int,
        totalDueText: String,
        cashText: String,
        otherDueText: String,
        isTest: Boolean = false
    ): Boolean {
        val appContext = context.applicationContext ?: context
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) {
            return false
        }

        val title = when {
            isTest -> "Finance Payment Reminder (4 PM & 4 AM)"
            isEveningSlot -> "4:00 PM Finance Payment Due"
            else -> "4:00 AM Finance Payment Due"
        }
        val body = if (pendingShiftCount > 0) {
            "You have $pendingShiftCount shift(s) pending ($totalDueText) — Cash: $cashText, Other Due: $otherDueText. Tap to submit to Finance."
        } else {
            "Daily Finance payment check is active at 4:00 PM & 4:00 AM. When shifts are pending submission, you'll be reminded here."
        }

        val notificationId = when {
            isTest -> NOTIFICATION_ID_FINANCE_TEST
            isEveningSlot -> NOTIFICATION_ID_FINANCE_4PM
            else -> NOTIFICATION_ID_FINANCE_4AM
        }

        return runCatching {
            val tapIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_FINANCE, true)
            }
            val tapPendingIntent = PendingIntent.getActivity(
                appContext,
                notificationId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_FINANCE)
                .setSmallIcon(R.drawable.ic_notification_ride)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(tapPendingIntent)
                .addAction(R.drawable.ic_notification_ride, "Open Finance", tapPendingIntent)
                .build()

            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    private fun scheduleSlot(
        context: Context,
        slot: String,
        requestCode: Int,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(context, slot, requestCode)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.onFailure {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        slot: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, RideReminderReceiver::class.java).apply {
            action = ACTION_DAILY_RIDE_REMINDER
            putExtra(EXTRA_REMINDER_SLOT, slot)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
