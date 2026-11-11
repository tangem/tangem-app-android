package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NEWS_ID_KEY
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NewsDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk()

    private val handler = NewsDeepLinkHandler(appRouter = appRouter)

    @BeforeEach
    fun setUp() {
        every { appRouter.push(any(), any()) } just Runs
    }

    @Test
    fun `no params opens news list with null category`() {
        handler.handle(createRequest(queryParams = emptyMap()))

        verify { appRouter.push(AppRoute.News(categoryId = null), any()) }
    }

    @Test
    fun `valid categoryId opens news list with that category`() {
        handler.handle(createRequest(queryParams = mapOf(CATEGORY_ID_KEY to "5")))

        verify { appRouter.push(AppRoute.News(categoryId = 5), any()) }
    }

    @Test
    fun `unknown categoryId is forwarded — sanitized in NewsListModel against loaded chips`() {
        handler.handle(createRequest(queryParams = mapOf(CATEGORY_ID_KEY to "42")))

        verify { appRouter.push(AppRoute.News(categoryId = 42), any()) }
    }

    @Test
    fun `non-integer categoryId is silently dropped`() {
        handler.handle(createRequest(queryParams = mapOf(CATEGORY_ID_KEY to "not-a-number")))

        verify { appRouter.push(AppRoute.News(categoryId = null), any()) }
    }

    @Test
    fun `newsId opens news details`() {
        handler.handle(createRequest(queryParams = mapOf(NEWS_ID_KEY to "20533")))

        verify { appRouter.push(AppRoute.NewsDetails(newsId = 20533), any()) }
    }

    @Test
    fun `newsId takes priority over categoryId`() {
        handler.handle(createRequest(queryParams = mapOf(NEWS_ID_KEY to "20533", CATEGORY_ID_KEY to "5")))

        verify { appRouter.push(AppRoute.NewsDetails(newsId = 20533), any()) }
    }

    private fun createRequest(queryParams: Map<String, String>) = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = queryParams,
        scope = mockk<CoroutineScope>(),
        isFromOnNewIntent = false,
    )
}