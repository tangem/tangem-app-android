package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.*
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.network.Network
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeStatusState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

class ExpressStatusBottomSheetStateProvider : PreviewParameterProvider<ExpressStatusBottomSheetConfig> {
    override val values: Sequence<ExpressStatusBottomSheetConfig>
        get() = sequenceOf(
            ExpressStatusBottomSheetConfig(
                ExchangeUM(
                    info = ExpressTransactionStateInfoUM(
                        title = TextReference.Str("Transaction Status"),
                        status = ExpressStatusUM(
                            title = TextReference.Str("Status Details"),
                            link = ExpressLinkUM.Empty,
                            statuses = persistentListOf(
                                ExpressStatusItemUM(TextReference.Str("Created"), ExpressStatusItemState.Active),
                                ExpressStatusItemUM(TextReference.Str("Exchanging"), ExpressStatusItemState.Active),
                                ExpressStatusItemUM(TextReference.Str("Done"), ExpressStatusItemState.Active),
                            ),
                        ),
                        notification = null,
                        txId = "123456",
                        txExternalId = "78910",
                        txExternalUrl = "https://example.com/tx/78910",
                        timestamp = System.currentTimeMillis(),
                        timestampFormatted = TextReference.Str("Just now"),
                        onGoToProviderClick = {},
                        onClick = {},
                        onDisposeExpressStatus = {},
                        iconState = ExpressTransactionStateIconUM.None,
                        toAmount = TextReference.Str("0.1 BTC"),
                        toFiatAmount = TextReference.Str("$5000"),
                        toAmountSymbol = "BTC",
                        toCurrencyIcon = CurrencyIconState.Empty(),
                        fromAmount = TextReference.Str("5000 USDT"),
                        fromFiatAmount = TextReference.Str("$5000"),
                        fromAmountSymbol = "USDT",
                        fromCurrencyIcon = CurrencyIconState.Empty(),
                    ),
                    provider = SwapProvider(
                        providerId = "1",
                        rateTypes = emptyList(),
                        name = "Provider",
                        type = ExchangeProviderType.DEX,
                        imageLarge = "",
                        termsOfUse = null,
                        privacyPolicy = null,
                        slippage = BigDecimal.ZERO,
                    ),
                    activeStatus = null,
                    statuses = persistentListOf(
                        ExchangeStatusState(
                            status = ExchangeStatus.Exchanging,
                            text = stringReference("Exchanging"),
                            isActive = true,
                            isDone = false,
                        ),
                    ),
                    notification = ExchangeStatusNotification.LongTimeExchange {},
                    showProviderLink = false,
                    fromCryptoCurrency = token,
                    toCryptoCurrency = token,
                    hasLongTime = true,
                ),
            ),
        )

    private val token
        get() = CryptoCurrency.Coin(
            id = ID(
                ID.Prefix.COIN_PREFIX,
                ID.Body.NetworkId(network.rawId),
                ID.Suffix.RawID("token1"),
            ),
            network = network,
            name = "Token 1",
            symbol = "T1",
            decimals = 8,
            iconUrl = null,
            isCustom = false,
        )

    private val network = Network(
        id = Network.ID(value = "network1", derivationPath = Network.DerivationPath.None),
        name = "Network One",
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        backendId = "network1",
        currencySymbol = "ETH",
        derivationPath = Network.DerivationPath.None,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )
}