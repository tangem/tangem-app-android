package com.tangem.domain.sociallogin.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Whether social recovery is set up for a wallet — the single source for treating it as a backup
 * alongside the seed phrase and the cloud backup (FR-15). Reads the local flag, so it never waits on
 * the cloud or the backend.
 *
 * With the feature off the answer is always `false`, including for wallets whose flag was already
 * stored: social recovery cannot be used while the feature is disabled, so treating it as a backup
 * would hide the "finalize setup" prompts from a wallet that has none.
 */
class IsSocialBackupCompletedUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles,
) {

    suspend operator fun invoke(walletId: UserWalletId): Boolean {
        return socialLoginFeatureToggles.isSocialLoginEnabled &&
            socialBackupRepository.isSocialBackupCompleted(walletId)
    }

    fun flow(walletId: UserWalletId): Flow<Boolean> = if (socialLoginFeatureToggles.isSocialLoginEnabled) {
        socialBackupRepository.isSocialBackupCompletedFlow(walletId)
    } else {
        flowOf(false)
    }
}