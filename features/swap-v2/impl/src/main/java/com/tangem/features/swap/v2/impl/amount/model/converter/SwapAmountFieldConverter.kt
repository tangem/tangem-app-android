package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.converters.AmountAccountConverter
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.converters.MaxEnterAmountConverter
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.utils.StringsSigns.DOT

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

    fun convert(selectedType: SwapAmountType, cryptoCurrencyStatus: CryptoCurrencyStatus): SwapAmountFieldUM {
        val walletTitle = if (isSingleWallet) {
            resourceReference(R.string.send_from_title)
        } else {
            resourceReference(R.string.send_from_wallet_name, wrappedList(userWallet.name))
        }
        return SwapAmountFieldUM.Content(
            amountType = selectedType,
            title = stringReference(cryptoCurrencyStatus.currency.name),
            subtitleLeft = getSubtitleLeft(selectedType = selectedType, cryptoCurrencyStatus = cryptoCurrencyStatus),
            subtitleEllipsisLeft = getSubtitleEllipsisLeft(
                selectedType = selectedType,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ),
            subtitleRight = getSubtitleRight(selectedType = selectedType, cryptoCurrencyStatus = cryptoCurrencyStatus),
            subtitleEllipsisRight = TextEllipsis.OffsetEnd(appCurrency.symbol.length),
            priceImpact = null,
            isClickEnabled = selectedType.isViewingField(),
            amountField = AmountStateConverter(
                clickIntents = clickIntents,
                appCurrency = appCurrency,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                maxEnterAmount = maxEnterAmountConverter.convert(cryptoCurrencyStatus),
                iconStateConverter = iconStateConverter,
                isBalanceHidden = isBalanceHidden,
                accountTitleUM = AmountAccountConverter(
                    isAccountsMode = isAccountsMode,
                    walletTitle = walletTitle,
                    prefixText = when {
                        selectedType.isEnteringField() -> resourceReference(R.string.common_from)
                        selectedType.isViewingField() -> resourceReference(R.string.common_to)
                        else -> TextReference.Companion.EMPTY
                    },
                ).convert(account),
            ).convert(
                AmountParameters(
                    title = walletTitle,
                    value = "",
                ),
            ),
        )
    }

    private fun getSubtitleLeft(selectedType: SwapAmountType, cryptoCurrencyStatus: CryptoCurrencyStatus) = when {
        selectedType.isEnteringField() -> combinedReference(
            stringReference(
                cryptoCurrencyStatus.value.amount.format {
                    crypto(cryptoCurrency = cryptoCurrencyStatus.currency)
                },
            ),
        ).orMaskWithStars(isBalanceHidden)
        selectedType.isViewingField() -> resourceReference(R.string.send_with_swap_recipient_get_amount)
        else -> TextReference.Companion.EMPTY
    }

    private fun getSubtitleRight(selectedType: SwapAmountType, cryptoCurrencyStatus: CryptoCurrencyStatus) = when {
        selectedType.isEnteringField() -> if (isBalanceHidden) {
            TextReference.EMPTY
        } else {
            combinedReference(
                stringReference(value = " $DOT "),
                stringReference(
                    cryptoCurrencyStatus.value.fiatAmount.format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                ),
            )
        }
        else -> TextReference.EMPTY
    }

    private fun getSubtitleEllipsisLeft(selectedType: SwapAmountType, cryptoCurrencyStatus: CryptoCurrencyStatus) =
        when {
            selectedType.isEnteringField() -> TextEllipsis.OffsetEnd(cryptoCurrencyStatus.currency.symbol.length)
            selectedType.isViewingField() -> TextEllipsis.End
            else -> TextEllipsis.End
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