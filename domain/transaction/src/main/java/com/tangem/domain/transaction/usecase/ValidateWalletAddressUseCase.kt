package com.tangem.domain.transaction.usecase

import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.ResolveAddressResult
import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.AddressValidationResult
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWalletId
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
        currencyAddresses: Set<NetworkAddress.Address>?,
    ): AddressValidationResult = validateAddressInternal(
        userWalletId,
        network,
        address,
        isCurrentAddress = { toValidate ->
            currencyAddresses?.any { it.value == toValidate } ?: true
        },
    )

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        address: String,
        senderAddresses: List<CryptoCurrencyAddress>,
    ): AddressValidationResult = validateAddressInternal(
        userWalletId,
        network,
        address,
        isCurrentAddress = { toValidate ->
            senderAddresses.any { it.address == toValidate }
        },
    )

    private suspend fun validateAddressInternal(
        userWalletId: UserWalletId,
        network: Network,
        address: String,
        isCurrentAddress: (String) -> Boolean,
    ): AddressValidationResult {
        val decodedXAddress = BlockchainUtils.decodeRippleXAddress(address, network.rawId)
        val isUtxoConsolidationAvailable =
            walletManagersFacade.checkUtxoConsolidationAvailability(userWalletId, network)

        val addressToValidate = decodedXAddress?.address ?: address
        val current = isCurrentAddress(addressToValidate)
        val isForbidSelfSend = current && !isUtxoConsolidationAvailable
        val isValidAddress = walletAddressServiceRepository.validateAddress(userWalletId, network, addressToValidate)

        return when {
            !isValidAddress -> {
                val resolveAddressResult = walletAddressServiceRepository.resolveAddress(
                    userWalletId = userWalletId,
                    network = network,
                    address = addressToValidate,
                )

                if (resolveAddressResult is ResolveAddressResult.Resolved) {
                    AddressValidation.Success.ValidNamedAddress(resolveAddressResult.address).right()
                } else {
                    AddressValidation.Error.InvalidAddress.left()
                }
            }
            isForbidSelfSend -> AddressValidation.Error.AddressInWallet.left()
            decodedXAddress != null -> AddressValidation.Success.ValidXAddress.right()
            else -> AddressValidation.Success.Valid.right()
        }
    }
}