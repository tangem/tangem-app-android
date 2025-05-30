package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.isLocked
import com.tangem.domain.wallets.models.isMultiCurrency
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAdditionalInfo
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
    fun resolve(wallet: UserWallet, currencyAmount: BigDecimal? = null): WalletAdditionalInfo {
        return when (wallet) {
            is UserWallet.Cold -> {
                if (wallet.isMultiCurrency) {
                    wallet.resolveMultiCurrencyInfo()
                } else {
                    wallet.resolveSingleCurrencyInfo(currencyAmount)
                }
            }
            is UserWallet.Hot -> TODO("[REDACTED_TASK_KEY]")
        }
    }

    private fun UserWallet.Cold.resolveMultiCurrencyInfo(): WalletAdditionalInfo {
        return if (isLocked) {
            WalletAdditionalInfo(
                hideable = false,
                content = getBackupInfoWithDivider(
                    backupCardsCount = getCardsCount(),
                ) + TextReference.Res(R.string.common_locked),
            )
        } else {
            val cardTypeResolver = scanResponse.cardTypesResolver
            if (cardTypeResolver.isWallet2()) {
                resolveWallet2Info()
            } else {
                getBackupInfo(backupCardsCount = getCardsCount())
            }
        }
    }

    private fun UserWallet.Cold.resolveWallet2Info(): WalletAdditionalInfo {
        return if (isImported) {
            WalletAdditionalInfo(
                hideable = false,
                content = getBackupInfoWithDivider(backupCardsCount = getCardsCount()) + TextReference.Res(
                    id = R.string.common_seed_phrase,
                ),
            )
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

    private fun getBackupInfo(backupCardsCount: Int?): WalletAdditionalInfo {
        val content = if (backupCardsCount != null) {
            getBackupInfoTextReference(count = backupCardsCount)
        } else {
            TextReference.EMPTY
        }

        return WalletAdditionalInfo(hideable = false, content = content)
    }

    private fun getBackupInfoTextReference(count: Int): TextReference {
        return TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = count,
            formatArgs = wrappedList(count),
        )
    }

    private fun UserWallet.Cold.resolveSingleCurrencyInfo(currencyAmount: BigDecimal?): WalletAdditionalInfo {
        return if (isLocked) {
            WalletAdditionalInfo(hideable = false, content = TextReference.Res(R.string.common_locked))
        } else {
            val blockchain = scanResponse.cardTypesResolver.getBlockchain()
            val amount = currencyAmount?.format { crypto(blockchain.currency, blockchain.decimals()) }

            WalletAdditionalInfo(hideable = true, content = TextReference.Str(value = amount.orEmpty()))
        }
    }
}