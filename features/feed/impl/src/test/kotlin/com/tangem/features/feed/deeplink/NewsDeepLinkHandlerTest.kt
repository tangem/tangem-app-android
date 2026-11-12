package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NEWS_ID_KEY
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NewsDeepLinkHandlerTest {

    private val router: Router = mockk()

    private val handler = NewsDeepLinkHandler()

    @BeforeEach
    fun setUp() {
        every { router.push(any(), any()) } just Runs
    }

    @Test
    fun `no params opens news list with null category`() {
        handler.handle(createRequest(queryParams = emptyMap()))

        verify { router.push(AppRoute.News(categoryId = null), any()) }
    }

    @Test
    fun `valid categoryId opens news list with that category`() {
        handler.handle(createRequest(queryParams = mapOf(CATEGORY_ID_KEY to "5")))

        verify { router.push(AppRoute.News(categoryId = 5), any()) }
    }

    @Test
    fun `unknown categoryId is forwarded — sanitized in NewsListModel against loaded chips`() {
        handler.handle(createRequest(queryParams = mapOf(CATEGORY_ID_KEY to "42")))

        verify { router.push(AppRoute.News(categoryId = 42), any()) }
    }

    @Test
    fun `non-integer categoryId is silently dropped`() {
        handler.handle(createRequest(queryParams = mapOf(CATEGORY_ID_KEY to "not-a-number")))

        verify { router.push(AppRoute.News(categoryId = null), any()) }
    }

    @Test
    fun `newsId opens news details`() {
        handler.handle(createRequest(queryParams = mapOf(NEWS_ID_KEY to "20533")))

        verify { router.push(AppRoute.NewsDetails(newsId = 20533), any()) }
    }

    @Test
    fun `newsId takes priority over categoryId`() {
        handler.handle(createRequest(queryParams = mapOf(NEWS_ID_KEY to "20533", CATEGORY_ID_KEY to "5")))

        verify { router.push(AppRoute.NewsDetails(newsId = 20533), any()) }
    }

    private fun createRequest(queryParams: Map<String, String>) = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = queryParams,
        scope = mockk<CoroutineScope>(),
        isFromOnNewIntent = false,
        router = router,
    )
}