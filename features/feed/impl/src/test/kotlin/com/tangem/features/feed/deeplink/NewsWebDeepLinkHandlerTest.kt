package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.features.feed.entry.deeplink.NewsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDetailsDeepLinkHandler
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

    private val newsDeepLinkHandlerFactory: NewsDeepLinkHandler.Factory = mockk(relaxed = true)
    private val newsDetailsDeepLinkHandlerFactory: NewsDetailsDeepLinkHandler.Factory = mockk(relaxed = true)
    private val scope: CoroutineScope = mockk()

    private val handler = NewsWebDeepLinkHandler(
        newsDeepLinkHandlerFactory = newsDeepLinkHandlerFactory,
        newsDetailsDeepLinkHandlerFactory = newsDetailsDeepLinkHandlerFactory,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(newsDeepLinkHandlerFactory, newsDetailsDeepLinkHandlerFactory)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun handle(model: HandleModel) {
        // Arrange
        val uri = mockk<Uri> { every { path } returns model.path }
        val queryParams = mapOf("category_id" to "5")

        // Act
        handler.handle(
            DeepLinkRequest(uri = uri, queryParams = queryParams, scope = scope, isFromOnNewIntent = false),
        )

        // Assert
        if (model.isArticle) {
            verify { newsDetailsDeepLinkHandlerFactory.create(coroutineScope = scope, deeplinkUri = uri) }
            verify(inverse = true) { newsDeepLinkHandlerFactory.create(any()) }
        } else {
            verify { newsDeepLinkHandlerFactory.create(queryParams = queryParams) }
            verify(inverse = true) { newsDetailsDeepLinkHandlerFactory.create(any(), any()) }
        }
    }

    data class HandleModel(val path: String, val isArticle: Boolean)

    private fun provideTestModels() = listOf(
        // An article link opens the article
        HandleModel(path = "/news/markets/42-some-slug", isArticle = true),
        // Article-less links open the news list
        HandleModel(path = "/news", isArticle = false),
        HandleModel(path = "/news/markets", isArticle = false),
        HandleModel(path = "/news/markets/not-a-number", isArticle = false),
    )
}