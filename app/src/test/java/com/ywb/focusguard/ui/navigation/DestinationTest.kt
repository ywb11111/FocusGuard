package com.ywb.focusguard.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 防止专注页与详情页 route 再次冲突，并保护带 id 路由的生成格式。 */
class DestinationTest {
    @Test
    fun `专注页和专注详情页使用不同 route`() {
        assertNotEquals(Destination.Session.route, Destination.SessionDetail.route)
    }

    @Test
    fun `专注详情页 route 能按 sessionId 生成`() {
        assertEquals("session_detail/42", Destination.SessionDetail.createRoute(42L))
    }

    @Test
    fun `桌面入口时长会限制在合法范围`() {
        assertEquals("session/prepare/25", Destination.SessionQuickStart.createRoute(25))
        assertEquals("session/prepare/180", Destination.SessionQuickStart.createRoute(999))
    }

    @Test
    fun `底部导航页面之间使用顶层转场`() {
        assertEquals(
            true,
            isTopLevelTransition(Destination.Today.route, Destination.Reports.route)
        )
    }

    @Test
    fun `进入沉浸任务或详情页不使用顶层转场`() {
        assertEquals(
            false,
            isTopLevelTransition(Destination.Today.route, Destination.Session.route)
        )
        assertEquals(
            false,
            isTopLevelTransition(Destination.Reports.route, Destination.SessionDetail.route)
        )
    }
}
