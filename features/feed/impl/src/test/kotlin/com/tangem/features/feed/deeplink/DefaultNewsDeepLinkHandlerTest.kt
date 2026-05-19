package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NEWS_ID_KEY
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultNewsDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk()

    @BeforeEach
    fun setUp() {
        every { appRouter.push(any(), any()) } just Runs
    }

    @Test
    fun `no params opens news list with null category`() {
        DefaultNewsDeepLinkHandler(queryParams = emptyMap(), appRouter = appRouter)

        verify { appRouter.push(AppRoute.News(categoryId = null), any()) }
    }

    @Test
    fun `valid categoryId opens news list with that category`() {
        DefaultNewsDeepLinkHandler(
            queryParams = mapOf(CATEGORY_ID_KEY to "5"),
            appRouter = appRouter,
        )

        verify { appRouter.push(AppRoute.News(categoryId = 5), any()) }
    }

    @Test
    fun `unknown categoryId is forwarded — sanitized in NewsListModel against loaded chips`() {
        DefaultNewsDeepLinkHandler(
            queryParams = mapOf(CATEGORY_ID_KEY to "42"),
            appRouter = appRouter,
        )

        verify { appRouter.push(AppRoute.News(categoryId = 42), any()) }
    }

    @Test
    fun `non-integer categoryId is silently dropped`() {
        DefaultNewsDeepLinkHandler(
            queryParams = mapOf(CATEGORY_ID_KEY to "not-a-number"),
            appRouter = appRouter,
        )

        verify { appRouter.push(AppRoute.News(categoryId = null), any()) }
    }

    @Test
    fun `newsId opens news details`() {
        DefaultNewsDeepLinkHandler(
            queryParams = mapOf(NEWS_ID_KEY to "20533"),
            appRouter = appRouter,
        )

        verify { appRouter.push(AppRoute.NewsDetails(newsId = 20533), any()) }
    }

    @Test
    fun `newsId takes priority over categoryId`() {
        DefaultNewsDeepLinkHandler(
            queryParams = mapOf(NEWS_ID_KEY to "20533", CATEGORY_ID_KEY to "5"),
            appRouter = appRouter,
        )

        verify { appRouter.push(AppRoute.NewsDetails(newsId = 20533), any()) }
    }
}