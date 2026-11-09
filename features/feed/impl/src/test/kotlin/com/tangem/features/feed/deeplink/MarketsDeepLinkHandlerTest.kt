package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.INTERVAL_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.ORDER_KEY
import com.tangem.domain.markets.PreselectedMarketsInterval
import com.tangem.domain.markets.PreselectedMarketsOrder
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketsDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)

    private val handler = MarketsDeepLinkHandler(appRouter = appRouter)

    @BeforeEach
    fun resetMocks() {
        clearMocks(appRouter)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN query params WHEN handled THEN markets open with the parsed order and interval`(model: HandleModel) {
        // Arrange
        val request = DeepLinkRequest(
            uri = mockk<Uri>(),
            queryParams = model.queryParams,
            scope = mockk<CoroutineScope>(),
            isFromOnNewIntent = false,
        )

        // Act
        handler.handle(request)

        // Assert
        verify(exactly = 1) { appRouter.push(model.expectedRoute, any()) }
    }

    internal data class HandleModel(val queryParams: Map<String, String>, val expectedRoute: AppRoute.Markets)

    private fun provideTestModels() = listOf(
        HandleModel(queryParams = emptyMap(), expectedRoute = AppRoute.Markets()),
        HandleModel(
            queryParams = mapOf(ORDER_KEY to "gainers", INTERVAL_KEY to "1w"),
            expectedRoute = AppRoute.Markets(
                preselectedOrder = PreselectedMarketsOrder.Gainers,
                preselectedInterval = PreselectedMarketsInterval.W1,
            ),
        ),
        HandleModel(
            queryParams = mapOf(ORDER_KEY to "trending"),
            expectedRoute = AppRoute.Markets(preselectedOrder = PreselectedMarketsOrder.Trending),
        ),
        // Unknown values are dropped instead of failing the deep link
        HandleModel(
            queryParams = mapOf(ORDER_KEY to "unknown", INTERVAL_KEY to "1y"),
            expectedRoute = AppRoute.Markets(),
        ),
    )
}