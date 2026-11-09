package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.EARN_TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.domain.models.earn.PreselectedEarnType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EarnDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk()

    private val handler = EarnDeepLinkHandler(appRouter = appRouter)

    @BeforeEach
    fun setUp() {
        every { appRouter.push(any(), any()) } just Runs
    }

    @Test
    fun `no params opens earn list with no filters`() {
        handler.handle(createRequest(queryParams = emptyMap()))

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `staking earnType is parsed`() {
        handler.handle(createRequest(queryParams = mapOf(EARN_TYPE_KEY to "staking")))

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Staking, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `yield earnType is parsed`() {
        handler.handle(createRequest(queryParams = mapOf(EARN_TYPE_KEY to "yield")))

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Yield, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `invalid earnType is silently dropped`() {
        handler.handle(createRequest(queryParams = mapOf(EARN_TYPE_KEY to "bogus")))

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `networkId is passed through`() {
        handler.handle(createRequest(queryParams = mapOf(NETWORK_ID_KEY to "base")))

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = "base"),
                any(),
            )
        }
    }

    @Test
    fun `blank networkId is dropped`() {
        handler.handle(createRequest(queryParams = mapOf(NETWORK_ID_KEY to "   ")))

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `earnType and networkId together`() {
        handler.handle(createRequest(queryParams = mapOf(EARN_TYPE_KEY to "yield", NETWORK_ID_KEY to "base")))

        verify {
            appRouter.push(
                AppRoute.Earn(
                    preselectedEarnType = PreselectedEarnType.Yield,
                    preselectedNetworkId = "base",
                ),
                any(),
            )
        }
    }

    private fun createRequest(queryParams: Map<String, String>) = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = queryParams,
        scope = mockk<CoroutineScope>(),
        isFromOnNewIntent = false,
    )
}