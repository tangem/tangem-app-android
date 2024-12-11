package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class GetCurrencyCheckUseCase(
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        amount: BigDecimal?,
        fee: BigDecimal?,
        balanceAfterTransaction: BigDecimal?,
        recipientAddress: String? = null,
    ): CryptoCurrencyCheck {
        return withContext(dispatchers.io) {
            val currency = currencyStatus.currency
            val network = currency.network
            val dustValue = currencyChecksRepository.getDustValue(userWalletId, network)
            val reserveAmount = currencyChecksRepository.getReserveAmount(userWalletId, network)
            val minimumSendAmount = currencyChecksRepository.getMinimumSendAmount(userWalletId, network)
            val existentialDeposit = currencyChecksRepository.getExistentialDeposit(userWalletId, network)
            val rentWarning = currencyChecksRepository.getRentExemptionError(
                userWalletId = userWalletId,
                currencyStatus = currencyStatus,
                balanceAfterTransaction = balanceAfterTransaction ?: BigDecimal.ZERO,
            )
            val isAccountFunded = recipientAddress?.let {
                currencyChecksRepository.checkIfAccountFunded(
                    userWalletId,
                    network,
                    recipientAddress,
                )
            } ?: false
            val utxoAmountLimit = if (currency is CryptoCurrency.Coin && amount != null && fee != null) {
                currencyChecksRepository.checkUtxoAmountLimit(
                    userWalletId = userWalletId,
                    network = network,
                    currency = currencyStatus.currency,
                    amount = amount,
                    fee = fee,
                )
            } else {
                null
            }

            CryptoCurrencyCheck(
                dustValue = dustValue,
                reserveAmount = reserveAmount,
                minimumSendAmount = minimumSendAmount,
                existentialDeposit = existentialDeposit,
                utxoAmountLimit = utxoAmountLimit,
                isAccountFunded = isAccountFunded,
                rentWarning = rentWarning,
            )
        }
    }
}