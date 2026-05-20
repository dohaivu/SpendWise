package com.spendwise.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackNavigationTest {
    @Test
    fun mainTabsNavigateBackToInput() {
        assertNull(SpendWiseTab.Expense.backDestination())
        assertEquals(SpendWiseTab.Expense, SpendWiseTab.Calendar.backDestination())
        assertEquals(SpendWiseTab.Expense, SpendWiseTab.Report.backDestination())
        assertEquals(SpendWiseTab.Expense, SpendWiseTab.Settings.backDestination())
    }
}
