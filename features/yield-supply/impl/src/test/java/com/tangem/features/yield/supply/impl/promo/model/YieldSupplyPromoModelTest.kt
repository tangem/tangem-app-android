package com.tangem.features.yield.supply.impl.promo.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.promo.usecase.GetBoostedApyUseCase
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import com.tangem.features.yield.supply.impl.YieldBoostStoryPreloader
import com.tangem.features.yield.supply.impl.promo.YieldSupplyPromoConfig
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class YieldSupplyPromoModelTest {

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val boostStoryPreloader: YieldBoostStoryPreloader = mockk(relaxed = true)

    private val personal: CryptoCurrency = coin(derivationPath = Network.DerivationPath.None)

    @Test
    fun `GIVEN a personal token WHEN onStartEarningClick THEN the action sheet opens`() {
        // Arrange
        val model = createModel(currency = personal)
        val opened = model.openedBottomSheets()

        // Act
        model.onStartEarningClick()

        // Assert
        assertThat(opened).containsExactly(YieldSupplyPromoConfig.Action)
    }

    private fun YieldSupplyPromoModel.openedBottomSheets(): List<YieldSupplyPromoConfig?> {
        val opened = mutableListOf<YieldSupplyPromoConfig?>()
        bottomSheetNavigation.subscribe { event -> opened += event.transformer(null) }

        return opened
    }

    private fun createModel(currency: CryptoCurrency): YieldSupplyPromoModel = YieldSupplyPromoModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            YieldSupplyPromoComponent.Params(
                userWalletId = UserWalletId("011"),
                currency = currency,
                apy = "5",
            ),
        ),
        analytics = analytics,
        urlOpener = urlOpener,
        appRouter = appRouter,
        getBoostedApyUseCase = GetBoostedApyUseCase(),
        boostStoryPreloader = boostStoryPreloader,
    )

    private fun coin(derivationPath: Network.DerivationPath): CryptoCurrency.Coin {
        val network = Network(
            id = Network.ID(value = "ethereum", derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
                suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
            ),
            network = network,
            name = "Ethereum",
            symbol = "ETH",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
        )
    }
}