package com.spendwise.platform

import com.spendwise.domain.ExpenseReminder

class IosReminderScheduler : ReminderScheduler {
    override fun schedule(reminders: List<ExpenseReminder>) = Unit
}
