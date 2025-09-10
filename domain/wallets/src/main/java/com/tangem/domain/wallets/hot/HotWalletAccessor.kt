package com.tangem.domain.wallets.hot

import com.tangem.hot.sdk.model.*

interface HotWalletAccessor {

    suspend fun signHashes(hotWalletId: HotWalletId, dataToSign: List<DataToSign>): List<SignedData>

    suspend fun derivePublicKeys(hotWalletId: HotWalletId, request: DeriveWalletRequest): DerivedPublicKeyResponse

    suspend fun exportSeedPhrase(hotWalletId: HotWalletId): SeedPhrasePrivateInfo

    suspend fun unlockContextual(hotWalletId: HotWalletId): UnlockHotWallet

    fun getContextualUnlock(hotWalletId: HotWalletId): UnlockHotWallet?

    fun clearContextualUnlock(hotWalletId: HotWalletId)

    fun clearAllContextualUnlock()
}