package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items

import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupHeaderId
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getTokenItemId
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

internal class CryptoCurrencyToDraggableItemConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<CryptoCurrencyStatus, DraggableItem.Token> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: CryptoCurrencyStatus): DraggableItem.Token {
        return createDraggableToken(value, appCurrencyProvider())
    }

    override fun convertList(input: Collection<CryptoCurrencyStatus>): List<DraggableItem.Token> {
        val appCurrency = appCurrencyProvider()

        return input.map { createDraggableToken(it, appCurrency) }
    }

    private fun createDraggableToken(
        currencyStatus: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
    ): DraggableItem.Token {
        return DraggableItem.Token(
            tokenItemState = createTokenItemState(currencyStatus, appCurrency),
            groupId = getGroupHeaderId(currencyStatus.currency.network),
        )
    }

    private fun createTokenItemState(
        currencyStatus: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
    ): TokenItemState.Draggable {
        val currency = currencyStatus.currency

        return TokenItemState.Draggable(
            id = getTokenItemId(currency.id),
            iconState = iconStateConverter.convert(currencyStatus),
            titleState = TokenItemState.TitleState.Content(text = stringReference(currency.name)),
            subtitle2State = if (currencyStatus.value.isError) {
                TokenItemState.Subtitle2State.Unreachable
            } else {
                TokenItemState.Subtitle2State.TextContent(text = getFormattedFiatAmount(currencyStatus, appCurrency))
            },
        )
    }

    private fun getFormattedFiatAmount(currency: CryptoCurrencyStatus, appCurrency: AppCurrency): String {
        val yieldBalance = currency.value.yieldBalance as? YieldBalance.Data
        val fiatRate = currency.value.fiatRate ?: BigDecimal.ZERO
        val fiatYieldBalance = if (BlockchainUtils.isIncludeStakingTotalBalance(currency.currency.network.id.value)) {
            yieldBalance?.getTotalWithRewardsStakingBalance()?.multiply(fiatRate).orZero()
        } else {
            BigDecimal.ZERO
        }

        val fiatAmount = currency.value.fiatAmount ?: return BigDecimalFormatConstants.EMPTY_BALANCE_SIGN
        return (fiatAmount + fiatYieldBalance).format { fiat(appCurrency.code, appCurrency.symbol) }
    }
}