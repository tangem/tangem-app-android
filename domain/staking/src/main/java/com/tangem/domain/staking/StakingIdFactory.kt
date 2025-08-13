package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.walletmanager.WalletManagersFacade

/**
 * Factory class for creating instances of [StakingID]
 *
 * @property walletManagersFacade wallet manager facade
 *
[REDACTED_AUTHOR]
 */
class StakingIdFactory(
    private val walletManagersFacade: WalletManagersFacade,
) {

    /**
     * Creates a [StakingID] for the given user wallet and cryptocurrency
     *
     * @param userWalletId   the unique identifier of the user's wallet

     */
    suspend fun create(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Either<Error, StakingID> {
        return create(userWalletId = userWalletId, currencyId = cryptoCurrency.id, network = cryptoCurrency.network)
    }

    /**
     * Creates a [StakingID] for the given user wallet, currency, and network
     *
     * @param userWalletId the unique identifier of the user's wallet
     * @param currencyId   the identifier of the cryptocurrency
     * @param network      the network associated with the staking operation
     */
    suspend fun create(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network,
    ): Either<Error, StakingID> = either {
        val integrationId = StakingIntegrationID.create(currencyId = currencyId)

        ensureNotNull(integrationId) { Error.UnsupportedCurrency }

        val address = walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = network)

        ensureNotNull(address) { Error.UnableToGetAddress(integrationId = integrationId) }

        StakingID(integrationId = integrationId.value, address = address)
    }

    /** Represents possible errors that can occur during staking ID creation */
    sealed interface Error {

        /** Error indicating that the currency is not supported for staking */
        data object UnsupportedCurrency : Error

        /** Error indicating that the address could not be retrieved */
        data class UnableToGetAddress(val integrationId: StakingIntegrationID) : Error
    }
}