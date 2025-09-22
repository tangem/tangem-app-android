package com.tangem.tap.features.hot

import com.tangem.crypto.bip39.Mnemonic
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Proxy for [TangemHotSdk] to allow lazy initialization and provide a way to access the SDK state from domain layer.
 * SDK is initialized in the [com.tangem.tap.routing.component.RoutingComponent] and can be accessed through this proxy.
 * Be aware that the SDK is initialized on activity creation, so it may not be available immediately.
 */
@Singleton
class TangemHotSDKProxy @Inject constructor() : TangemHotSdk {

    val sdkState = MutableStateFlow<TangemHotSdk?>(null)

    override suspend fun importWallet(mnemonic: Mnemonic, passphrase: CharArray?, auth: HotAuth): HotWalletId =
        callSdk { importWallet(mnemonic, passphrase, auth) }

    override suspend fun generateWallet(auth: HotAuth, mnemonicType: MnemonicType): HotWalletId =
        callSdk { generateWallet(auth, mnemonicType) }

    override suspend fun exportMnemonic(unlockHotWallet: UnlockHotWallet): SeedPhrasePrivateInfo =
        callSdk { exportMnemonic(unlockHotWallet) }

    override suspend fun exportBackup(unlockHotWallet: UnlockHotWallet): ByteArray =
        callSdk { exportBackup(unlockHotWallet) }

    override suspend fun clearUnlockContext(hotWalletId: HotWalletId) {
        callSdk { clearUnlockContext(hotWalletId) }
    }

    override suspend fun getContextUnlock(unlockHotWallet: UnlockHotWallet): UnlockHotWallet =
        callSdk { getContextUnlock(unlockHotWallet) }

    override suspend fun delete(id: HotWalletId) = callSdk { delete(id) }

    override suspend fun changeAuth(unlockHotWallet: UnlockHotWallet, auth: HotAuth): HotWalletId =
        callSdk { changeAuth(unlockHotWallet, auth) }

    override suspend fun removeBiometryAuthIfPresented(id: HotWalletId): HotWalletId =
        callSdk { removeBiometryAuthIfPresented(id) }

    override suspend fun derivePublicKey(
        unlockHotWallet: UnlockHotWallet,
        request: DeriveWalletRequest,
    ): DerivedPublicKeyResponse = callSdk { derivePublicKey(unlockHotWallet, request) }

    override suspend fun signHashes(unlockHotWallet: UnlockHotWallet, dataToSign: List<DataToSign>): List<SignedData> =
        callSdk { signHashes(unlockHotWallet, dataToSign) }

    private suspend fun <T> callSdk(block: suspend TangemHotSdk.() -> T): T {
        return withTimeout(timeMillis = 1000) {
            sdkState.filterNotNull().first()
        }.block()
    }
}