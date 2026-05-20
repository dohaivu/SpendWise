package com.spendwise.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OthersPaneBackNavigationTest {
    @Test
    fun nestedOthersPanesNavigateBackToTheirParent() {
        assertNull(OthersPane.Home.backDestination())
        assertEquals(OthersPane.Home, OthersPane.CategoryList.backDestination())
        assertEquals(OthersPane.Home, OthersPane.TagUsage.backDestination())
        assertEquals(OthersPane.CategoryList, OthersPane.CategoryEditor.backDestination())
    }
}
