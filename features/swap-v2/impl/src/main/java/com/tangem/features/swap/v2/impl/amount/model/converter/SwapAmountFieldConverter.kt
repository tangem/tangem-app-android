package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.converters.AmountAccountConverter
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.converters.MaxEnterAmountConverter
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.*
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SwapAmountFieldConverter(
    private val swapDirection: SwapDirection,
    private val isBalanceHidden: Boolean,
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val clickIntents: AmountScreenClickIntents,
    private val isSingleWallet: Boolean,
    private val isAccountsMode: Boolean,
    private val account: Account?,
) {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val maxEnterAmountConverter = MaxEnterAmountConverter()

    fun convert(
        swapAmountType: SwapAmountType,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isSelected: Boolean,
        isAmountEmpty: Boolean = true,
        displayAmount: BigDecimal? = null,
    ): SwapAmountFieldUM {
        val walletTitle = if (isSingleWallet) {
            resourceReference(R.string.send_from_title)
        } else {
            resourceReference(R.string.send_from_wallet_name, wrappedList(userWallet.name))
        }
        val subtitles = computeSubtitles(
            swapAmountType = swapAmountType,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            isEntering = isSelected,
            isAmountEmpty = isAmountEmpty,
            displayAmount = displayAmount,
        )
        return SwapAmountFieldUM.Content(
            amountType = swapAmountType,
            title = stringReference(cryptoCurrencyStatus.currency.name),
            subtitleLeft = subtitles.subtitleLeft,
            subtitleEllipsisLeft = subtitles.subtitleEllipsisLeft,
            subtitleRight = subtitles.subtitleRight,
            subtitleEllipsisRight = subtitles.subtitleEllipsisRight,
            isClickEnabled = true,
            amountField = AmountStateConverter(
                clickIntents = clickIntents,
                appCurrency = appCurrency,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                maxEnterAmount = maxEnterAmountConverter.convert(cryptoCurrencyStatus),
                iconStateConverter = iconStateConverter,
                isBalanceHidden = isBalanceHidden,
                accountTitleUM = if (swapAmountType.isViewingField()) {
                    AccountTitleUM.Text(
                        resourceReference(R.string.send_with_swap_recipient_get_amount, wrappedList("")),
                    )
                } else {
                    AmountAccountConverter(
                        isAccountsMode = isAccountsMode,
                        walletTitle = walletTitle,
                        prefixText = when {
                            swapAmountType.isEnteringField() -> resourceReference(R.string.common_from)
                            else -> TextReference.Companion.EMPTY
                        },
                    ).convert(account)
                },
            ).convert(
                AmountParameters(
                    title = walletTitle,
                    value = "",
                ),
            ),
        )
    }

    private fun computeSubtitles(
        swapAmountType: SwapAmountType,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isEntering: Boolean,
        isAmountEmpty: Boolean,
        displayAmount: BigDecimal?,
    ): SwapSubtitleResult = when (swapAmountType) {
        SwapAmountType.From -> SwapFromSubtitleConverter.convert(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            isBalanceHidden = isBalanceHidden,
            isEntering = isEntering,
            isAmountEmpty = isAmountEmpty,
            displayAmount = displayAmount,
        )
        SwapAmountType.To -> SwapToSubtitleConverter.convert(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            isBalanceHidden = isBalanceHidden,
            isEntering = isEntering,
            isAmountEmpty = isAmountEmpty,
            displayAmount = displayAmount,
        )
    }

    private fun SwapAmountType.isEnteringField(): Boolean {
        return this == SwapAmountType.From && swapDirection == SwapDirection.Direct ||
            this == SwapAmountType.To && swapDirection == SwapDirection.Reverse
    }

    private fun SwapAmountType.isViewingField(): Boolean {
        return this == SwapAmountType.From && swapDirection == SwapDirection.Reverse ||
            this == SwapAmountType.To && swapDirection == SwapDirection.Direct
    }
}