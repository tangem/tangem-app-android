package com.tangem.features.earn.impl

import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HostEarnNavigatorTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletId = UserWalletId("0123456789abcdef")
    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    @BeforeEach
    fun resetMocks() {
        clearMocks(router)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN route WHEN navigate THEN router pushes the mapped route`(model: NavigateModel) {
        // Act
        navigator(model.source).navigate(model.route)

        // Assert
        verify(exactly = 1) { router.push(model.expected, any()) }
    }

    @Test
    fun `GIVEN flow route WHEN navigate THEN router is not touched`() {
        // Arrange
        val route = EarnFlowRoute(productId = LENDING, action = Deposit())

        // Act
        navigator(AppRoute.EarnEntry.Source.YieldSheet).navigate(route)

        // Assert
        verify(exactly = 0) { router.push(any(), any()) }
    }

    private fun provideTestModels() = listOf(
        NavigateModel(
            source = AppRoute.EarnEntry.Source.YieldSheet,
            route = EarnInfoRoute(productId = LENDING),
            expected = entry(AppRoute.EarnEntry.Source.YieldSheet, AppRoute.EarnEntry.Start.Info(productId = LENDING)),
        ),
        NavigateModel(
            source = AppRoute.EarnEntry.Source.EarnBanner,
            route = EarnInfoRoute(productId = LENDING),
            expected = entry(AppRoute.EarnEntry.Source.EarnBanner, AppRoute.EarnEntry.Start.Info(productId = LENDING)),
        ),
        NavigateModel(
            source = AppRoute.EarnEntry.Source.YieldSheet,
            route = EarnPickerRoute(preselected = null, shouldRedirectToActive = true),
            expected = entry(AppRoute.EarnEntry.Source.YieldSheet, AppRoute.EarnEntry.Start.Picker(preselected = null)),
        ),
        NavigateModel(
            source = AppRoute.EarnEntry.Source.YieldSheet,
            route = EarnPickerRoute(preselected = LENDING, shouldRedirectToActive = false),
            expected = entry(
                source = AppRoute.EarnEntry.Source.YieldSheet,
                start = AppRoute.EarnEntry.Start.Picker(preselected = LENDING),
            ),
        ),
        NavigateModel(source = AppRoute.EarnEntry.Source.EarnBanner, route = OtherRoute, expected = OtherRoute),
    )

    private fun navigator(source: AppRoute.EarnEntry.Source) = HostEarnNavigator(
        router = router,
        userWalletId = userWalletId,
        cryptoCurrency = currency,
        source = source,
    )

    private fun entry(source: AppRoute.EarnEntry.Source, start: AppRoute.EarnEntry.Start) = AppRoute.EarnEntry(
        userWalletId = userWalletId,
        cryptoCurrency = currency,
        source = source,
        start = start,
    )

    internal data class NavigateModel(
        val source: AppRoute.EarnEntry.Source,
        val route: Route,
        val expected: Route,
    ) {
        override fun toString(): String = "$source: $route"
    }

    private object OtherRoute : Route

    private companion object {
        val LENDING = EarnProductId("lending")
    }
}