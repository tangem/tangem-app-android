package com.tangem.domain.wallets.registration

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow

/**
 * Links a wallet to this device with the Tangem Auth Service on the user's explicit request, unlike the silent
 * best-effort registration behind [WalletRegistrationTrigger]. Features behind mobile authentication are
 * unavailable for a wallet until it is linked.
 */
interface WalletDeviceLinkRepository {

    /**
     * Whether [userWallet] is linked to this device, re-emitted whenever any path (this repository, a card scan,
     * transaction signing, a hot wallet unlock) changes it. Wallets that can't be linked emit `true`, so callers
     * never ask the user to link them.
     */
    fun isLinked(userWallet: UserWallet): Flow<Boolean>

    /** Requests a wallet nonce, has the card or the hot wallet key sign it and registers the signature. */
    suspend fun link(userWallet: UserWallet): Either<WalletLinkError, Unit>
}