package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

class GetCurrencyCheckUseCase(
    private val currencyChecksRepository: CurrencyChecksRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        amount: BigDecimal?,
        fee: BigDecimal?,
        recipientAddress: String? = null,
    ): CryptoCurrencyCheck {
        val network = currencyStatus.currency.network
        val dustValue = currencyChecksRepository.getDustValue(userWalletId, network)
        val reserveAmount = currencyChecksRepository.getReserveAmount(userWalletId, network)
        val existentialDeposit = currencyChecksRepository.getExistentialDeposit(userWalletId, network)
        val isAccountFunded = recipientAddress?.let {
            currencyChecksRepository.checkIfAccountFunded(
                userWalletId,
                network,
                recipientAddress,
            )
        } ?: false
        val utxoAmountLimit = if (amount != null && fee != null) {
            currencyChecksRepository.checkUtxoAmountLimit(
                userWalletId = userWalletId,
                network = network,
                amount = amount,
                fee = fee,
            )
        } else {
            null
        }

        return CryptoCurrencyCheck(
            dustValue = dustValue,
            reserveAmount = reserveAmount,
            existentialDeposit = existentialDeposit,
            utxoAmountLimit = utxoAmountLimit,
            isAccountFunded = isAccountFunded,
        )
    }
}
