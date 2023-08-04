package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.common.CardTypesResolver
import com.tangem.utils.toFormattedCurrencyString
import java.math.BigDecimal

/**
 * Wallet additional info factory
 *
[REDACTED_AUTHOR]
 */
// TODO: Finalize strings [REDACTED_JIRA]
internal object WalletAdditionalInfoFactory {

    /**
     * Get additional info
     *
     * @param cardTypesResolver card types resolver
     * @param isLocked          check if wallet is locked
     * @param currencyAmount    amount of currency
     */
    fun resolve(cardTypesResolver: CardTypesResolver, isLocked: Boolean, currencyAmount: BigDecimal? = null): String {
        return if (cardTypesResolver.isMultiwalletAllowed()) {
            val backupInfo = "${cardTypesResolver.getBackupCardsCount()} cards"
            when {
                cardTypesResolver.isWallet2() && !isLocked -> "$backupInfo • Seed phrase"
                cardTypesResolver.isTangemWallet() && !isLocked -> backupInfo
                isLocked -> "$backupInfo • Locked"
                else -> ""
            }
        } else {
            if (isLocked) {
                "Locked"
            } else {
                val blockchain = cardTypesResolver.getBlockchain()
                currencyAmount?.toFormattedCurrencyString(
                    decimals = blockchain.decimals(),
                    currency = blockchain.currency,
                ).orEmpty()
            }
        }
    }
}