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
    private val navigator = HostEarnNavigator(router = router, userWalletId = userWalletId, cryptoCurrency = currency)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN route WHEN navigate THEN router pushes the mapped route`(model: NavigateModel) {
        // Act
        navigator.navigate(model.route)

        // Assert
        verify(exactly = 1) { router.push(model.expected, any()) }
    }

    @Test
    fun `GIVEN flow route WHEN navigate THEN router is not touched`() {
        // Arrange
        val route = EarnFlowRoute(productId = LENDING, action = Deposit())

        // Act
        navigator.navigate(route)

        // Assert
        verify(exactly = 0) { router.push(any(), any()) }
    }

    private fun provideTestModels() = listOf(
        NavigateModel(
            route = EarnInfoRoute(productId = LENDING),
            expected = yieldSheetEntry(AppRoute.EarnEntry.Start.Info(productId = LENDING)),
        ),
        NavigateModel(
            route = EarnPickerRoute(preselected = null, shouldRedirectToActive = true),
            expected = yieldSheetEntry(AppRoute.EarnEntry.Start.Picker(preselected = null)),
        ),
        NavigateModel(
            route = EarnPickerRoute(preselected = LENDING, shouldRedirectToActive = false),
            expected = yieldSheetEntry(AppRoute.EarnEntry.Start.Picker(preselected = LENDING)),
        ),
        NavigateModel(route = OtherRoute, expected = OtherRoute),
    )

    private fun yieldSheetEntry(start: AppRoute.EarnEntry.Start) = AppRoute.EarnEntry(
        userWalletId = userWalletId,
        cryptoCurrency = currency,
        source = AppRoute.EarnEntry.Source.YieldSheet,
        start = start,
    )

    internal data class NavigateModel(val route: Route, val expected: Route) {
        override fun toString(): String = route.toString()
    }

    private object OtherRoute : Route

    private companion object {
        val LENDING = EarnProductId("lending")
    }
}