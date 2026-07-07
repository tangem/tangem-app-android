package com.tangem.domain.addressbook.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetNetworkAddressesUseCase
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase

/**
 * Validates a contact's address for a network, reusing the transaction-layer validation. Self-send
 * is allowed since saving one's own address in the book is valid.
 */
class ValidateContactAddressUseCase(
    private val validateWalletAddressUseCase: ValidateWalletAddressUseCase,
    private val getNetworkAddressesUseCase: GetNetworkAddressesUseCase,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        address: String,
    ): Either<AddressValidation.Error, Unit> = either {
        val senderAddresses = getNetworkAddressesUseCase.invokeSync(
            userWalletId = userWalletId,
            networkRawId = network.id.rawId,
        )
        validateWalletAddressUseCase(
            userWalletId = userWalletId,
            network = network,
            address = address,
            senderAddresses = senderAddresses,
            allowSelfSend = true,
        ).bind()
    }
}