package com.tangem.features.earn.impl.flow

import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.EarnEntryStart
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnFlowExitHandlerTest {

    private val innerRouter: Router = mockk(relaxed = true)
    private val hostRouter: Router = mockk(relaxed = true)
    private val userWalletId = UserWalletId("0123456789abcdef")
    private val currency = MockCryptoCurrencyFactory().ethereum
    private val productId = EarnProductId("lending")

    @BeforeEach
    fun resetMocks() {
        clearMocks(innerRouter, hostRouter)
    }

    @Test
    fun `GIVEN Done exit WHEN onExit THEN the flow is popped from the Earn stack only`() {
        // Arrange
        val handler = createHandler(EarnEntrySource.TokenPage)

        // Act
        handler.onExit(productId, EarnFlowExit.Done)

        // Assert
        verify(exactly = 1) { innerRouter.pop(any()) }
        verify(exactly = 0) { hostRouter.pop(any()) }
        verify(exactly = 0) { hostRouter.replaceCurrent(any(), any()) }
    }

    @Test
    fun `GIVEN ToYield exit WHEN onExit THEN the Earn stack is replaced with Info of the product`() {
        // Arrange
        val handler = createHandler(EarnEntrySource.TokenPage)

        // Act
        handler.onExit(productId, EarnFlowExit.ToYield)

        // Assert
        verify(exactly = 1) { innerRouter.replaceAll(EarnInfoRoute(productId), onComplete = any()) }
        verify(exactly = 0) { hostRouter.pop(any()) }
        verify(exactly = 0) { hostRouter.replaceCurrent(any(), any()) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToTokenWithTokenBelow {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN entry from a token screen WHEN ToToken exit THEN Earn is popped to the token`(
            source: EarnEntrySource,
        ) {
            // Arrange
            val handler = createHandler(source)

            // Act
            handler.onExit(productId, EarnFlowExit.ToToken)

            // Assert
            verify(exactly = 1) { hostRouter.pop(any()) }
            verify(exactly = 0) { hostRouter.replaceCurrent(any(), any()) }
            verify(exactly = 0) { innerRouter.pop(any()) }
        }

        private fun provideTestModels() = listOf(
            EarnEntrySource.TokenPage,
            EarnEntrySource.EarnBanner,
            EarnEntrySource.YieldSheet,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToTokenWithoutTokenBelow {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN entry from another screen WHEN ToToken exit THEN Earn is replaced with the token screen`(
            source: EarnEntrySource,
        ) {
            // Arrange
            val handler = createHandler(source)
            val expected = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = currency)

            // Act
            handler.onExit(productId, EarnFlowExit.ToToken)

            // Assert
            verify(exactly = 1) { hostRouter.replaceCurrent(expected, any()) }
            verify(exactly = 0) { hostRouter.pop(any()) }
            verify(exactly = 0) { innerRouter.pop(any()) }
        }

        private fun provideTestModels() = listOf(
            EarnEntrySource.Markets,
            EarnEntrySource.ForYou,
            EarnEntrySource.Deeplink,
        )
    }

    private fun createHandler(source: EarnEntrySource) = EarnFlowExitHandler(
        innerRouter = innerRouter,
        hostRouter = hostRouter,
        entry = EarnEntryComponent.Params(
            userWalletId = userWalletId,
            cryptoCurrency = currency,
            source = source,
            start = EarnEntryStart.Picker(preselected = null),
        ),
    )
}