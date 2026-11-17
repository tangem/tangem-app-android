package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.EARN_TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
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

    private val router: Router = mockk()

    private val handler = EarnDeepLinkHandler()

    @BeforeEach
    fun setUp() {
        every { router.push(any(), any()) } just Runs
    }

    @Test
    fun `no params opens earn list with no filters`() {
        handler.handle(createRequest(queryParams = emptyMap()))

        verify {
            router.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `staking earnType is parsed`() {
        handler.handle(createRequest(queryParams = mapOf(EARN_TYPE_KEY to "staking")))

        verify {
            router.push(
                AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Staking, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `yield earnType is parsed`() {
        handler.handle(createRequest(queryParams = mapOf(EARN_TYPE_KEY to "yield")))

        verify {
            router.push(
                AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Yield, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `invalid earnType is silently dropped`() {
        handler.handle(createRequest(queryParams = mapOf(EARN_TYPE_KEY to "bogus")))

        verify {
            router.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `networkId is passed through`() {
        handler.handle(createRequest(queryParams = mapOf(NETWORK_ID_KEY to "base")))

        verify {
            router.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = "base"),
                any(),
            )
        }
    }

    @Test
    fun `blank networkId is dropped`() {
        handler.handle(createRequest(queryParams = mapOf(NETWORK_ID_KEY to "   ")))

        verify {
            router.push(
                AppRoute.Earn(preselectedEarnType = null, preselectedNetworkId = null),
                any(),
            )
        }
    }

    @Test
    fun `earnType and networkId together`() {
        handler.handle(createRequest(queryParams = mapOf(EARN_TYPE_KEY to "yield", NETWORK_ID_KEY to "base")))

        verify {
            router.push(
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
        router = router,
    )
}