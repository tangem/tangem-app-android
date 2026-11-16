package com.tangem.features.onramp.deeplink

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.onramp.success.OnrampSuccessScreenTrigger
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultOnrampDeepLinkHandlerTest {

    private val router: Router = mockk(relaxed = true)
    private val onrampSuccessScreenTrigger: OnrampSuccessScreenTrigger = mockk(relaxed = true)
    private val conciergeWaitlistTrigger: ConciergeWaitlistTrigger = mockk(relaxed = true)
    private var stack: List<Route> = emptyList()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, onrampSuccessScreenTrigger, conciergeWaitlistTrigger)
        stack = listOf(AppRoute.Wallet, AppRoute.OnrampSuccess("previous"))
        every { router.navigate(any(), any()) } answers {
            stack = firstArg<(List<Route>) -> List<Route>>().invoke(stack)
        }
    }

    @Test
    fun `GIVEN concierge waitlist type WHEN handled THEN waitlist triggered`() = runTest {
        // Act
        handle(queryParams = mapOf("type" to "concierge_waitlist"))
        runCurrent()

        // Assert
        coVerify(exactly = 1) { conciergeWaitlistTrigger.triggerOpenWaitlist() }
        coVerify(exactly = 0) { onrampSuccessScreenTrigger.triggerOnrampSuccess(any()) }
        verify(exactly = 0) { router.navigate(any(), any()) }
    }

    @Test
    fun `GIVEN unknown type WHEN handled THEN nothing happens`() = runTest {
        // Act
        handle(queryParams = mapOf("type" to "concierge_unknown"))
        runCurrent()

        // Assert
        coVerify(exactly = 0) { conciergeWaitlistTrigger.triggerOpenWaitlist() }
        coVerify(exactly = 0) { onrampSuccessScreenTrigger.triggerOnrampSuccess(any()) }
        verify(exactly = 0) { router.navigate(any(), any()) }
    }

    @Test
    fun `GIVEN tx id and waitlist type WHEN handled THEN success screen replaces the onramp ones instead of waitlist`() = runTest {
        // Act
        handle(queryParams = mapOf("tx_id" to "tx", "type" to "concierge_waitlist"))
        runCurrent()

        // Assert
        assertThat(stack).containsExactly(AppRoute.Wallet, AppRoute.OnrampSuccess("tx")).inOrder()
        coVerify(exactly = 0) { conciergeWaitlistTrigger.triggerOpenWaitlist() }
    }

    @Test
    fun `GIVEN result and waitlist type WHEN handled THEN result reported instead of waitlist`() = runTest {
        // Act
        handle(queryParams = mapOf("result" to "success", "type" to "concierge_waitlist"))
        runCurrent()

        // Assert
        coVerify(exactly = 1) { onrampSuccessScreenTrigger.triggerOnrampSuccess(true) }
        coVerify(exactly = 0) { conciergeWaitlistTrigger.triggerOpenWaitlist() }
    }

    private val handler = DefaultOnrampDeepLinkHandler(
        onrampSuccessScreenTrigger = onrampSuccessScreenTrigger,
        conciergeWaitlistTrigger = conciergeWaitlistTrigger,
    )

    private fun TestScope.handle(queryParams: Map<String, String>) {
        handler.handle(
            request = DeepLinkRequest(
                uri = mockk<Uri>(),
                queryParams = queryParams,
                scope = backgroundScope,
                isFromOnNewIntent = false,
                router = router,
            ),
        )
    }
}