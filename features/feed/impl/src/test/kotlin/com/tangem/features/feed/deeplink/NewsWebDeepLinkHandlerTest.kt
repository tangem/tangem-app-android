package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NewsWebDeepLinkHandlerTest {

    private val newsDeepLinkHandler: NewsDeepLinkHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val scope: CoroutineScope = mockk()

    private val handler = NewsWebDeepLinkHandler(newsDeepLinkHandler = newsDeepLinkHandler)

    @BeforeEach
    fun resetMocks() {
        clearMocks(newsDeepLinkHandler, router)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun handle(model: HandleModel) {
        // Arrange
        val uri = mockk<Uri> { every { path } returns model.path }
        val queryParams = mapOf("category_id" to "5")

        val request = DeepLinkRequest(
            uri = uri,
            queryParams = queryParams,
            scope = scope,
            isFromOnNewIntent = false,
            router = router,
        )

        // Act
        handler.handle(request)

        // Assert
        if (model.articleId != null) {
            verify { router.push(AppRoute.NewsDetails(newsId = model.articleId), any()) }
            verify(inverse = true) { newsDeepLinkHandler.handle(any()) }
        } else {
            verify { newsDeepLinkHandler.handle(request) }
            verify(inverse = true) { router.push(any(), any()) }
        }
    }

    data class HandleModel(val path: String, val articleId: Int?)

    private fun provideTestModels() = listOf(
        // An article link opens the article
        HandleModel(path = "/news/markets/42-some-slug", articleId = 42),
        // Article-less links open the news list
        HandleModel(path = "/news", articleId = null),
        HandleModel(path = "/news/markets", articleId = null),
        HandleModel(path = "/news/markets/not-a-number", articleId = null),
    )
}