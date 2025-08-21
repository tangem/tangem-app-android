package com.tangem.features.swap.v2.impl.amount.ui.preview

import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal data object SwapAmountContentPreview {

    val cryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = CryptoCurrency.Coin(
            id = CryptoCurrency.ID.fromValue("coin⟨BITCOIN⟩bitcoin"),
            network = Network(
                id = Network.ID(
                    value = "bitcoin",
                    derivationPath = Network.DerivationPath.None,
                ),
                backendId = "bitcoin",
                name = "Bitcoin",
                currencySymbol = "BTC",
                derivationPath = Network.DerivationPath.None,
                isTestnet = false,
                standardType = Network.StandardType.Unspecified("bitcoin"),
                hasFiatFeeRate = false,
                canHandleTokens = false,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
                nameResolvingType = Network.NameResolvingType.NONE,

            ),
            name = "Bitcoin",
            symbol = "BTC",
            decimals = 8,
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/medium/bitcoin.png",
            isCustom = false,
        ),
        value = CryptoCurrencyStatus.Loading,
    )

    private val provider = ExpressProvider(
        providerId = "changenow",
        rateTypes = listOf(ExpressRateType.Float),
        name = "ChangeNow",
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = "",
        privacyPolicy = "",
        isRecommended = true,
        slippage = BigDecimal.ZERO,
    )

    private val quote = SwapQuoteUM.Content(
        provider = provider,
        quoteAmount = "123".toBigDecimal(),
        quoteAmountValue = stringReference("123"),
        rate = stringReference("1 USD ≈ 123.123 POL"),
        diffPercent = SwapQuoteUM.Content.DifferencePercent.Best,
        isSingleProvider = false,
    )

    val emptyState = SwapAmountUM.Content(
        isPrimaryButtonEnabled = false,
        primaryAmount = SwapAmountFieldUM.Empty(
            amountType = SwapAmountType.From,
        ),
        secondaryAmount = SwapAmountFieldUM.Empty(
            amountType = SwapAmountType.To,
        ),
        swapDirection = SwapDirection.Direct,
        selectedAmountType = SwapAmountType.From,
        swapCurrencies = SwapCurrencies.EMPTY,
        swapQuotes = persistentListOf(),
        selectedQuote = SwapQuoteUM.Empty,
        primaryCryptoCurrencyStatus = cryptoCurrencyStatus,
        secondaryCryptoCurrencyStatus = cryptoCurrencyStatus,
        swapRateType = ExpressRateType.Float,
        appCurrency = AppCurrency.Default,
        showBestRateAnimation = false,
        showFCAWarning = false,
    )

    val defaultState = SwapAmountUM.Content(
        primaryAmount = SwapAmountFieldUM.Content(
            amountType = SwapAmountType.From,
            amountField = AmountStatePreviewData.amountState.copy(
                availableBalance = stringReference("Balance: 100 BTC"),
            ),
            title = stringReference("Tether"),
            subtitle = stringReference("Balance: 100 BTC"),
            priceImpact = null,
            isClickEnabled = false,
            subtitleEllipsis = TextEllipsis.OffsetEnd(3),
        ),
        secondaryAmount = SwapAmountFieldUM.Content(
            amountType = SwapAmountType.To,
            amountField = AmountStatePreviewData.amountState.copy(
                title = stringReference("Amount to receive"),
                availableBalance = TextReference.EMPTY,
            ),
            title = stringReference("Shiba Inu"),
            priceImpact = stringReference("(-10%)"),
            subtitle = TextReference.EMPTY,
            isClickEnabled = false,
            subtitleEllipsis = TextEllipsis.OffsetEnd(3),
        ),
        appCurrency = AppCurrency.Default,
        swapDirection = SwapDirection.Direct,
        selectedAmountType = SwapAmountType.From,
        swapCurrencies = SwapCurrencies.EMPTY,
        swapQuotes = persistentListOf(),
        selectedQuote = quote,
        primaryCryptoCurrencyStatus = cryptoCurrencyStatus,
        secondaryCryptoCurrencyStatus = cryptoCurrencyStatus,
        swapRateType = ExpressRateType.Float,
        isPrimaryButtonEnabled = true,
        showBestRateAnimation = false,
        showFCAWarning = true,
    )
}