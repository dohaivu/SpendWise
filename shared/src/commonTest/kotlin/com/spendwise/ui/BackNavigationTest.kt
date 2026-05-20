package com.spendwise.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackNavigationTest {
    @Test
    fun mainTabsNavigateBackToInput() {
        assertNull(SpendWiseTab.Input.backDestination())
        assertEquals(SpendWiseTab.Input, SpendWiseTab.Calendar.backDestination())
        assertEquals(SpendWiseTab.Input, SpendWiseTab.Report.backDestination())
        assertEquals(SpendWiseTab.Input, SpendWiseTab.Others.backDestination())
    }
}
