package com.tangem.domain.walletconnect.usecase.pay

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.model.pay.WcPayConfirmResult
import com.tangem.domain.walletconnect.model.pay.WcPayRequiredAction
import com.tangem.domain.walletconnect.model.pay.WcPaymentOptionsResponse

interface WcPayUseCase {

    fun isPaymentLink(uri: String): Boolean

    suspend fun getPaymentOptions(paymentLink: String, accounts: List<String>,): Result<WcPaymentOptionsResponse>

    suspend fun getRequiredActions(paymentId: String, optionId: String,): Result<List<WcPayRequiredAction>>

    suspend fun confirmPayment(
        paymentId: String,
        optionId: String,
        signatures: List<String>,
    ): Result<WcPayConfirmResult>

    /**
     * Signs a single Pay action (eth_signTypedData_v4, personal_sign, etc.)
     * and returns the hex-encoded signature string.
     */
    suspend fun signPayAction(action: WcPayRequiredAction, userWallet: UserWallet,): Result<String>

    /**
     * Builds CAIP-10 formatted account list for the given wallet.
     * Returns addresses in format "eip155:{chainId}:{address}" for all supported EVM networks.
     */
    suspend fun buildPayAccounts(userWallet: UserWallet): List<String>
}