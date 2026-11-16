package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.handleErrorWith
import arrow.core.left
import arrow.core.right
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import java.math.BigDecimal
import javax.inject.Inject

class WithdrawFromCollectiblesVaultUseCase @Inject constructor(
    private val gachaVaultRepository: GachaVaultRepository,
) {

    /**
     * A [CollectiblesAccountError.MaybeSent] that carries a signature is a success: the gasless service books the
     * signature before it sends, so the transaction is tracked like any sent one.
     *
     * @param token the vault's token being withdrawn
     * @param recipientAddress the recipient's wallet address; its ATA for [token] receives the funds
     * @return the transaction signature
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        token: CryptoCurrency.Token,
        recipientAddress: String,
        amount: BigDecimal,
    ): Either<CollectiblesAccountError, String> = gachaVaultRepository.withdraw(
        userWallet = userWallet,
        token = token,
        recipientWallet = recipientAddress,
        amount = amount,
    ).handleErrorWith { error ->
        (error as? CollectiblesAccountError.MaybeSent)?.txHash?.right() ?: error.left()
    }
}