package com.tangem.domain.wallets.hot

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.model.*

interface HotWalletAccessor {

    suspend fun signHashes(hotWalletId: HotWalletId, dataToSign: List<DataToSign>): List<SignedData>

    /**
     * Signs [dataToSign] reusing an already-established contextual unlock for [hotWalletId] (no auth
     * prompt). Falls back to [signHashes] (which may prompt) when no contextual unlock is held — so a
     * caller that established one via [unlockContextual] gets a prompt-free signature, while callers
     * without a context keep the ordinary behaviour.
     */
    suspend fun signHashesReusingContextualUnlock(
        hotWalletId: HotWalletId,
        dataToSign: List<DataToSign>,
    ): List<SignedData>

    suspend fun derivePublicKeys(hotWalletId: HotWalletId, request: DeriveWalletRequest): DerivedPublicKeyResponse

    suspend fun exportSeedPhrase(hotWalletId: HotWalletId): SeedPhrasePrivateInfo

    suspend fun unlockContextual(hotWalletId: HotWalletId): UnlockHotWallet

    /**
     * Establishes the contextual unlock for [hotWalletId] with an [auth] the user has already given (e.g.
     * the access code just entered to unlock the app), so no auth prompt is shown.
     */
    suspend fun unlockContextual(hotWalletId: HotWalletId, auth: HotAuth): UnlockHotWallet

    /**
     * Establishes the contextual unlock for [hotWalletId] by reusing the biometric authentication the user
     * has just passed (e.g. to unlock the app), never showing a prompt.
     *
     * @throws com.tangem.hot.sdk.exception.RecentBiometryUnavailableException if the wallet is not
     * biometry-protected, the access code is required for it, or there is no recent biometric authentication to reuse.
     */
    suspend fun unlockContextualWithRecentBiometry(hotWalletId: HotWalletId): UnlockHotWallet

    fun getContextualUnlock(hotWalletId: HotWalletId): UnlockHotWallet?

    fun clearContextualUnlock(hotWalletId: HotWalletId)

    fun clearContextualUnlock(userWalletId: UserWalletId)

    fun clearAllContextualUnlock()
}