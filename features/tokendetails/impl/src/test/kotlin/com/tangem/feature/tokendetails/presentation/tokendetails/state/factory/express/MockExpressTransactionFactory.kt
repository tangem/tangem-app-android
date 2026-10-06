package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateIconUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateInfoUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object MockExpressTransactionFactory {

    private val cryptoCurrency: CryptoCurrency = mockk()

    fun createExchange(
        txId: String = "exchange",
        timestamp: Long = 0L,
        providerType: ExchangeProviderType = ExchangeProviderType.CEX,
        activeStatus: ExchangeStatus? = ExchangeStatus.Exchanging,
        fromUserWalletId: UserWalletId = MockAccounts.userWalletId,
    ): ExchangeUM = ExchangeUM(
        info = createInfo(txId = txId, timestamp = timestamp),
        provider = createProvider(type = providerType),
        activeStatus = activeStatus,
        statuses = persistentListOf(),
        notification = null,
        showProviderLink = false,
        fromUserWalletId = fromUserWalletId,
        fromCryptoCurrency = cryptoCurrency,
        toCryptoCurrency = cryptoCurrency,
        hasLongTime = false,
    )

    fun createOnramp(
        txId: String = "onramp",
        timestamp: Long = 0L,
        activeStatus: OnrampStatus.Status = OnrampStatus.Status.Sending,
    ): ExpressTransactionStateUM.OnrampUM = ExpressTransactionStateUM.OnrampUM(
        info = createInfo(txId = txId, timestamp = timestamp),
        providerName = "Mercuryo",
        providerImageUrl = "",
        providerType = "onramp",
        activeStatus = activeStatus,
        fromCurrencyCode = "USD",
    )

    fun createProvider(type: ExchangeProviderType = ExchangeProviderType.CEX): SwapProvider = SwapProvider(
        providerId = "changenow",
        name = "ChangeNOW",
        type = type,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    fun createInfo(txId: String = "tx", timestamp: Long = 0L): ExpressTransactionStateInfoUM =
        ExpressTransactionStateInfoUM(
            title = TextReference.EMPTY,
            status = ExpressStatusUM(
                title = TextReference.EMPTY,
                link = ExpressLinkUM.Empty,
                statuses = persistentListOf(),
            ),
            notification = null,
            txId = txId,
            txExternalId = null,
            txExternalUrl = null,
            timestamp = timestamp,
            timestampFormatted = TextReference.EMPTY,
            timestampAgoFormatted = TextReference.EMPTY,
            activeStatus = TextReference.EMPTY,
            onGoToProviderClick = {},
            onClick = {},
            onDisposeExpressStatus = {},
            iconState = ExpressTransactionStateIconUM.None,
            toAmount = TextReference.EMPTY,
            toAmountValue = BigDecimal.ZERO,
            toFiatAmount = null,
            toAmountDecimals = 0,
            toAmountSymbol = "",
            toCurrencyIcon = CurrencyIconState.Loading,
            toAddress = "",
            fromAmount = TextReference.EMPTY,
            fromAmountValue = BigDecimal.ZERO,
            fromAmountDecimals = 0,
            fromFiatAmount = null,
            fromAmountSymbol = "",
            fromCurrencyIcon = CurrencyIconState.Loading,
            fromAddress = null,
        )
}