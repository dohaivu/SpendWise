package com.spendwise.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OthersPaneBackNavigationTest {
    @Test
    fun nestedOthersPanesNavigateBackToTheirParent() {
        assertNull(SettingsPane.Home.backDestination())
        assertEquals(SettingsPane.Home, SettingsPane.AnnualReport.backDestination())
        assertEquals(SettingsPane.Home, SettingsPane.CategoryList.backDestination())
        assertEquals(SettingsPane.Home, SettingsPane.TagUsage.backDestination())
        assertEquals(SettingsPane.CategoryList, SettingsPane.CategoryEditor.backDestination())
    }
}
