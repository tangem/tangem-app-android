package com.tangem.tap.common.libs.blockchainsdk

import androidx.annotation.VisibleForTesting
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.core.analytics.models.Basic.TransactionSent.WalletForm
import com.tangem.core.analytics.store.LastSignedWalletFormStore
import com.tangem.data.card.TransactionSignerFactory
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.update
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.domain.TangemSignerResponse
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch

internal class DefaultTransactionSignerFactory(
    private val lastSignedWalletFormStore: LastSignedWalletFormStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val coroutineScope: AppCoroutineScope,
) : TransactionSignerFactory {

    override fun createTransactionSigner(
        cardId: String?,
        sdk: TangemSdk,
        twinKey: TwinKey?,
        userWalletId: UserWalletId,
    ): TransactionSigner {
        return TangemSigner(
            cardId = cardId,
            tangemSdk = sdk,
            initialMessage = Message(),
            twinKey = twinKey,
        ) { signResponse ->
            onSignerResponse(userWalletId, signResponse)
        }
    }

    @VisibleForTesting
    internal fun onSignerResponse(userWalletId: UserWalletId, signResponse: TangemSignerResponse) {
        lastSignedWalletFormStore.update(
            if (signResponse.isRing) WalletForm.Ring else WalletForm.Card,
        )

        coroutineScope.launch {
            userWalletsListRepository.update(userWalletId) { userWallet ->
                userWallet.updateSignedHashes(signResponse)
            }
        }
    }

    private fun UserWallet.updateSignedHashes(signResponse: TangemSignerResponse): UserWallet {
        if (this !is UserWallet.Cold) return this

        return copy(
            scanResponse = scanResponse.copy(
                card = scanResponse.card.copy(
                    wallets = scanResponse.card.wallets.map { wallet ->
                        if (wallet.publicKey.contentEquals(signResponse.signedWalletPublicKey)) {
                            wallet.copy(
                                // Keep previously known counters if the signer response does not provide them,
                                // otherwise we would regress the UI counters to null.
                                totalSignedHashes = signResponse.totalSignedHashes ?: wallet.totalSignedHashes,
                                remainingSignatures = signResponse.remainingSignatures ?: wallet.remainingSignatures,
                            )
                        } else {
                            wallet
                        }
                    },
                ),
            ),
        )
    }
}