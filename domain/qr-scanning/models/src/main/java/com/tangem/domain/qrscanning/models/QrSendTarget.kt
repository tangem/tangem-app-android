package com.tangem.domain.qrscanning.models

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

sealed class QrSendTarget {

    /** Single match — navigate directly to Send */
    data class Single(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val address: String,
        val amount: BigDecimal?,
        val memo: String?,
    ) : QrSendTarget()

    /** Multiple matches — data for bottom sheet selection */
    data class Multiple(
        val address: String,
        val amount: BigDecimal?,
        val memo: String?,
        val walletGroups: List<WalletGroup>,
    ) : QrSendTarget() {

        data class WalletGroup(
            val userWalletId: UserWalletId,
            val walletName: String,
            val accounts: List<AccountGroup>,
        )

        data class AccountGroup(
            val accountId: AccountId,
            val accountName: AccountName,
            val currencies: List<CryptoCurrency>,
            val hiddenTokensCount: Int = 0,
        )
    }

    data object AddressSameAsWallet : QrSendTarget()

    data class Warning(
        val target: QrSendTarget,
        val unsupportedParams: Map<String, String>,
    ) : QrSendTarget()

    data class WalletConnect(val uri: String) : QrSendTarget()

    data class Error(val error: ClassifiedQrContent.Error) : QrSendTarget()
}