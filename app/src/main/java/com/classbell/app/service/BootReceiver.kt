package com.classbell.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.classbell.app.ClassBellApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val app = context.applicationContext as? ClassBellApp ?: return
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val warnMin = app.settingsRepository.endWarnMinutesFlow.firstOrNull() ?: 5
                    val today = LocalDate.now()
                    val slots = app.database.periodSlotDao().getAllPeriodSlotsSync()
                    ClassBellApp.cachedPeriodSlots = slots

                    // Reschedule today's meetings with sub-period precision
                    val meetings = app.scheduleRepository.getMeetingsOnDate(today).firstOrNull() ?: emptyList()
                    for (m in meetings) {
                        app.alarmScheduler.scheduleClassAlarms(m, warnMin, slots)
                    }

                    // Reschedule upcoming affairs
                    val affairs = app.affairRepository.getAllAffairsSync()
                    for (a in affairs) {
                        app.alarmScheduler.scheduleAffairAlarm(a)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
