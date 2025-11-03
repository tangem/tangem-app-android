package com.tangem.common.ui.amountScreen.converters.field

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.transformer.Transformer

/**
 * Updates amount boundaries and revalidates current amount state
 *
 * @property cryptoCurrencyStatus current cryptocurrency status
 * @property maxEnterAmount new maximum enter amount boundary
 * @property appCurrency current app currency
 * @property isBalanceHidden whether balance is hidden
 */
class AmountBoundaryUpdateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val maxEnterAmount: EnterAmountBoundary,
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
) : Transformer<AmountState> {

    override fun transform(prevState: AmountState): AmountState {
        if (prevState !is AmountState.Data) return prevState

        val fiat = maxEnterAmount.fiatAmount.format { fiat(appCurrency.code, appCurrency.symbol) }
        val crypto = maxEnterAmount.amount.format { crypto(cryptoCurrencyStatus.currency) }

        return prevState.copy(
            availableBalanceCrypto = stringReference(crypto).orMaskWithStars(isBalanceHidden),
            availableBalanceFiat = if (isBalanceHidden) {
                TextReference.EMPTY
            } else {
                combinedReference(
                    stringReference(" $DOT "),
                    stringReference(fiat),
                )
            },
        )
    }
}