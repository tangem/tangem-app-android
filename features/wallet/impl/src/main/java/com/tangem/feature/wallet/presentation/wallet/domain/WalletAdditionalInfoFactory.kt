package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.common.CardTypesResolver
import com.tangem.feature.wallet.impl.R
import java.math.BigDecimal

/**
 * Wallet additional info factory
 *
[REDACTED_AUTHOR]
 */
internal object WalletAdditionalInfoFactory {

    private val DIVIDER_RES by lazy { TextReference.Str(value = " • ") }

    /**
     * Get additional info
     *
     * @param cardTypesResolver  card type resolver
     * @param isLocked          check if wallet is locked
     * @param currencyAmount    amount of currency
     */
    fun resolve(
        cardTypesResolver: CardTypesResolver,
        isLocked: Boolean,
        currencyAmount: BigDecimal? = null,
    ): TextReference {
        return if (cardTypesResolver.isMultiwalletAllowed()) {
            resolveMultiCurrencyInfo(cardTypesResolver, isLocked)
        } else {
            resolveSingleCurrencyInfo(cardTypesResolver, isLocked, currencyAmount)
        }
    }

    private fun resolveMultiCurrencyInfo(cardTypeResolver: CardTypesResolver, isLocked: Boolean): TextReference {
        val backupCardsCount = cardTypeResolver.getBackupCardsCount()
        val backupInfoRes = TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = backupCardsCount,
            formatArgs = wrappedList(backupCardsCount),
        )

        return when {
            cardTypeResolver.isWallet2() && !isLocked -> {
                backupInfoRes + DIVIDER_RES + TextReference.Res(id = R.string.common_seed_phrase)
            }
            cardTypeResolver.isTangemWallet() && !isLocked -> {
                backupInfoRes
            }
            isLocked -> {
                backupInfoRes + TextReference.Res(R.string.common_locked)
            }
            else -> error("It isn't exist additional info for this case")
        }
    }

    private fun resolveSingleCurrencyInfo(
        cardTypeResolver: CardTypesResolver,
        isLocked: Boolean,
        currencyAmount: BigDecimal?,
    ): TextReference {
        return if (isLocked) {
            TextReference.Res(R.string.common_locked)
        } else {
            val blockchain = cardTypeResolver.getBlockchain()
            val amount = currencyAmount?.let {
                BigDecimalFormatter.formatCryptoAmount(
                    cryptoAmount = it,
                    cryptoCurrency = blockchain.currency,
                    decimals = blockchain.decimals(),
                )
            }

            TextReference.Str(value = amount.orEmpty())
        }
    }
}