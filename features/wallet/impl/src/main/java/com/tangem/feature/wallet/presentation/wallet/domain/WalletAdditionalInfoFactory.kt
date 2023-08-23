package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
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
     * @param cardTypesResolver card type resolver
     * @param wallet            current wallet
     * @param currencyAmount    amount of currency
     */
    fun resolve(
        cardTypesResolver: CardTypesResolver,
        wallet: UserWallet,
        currencyAmount: BigDecimal? = null,
    ): TextReference {
        return if (cardTypesResolver.isMultiwalletAllowed()) {
            resolveMultiCurrencyInfo(cardTypesResolver, wallet)
        } else {
            resolveSingleCurrencyInfo(cardTypesResolver, wallet, currencyAmount)
        }
    }

    private fun resolveMultiCurrencyInfo(cardTypeResolver: CardTypesResolver, wallet: UserWallet): TextReference {
        val backupCardsCount = wallet.cardsInWallet.size + 1
        val backupInfoRes = TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = backupCardsCount,
            formatArgs = wrappedList(backupCardsCount),
        )

        return if (wallet.isLocked) {
            backupInfoRes + DIVIDER_RES + TextReference.Res(R.string.common_locked)
        } else {
            if (cardTypeResolver.isWallet2()) {
                backupInfoRes + DIVIDER_RES + TextReference.Res(id = R.string.common_seed_phrase)
            } else {
                backupInfoRes
            }
        }
    }

    private fun resolveSingleCurrencyInfo(
        cardTypeResolver: CardTypesResolver,
        wallet: UserWallet,
        currencyAmount: BigDecimal?,
    ): TextReference {
        return if (wallet.isLocked) {
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