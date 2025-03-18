package com.tangem.domain.transaction.usecase

import arrow.core.left
import arrow.core.right
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.AddressValidationResult
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.lib.crypto.BlockchainUtils

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
    ): AddressValidationResult {
        val isUtxoConsolidationAvailable =
            walletManagersFacade.checkUtxoConsolidationAvailability(userWalletId, network)
        val isCurrentAddress = currencyAddress?.any { it.value == address } ?: true

        val isForbidSelfSend = isCurrentAddress && !isUtxoConsolidationAvailable

        val decodedXAddress = BlockchainUtils.decodeRippleXAddress(address, network.id.value)
        val addressToValidate = decodedXAddress?.address ?: address

        val isValidAddress = walletAddressServiceRepository.validateAddress(userWalletId, network, addressToValidate)

        return when {
            !isValidAddress -> AddressValidation.Error.InvalidAddress.left()
            isForbidSelfSend -> AddressValidation.Error.AddressInWallet.left()
            decodedXAddress != null -> AddressValidation.Success.ValidXAddress.right()
            else -> AddressValidation.Success.Valid.right()
        }
    }
}