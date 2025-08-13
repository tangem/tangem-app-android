package com.tangem.domain.wallets.builder

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext

class HotUserWalletBuilder @AssistedInject constructor(
    @Assisted private val hotWalletId: HotWalletId,
    private val hotSdk: TangemHotSdk,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) {

    suspend fun build(): UserWallet.Hot = withContext(dispatcherProvider.default) {
        val allNetworks = Blockchain.entries // TODO [REDACTED_TASK_KEY] [Hot Wallet] Derivation config for hot wallet
        val curves = allNetworks.map { it.getSupportedCurves() }.flatten().toSet()
        val requests = curves.sortedBy { it.ordinal }.map { curve ->
            val derivationPaths = allNetworks.filter { curve in it.getSupportedCurves() }
                .mapNotNull {
                    val derivationPath = it.derivationPath(DerivationStyle.V3) ?: return@mapNotNull null
                    if (it == Blockchain.Cardano) {
                        val extendedDerivationPath = CardanoUtils.extendedDerivationPath(derivationPath)
                        listOf(derivationPath, extendedDerivationPath)
                    } else {
                        listOf(derivationPath)
                    }
                }.flatten()

            DeriveWalletRequest.Request(
                curve = curve,
                paths = derivationPaths,
            )
        }

        val derivationResult = hotSdk.derivePublicKey(
            unlockHotWallet = UnlockHotWallet(
                walletId = hotWalletId,
                auth = HotAuth.NoAuth,
            ),
            request = DeriveWalletRequest(
                requests = requests,
            ),
        )

        val wallets = derivationResult.responses.map {
            MobileWallet(
                publicKey = it.seedKey.publicKey,
                chainCode = it.seedKey.chainCode,
                curve = it.curve,
                derivedKeys = it.publicKeys,
            )
        }

        UserWallet.Hot(
            name = generateWalletNameUseCase.invokeForHot(),
            walletId = UserWalletIdBuilder.walletPublicKey(wallets.first().publicKey),
            hotWalletId = hotWalletId,
            wallets = wallets,
            backedUp = false,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(hotWalletId: HotWalletId): HotUserWalletBuilder
    }
}