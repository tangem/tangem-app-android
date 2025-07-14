package com.tangem.domain.wallets.builder

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.UnlockHotWallet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class HotUserWalletBuilder @AssistedInject constructor(
    @Assisted private val hotWalletId: HotWalletId,
    private val hotSdk: TangemHotSdk,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
) {

    suspend fun build(): UserWallet.Hot {
        val allNetworks = Blockchain.entries // TODO use HotDerivationsRepository to get supported networks
        val requests = allNetworks.map {
            val curves = it.getSupportedCurves()
            val derivationPath = it.derivationPath(DerivationStyle.V3)

            curves.map {
                DeriveWalletRequest.Request(
                    curve = it,
                    paths = listOfNotNull(derivationPath),
                )
            }
        }.flatten()

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

        return UserWallet.Hot(
            name = generateWalletNameUseCase.invokeForHot(),
            walletId = UserWalletId(wallets.first().publicKey),
            hotWalletId = hotWalletId,
            wallets = wallets,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(hotWalletId: HotWalletId): HotUserWalletBuilder
    }
}