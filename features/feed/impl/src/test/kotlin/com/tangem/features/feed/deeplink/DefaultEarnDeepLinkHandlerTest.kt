package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.EARN_TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.domain.models.earn.PreselectedEarnType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultEarnDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk()

    @BeforeEach
    fun setUp() {
        every { appRouter.push(any(), any()) } just Runs
    }

    @Test
    fun `no params opens earn list with no filters`() {
        DefaultEarnDeepLinkHandler(queryParams = emptyMap(), appRouter = appRouter)

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `staking earnType is parsed`() {
        DefaultEarnDeepLinkHandler(
            queryParams = mapOf(EARN_TYPE_KEY to "staking"),
            appRouter = appRouter,
        )

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Staking, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `yield earnType is parsed`() {
        DefaultEarnDeepLinkHandler(
            queryParams = mapOf(EARN_TYPE_KEY to "yield"),
            appRouter = appRouter,
        )

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Yield, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `invalid earnType is silently dropped`() {
        DefaultEarnDeepLinkHandler(
            queryParams = mapOf(EARN_TYPE_KEY to "bogus"),
            appRouter = appRouter,
        )

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `networkId is passed through`() {
        DefaultEarnDeepLinkHandler(
            queryParams = mapOf(NETWORK_ID_KEY to "base"),
            appRouter = appRouter,
        )

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = "base"),
                any(),
            )
        }
    }

    @Test
    fun `blank networkId is dropped`() {
        DefaultEarnDeepLinkHandler(
            queryParams = mapOf(NETWORK_ID_KEY to "   "),
            appRouter = appRouter,
        )

        verify {
            appRouter.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `earnType and networkId together`() {
        DefaultEarnDeepLinkHandler(
            queryParams = mapOf(EARN_TYPE_KEY to "yield", NETWORK_ID_KEY to "base"),
            appRouter = appRouter,
        )

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
}