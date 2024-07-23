package com.tangem.domain.redux

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import org.rekotlin.Action
import java.math.BigDecimal

sealed interface LegacyAction : Action {

    data class SendEmailSupport(val scanResponse: ScanResponse) : LegacyAction

    data class SendEmailRateCanBeBetter(val scanResponse: ScanResponse) : LegacyAction

    /**
     * Initiate an onboarding process.
     * For resuming unfinished backup of standard Wallet see
     * BackupAction.CheckForUnfinishedBackup, GlobalAction.Onboarding.StartForUnfinishedBackup
     */
    data class StartOnboardingProcess(val scanResponse: ScanResponse, val canSkipBackup: Boolean = true) : LegacyAction

    /**
     * Sending an email to support when sending transaction failed
     *
     * @param cryptoCurrency current currency
     * @param userWalletId selected user wallet id
     * @param amount sending amount
     * @param fee spending fee
     * @param destinationAddress recipient address
     * @param errorMessage sending error
     */
    data class SendEmailTransactionFailed(
        val cryptoCurrency: CryptoCurrency,
        val userWalletId: UserWalletId,
        val amount: BigDecimal?,
        val fee: BigDecimal?,
        val destinationAddress: String?,
        val errorMessage: String,
        val scanResponse: ScanResponse,
    ) : LegacyAction
}