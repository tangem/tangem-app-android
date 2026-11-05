package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import com.tangem.domain.wallets.registration.WalletLinkRequester
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Gate for a user action of a feature behind mobile authentication: such a request fails for a wallet not linked to
 * this device, so the user is asked to link it first. Call it from the action, not around background requests — it
 * shows a sheet.
 */
class EnsureWalletLinkedUseCase @Inject constructor(
    private val walletDeviceLinkRepository: WalletDeviceLinkRepository,
    private val walletLinkRequester: WalletLinkRequester,
) {

    /** Returns `false` when the user closed the sheet or linking failed, so the action must not go on. */
    suspend operator fun invoke(userWallet: UserWallet): Boolean {
        // An unknown status lets the action go: asking every time would nag a wallet that is most likely linked
        val isLinked = walletDeviceLinkRepository.isLinked(userWallet).firstOrNull()
        if (isLinked != false) return true

        return walletLinkRequester.requestLink(userWallet) == WalletLinkRequester.Result.Linked
    }
}