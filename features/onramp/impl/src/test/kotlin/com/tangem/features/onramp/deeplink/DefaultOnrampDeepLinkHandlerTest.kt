package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
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

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val onrampSuccessScreenTrigger: OnrampSuccessScreenTrigger = mockk(relaxed = true)
    private val conciergeWaitlistTrigger: ConciergeWaitlistTrigger = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(appRouter, onrampSuccessScreenTrigger, conciergeWaitlistTrigger)
        every { appRouter.stack } returns emptyList()
    }

    @Test
    fun `GIVEN concierge waitlist type WHEN handled THEN waitlist triggered`() = runTest {
        // Act
        createHandler(queryParams = mapOf("type" to "concierge_waitlist"))
        runCurrent()

        // Assert
        coVerify(exactly = 1) { conciergeWaitlistTrigger.triggerOpenWaitlist() }
        coVerify(exactly = 0) { onrampSuccessScreenTrigger.triggerOnrampSuccess(any()) }
        verify(exactly = 0) { appRouter.replaceAll(*anyVararg(), onComplete = any()) }
    }

    @Test
    fun `GIVEN unknown type WHEN handled THEN nothing happens`() = runTest {
        // Act
        createHandler(queryParams = mapOf("type" to "concierge_unknown"))
        runCurrent()

        // Assert
        coVerify(exactly = 0) { conciergeWaitlistTrigger.triggerOpenWaitlist() }
        coVerify(exactly = 0) { onrampSuccessScreenTrigger.triggerOnrampSuccess(any()) }
        verify(exactly = 0) { appRouter.replaceAll(*anyVararg(), onComplete = any()) }
    }

    @Test
    fun `GIVEN tx id and waitlist type WHEN handled THEN success screen opened instead of waitlist`() = runTest {
        // Act
        createHandler(queryParams = mapOf("tx_id" to "tx", "type" to "concierge_waitlist"))
        runCurrent()

        // Assert
        verify(exactly = 1) { appRouter.replaceAll(AppRoute.OnrampSuccess("tx"), onComplete = any()) }
        coVerify(exactly = 0) { conciergeWaitlistTrigger.triggerOpenWaitlist() }
    }

    @Test
    fun `GIVEN result and waitlist type WHEN handled THEN result reported instead of waitlist`() = runTest {
        // Act
        createHandler(queryParams = mapOf("result" to "success", "type" to "concierge_waitlist"))
        runCurrent()

        // Assert
        coVerify(exactly = 1) { onrampSuccessScreenTrigger.triggerOnrampSuccess(true) }
        coVerify(exactly = 0) { conciergeWaitlistTrigger.triggerOpenWaitlist() }
    }

    private fun TestScope.createHandler(queryParams: Map<String, String>) =
        DefaultOnrampDeepLinkHandler(
            scope = backgroundScope,
            queryParams = queryParams,
            appRouter = appRouter,
            onrampSuccessScreenTrigger = onrampSuccessScreenTrigger,
            conciergeWaitlistTrigger = conciergeWaitlistTrigger,
        )
}