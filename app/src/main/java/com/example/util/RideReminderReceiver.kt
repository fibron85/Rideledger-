package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.RideLedgerApp
import com.example.domain.RideCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RideReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val slot = intent.getStringExtra(RideReminderScheduler.EXTRA_REMINDER_SLOT)
            ?: RideReminderScheduler.SLOT_EVENING_4PM
        val isEveningSlot = slot != RideReminderScheduler.SLOT_MORNING_4AM

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = appContext as? RideLedgerApp
                val sessionManager = app?.sessionManager
                val enabled = sessionManager?.remindersEnabled?.first() ?: true
                if (enabled) {
                    // Schedule the next occurrence cleanly
                    RideReminderScheduler.scheduleDailyReminders(
                        context = appContext,
                        nowMillis = System.currentTimeMillis() + 60_000L
                    )
                    // 1. Daily ride entry reminder
                    RideReminderScheduler.showRideReminderNotification(
                        context = appContext,
                        isEveningSlot = isEveningSlot,
                        isTest = false
                    )
                    // 2. Finance due-payment submission reminder if unsubmitted shifts exist
                    val pendingFinance = app?.repository?.getPendingFinanceSummaryForActiveDriver()
                    if (pendingFinance != null) {
                        val (currencyCode, financeState) = pendingFinance
                        if (financeState.pendingShifts.isNotEmpty()) {
                            RideReminderScheduler.showFinanceReminderNotification(
                                context = appContext,
                                isEveningSlot = isEveningSlot,
                                pendingShiftCount = financeState.pendingShifts.size,
                                totalDueText = RideCalculator.formatMoney(financeState.totalPendingDueGross, currencyCode),
                                cashText = RideCalculator.formatMoney(financeState.cashOnHandGross, currencyCode),
                                otherDueText = RideCalculator.formatMoney(financeState.otherPaymentDueGross, currencyCode),
                                isTest = false
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionManager = (appContext as? RideLedgerApp)?.sessionManager
                val enabled = sessionManager?.remindersEnabled?.first() ?: true
                if (enabled) {
                    RideReminderScheduler.scheduleDailyReminders(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
