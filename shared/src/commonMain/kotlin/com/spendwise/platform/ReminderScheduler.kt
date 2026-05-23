package com.spendwise.platform

import com.spendwise.domain.ExpenseReminder

interface ReminderScheduler {
    fun schedule(reminders: List<ExpenseReminder>)
}
