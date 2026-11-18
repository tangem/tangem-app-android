package com.tangem.features.commonfeatures.impl.tokenactions.model

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.QuickActionUM
import com.tangem.common.ui.markets.action.TokenActionsContext
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.settings.usercountry.models.UserCountryError
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.features.commonfeatures.api.tokenactions.BottomAction
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.tokenactions.TokenActionsComponent
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenActionsUiBuilderTest {

    private val currency = MockCryptoCurrencyFactory().bitcoin
    private val buy = QuickActionUM.V2.Buy
    private val swap = QuickActionUM.V2.Exchange(shouldShowBadge = false)
    private val receive = QuickActionUM.V2.Receive

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN region and asset availability WHEN building Add funds THEN the correct rows and banner are shown`(
        case: Case,
    ) {
        // Arrange
        val data = CryptoCurrencyData(
            userWallet = mockk(),
            status = CryptoCurrencyStatus(currency, CryptoCurrencyStatus.Loading),
            actions = listOf(
                ActionState.Buy(ScenarioUnavailabilityReason.None),
                ActionState.Swap(case.swapReason, shouldShowBadge = false),
                ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
            isAccountMode = false,
            account = mockk(),
        )
        val params = TokenActionsComponent.Params(
            data = emptyFlow(),
            callbacks = mockk(),
            context = TokenActionsContext.AddFunds,
            isRegionRestricted = case.assetRestricted,
        )
        val builder = TokenActionsUiBuilder(
            paramsContainer = MutableParamsContainer(params),
            getWalletIconUseCase = mockk(),
            getWalletsUseCase = mockk { every { invokeSync() } returns emptyList() },
            getUserCountryUseCase = mockk { every { invokeSync() } returns UserCountryError.NotSetup.left() },
            walletIconUMConverter = mockk(),
        )

        val handler = mockk<TokenActionsHandler>(relaxUnitFun = true)
        var canBuy = case.region == OnrampRegionState.Allowed

        // Act
        val result = builder.build(
            input = TokenActionsBuildInput(
                cryptoCurrencyData = data,
                isBalanceHidden = false,
                bottomAction = BottomAction.None,
                onrampRegion = case.region,
            ),
            tokenActionsHandler = handler,
            appCurrency = AppCurrency.Default,
            canBuy = { canBuy },
        )

        result.quickActions.onQuickActionClick(buy)
        canBuy = false
        result.quickActions.onQuickActionClick(buy)

        // Assert
        val expectedBuyClicks = if (buy in case.disabled) 0 else 1
        verify(exactly = expectedBuyClicks) { handler.handle(any(), data, TokenActionsContext.AddFunds) }
        assertThat(result.quickActions.actions).containsExactly(buy, swap, receive).inOrder()
        assertThat(result.quickActions.disabledActions).containsExactlyElementsIn(case.disabled)
        assertThat(result.regionRestriction?.title).isEqualTo(case.title?.let { resourceReference(it) })
    }

    private fun provideTestModels() = listOf(
        Case(OnrampRegionState.Checking, setOf(buy)),
        Case(OnrampRegionState.Unknown, setOf(buy)),
        Case(OnrampRegionState.Allowed, emptySet()),
        Case(OnrampRegionState.Restricted("France"), setOf(buy), R.string.onramp_region_restriction_title),
        Case(
            OnrampRegionState.Restricted("France"),
            setOf(buy, swap),
            R.string.onramp_region_restriction_title,
            swapReason = ScenarioUnavailabilityReason.SingleWallet,
        ),
        Case(
            OnrampRegionState.Restricted("France"),
            setOf(buy, swap),
            R.string.buy_swap_region_restriction_title,
            assetRestricted = true,
        ),
    )

    internal data class Case(
        val region: OnrampRegionState,
        val disabled: Set<QuickActionUM>,
        val title: Int? = null,
        val assetRestricted: Boolean = false,
        val swapReason: ScenarioUnavailabilityReason = ScenarioUnavailabilityReason.None,
    )
}