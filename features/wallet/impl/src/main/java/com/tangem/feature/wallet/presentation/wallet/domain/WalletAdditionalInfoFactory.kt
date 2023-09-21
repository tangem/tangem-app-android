package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.impl.R
import java.math.BigDecimal

/**
 * Wallet additional info factory
 *
[REDACTED_AUTHOR]
 */
internal object WalletAdditionalInfoFactory {

    private val DIVIDER by lazy(mode = LazyThreadSafetyMode.NONE) { stringReference(value = " • ") }

    /**
     * Get additional info
     *
     * @param wallet            current wallet
     * @param currencyAmount    amount of currency
     */
    fun resolve(wallet: UserWallet, currencyAmount: BigDecimal? = null): TextReference {
        return if (wallet.isMultiCurrency) {
            wallet.resolveMultiCurrencyInfo()
        } else {
            wallet.resolveSingleCurrencyInfo(currencyAmount)
        }
    }

    private fun UserWallet.resolveMultiCurrencyInfo(): TextReference {
        return if (isLocked) {
            getBackupInfoWithDivider(backupCardsCount = getCardsCount()) + TextReference.Res(R.string.common_locked)
        } else {
            val cardTypeResolver = scanResponse.cardTypesResolver
            if (cardTypeResolver.isWallet2()) {
                resolveWallet2Info()
            } else {
                getBackupInfo(backupCardsCount = getCardsCount())
            }
        }
    }

    private fun UserWallet.resolveWallet2Info(): TextReference {
        return if (isImported) {
            getBackupInfoWithDivider(backupCardsCount = getCardsCount()) +
                TextReference.Res(id = R.string.common_seed_phrase)
        } else {
            getBackupInfo(backupCardsCount = getCardsCount())
        }
    }

    private fun getBackupInfoWithDivider(backupCardsCount: Int?): TextReference {
        return if (backupCardsCount != null) {
            getBackupInfoTextReference(count = backupCardsCount) + DIVIDER
        } else {
            TextReference.EMPTY
        }
    }

    private fun getBackupInfo(backupCardsCount: Int?): TextReference {
        return if (backupCardsCount != null) {
            getBackupInfoTextReference(count = backupCardsCount)
        } else {
            TextReference.EMPTY
        }
    }

    private fun getBackupInfoTextReference(count: Int): TextReference {
        return TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = count,
            formatArgs = wrappedList(count),
        )
    }

    private fun UserWallet.resolveSingleCurrencyInfo(currencyAmount: BigDecimal?): TextReference {
        return if (isLocked) {
            TextReference.Res(R.string.common_locked)
        } else {
            val blockchain = scanResponse.cardTypesResolver.getBlockchain()
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