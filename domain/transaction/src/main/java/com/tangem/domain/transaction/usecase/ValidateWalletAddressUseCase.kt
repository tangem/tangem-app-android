package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.error.ValidateAddressError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for validating wallet address.
 */
class ValidateWalletAddressUseCase(
    private val walletAddressServiceRepository: WalletAddressServiceRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        address: String,
        currencyAddress: Set<NetworkAddress.Address>?,
    ): Either<ValidateAddressError, Unit> {
        val isUtxoConsolidationAvailable =
            walletManagersFacade.checkUtxoConsolidationAvailability(userWalletId, network)
        val isCurrentAddress = currencyAddress?.any { it.value == address } ?: true

        val isForbidSelfSend = isCurrentAddress && !isUtxoConsolidationAvailable
        val isValidAddress = walletAddressServiceRepository.validateAddress(userWalletId, network, address)

        return when {
            !isValidAddress -> ValidateAddressError.InvalidAddress.left()
            isForbidSelfSend -> ValidateAddressError.AddressInWallet.left()
            else -> Unit.right()
        }
    }
}