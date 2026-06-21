package com.ywb.focusguard.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DestinationTest {
    @Test
    fun `专注页和专注详情页使用不同 route`() {
        assertNotEquals(Destination.Session.route, Destination.SessionDetail.route)
    }

    @Test
    fun `专注详情页 route 能按 sessionId 生成`() {
        assertEquals("session_detail/42", Destination.SessionDetail.createRoute(42L))
    }
}
