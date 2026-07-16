package com.tangem.data.wallets.hot

import com.tangem.common.core.TangemSdkError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.repositories.LegacySettingsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.exception.WrongPasswordException
import com.tangem.hot.sdk.model.*
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultHotWalletAccessor @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val hotWalletPasswordRequester: HotWalletPasswordRequester,
    private val walletsRepository: WalletsRepository,
    private val legacySettingsRepository: LegacySettingsRepository,
    private val scope: AppCoroutineScope,
) : HotWalletAccessor {

    private data class RequestedAuth(
        val auth: HotAuth,
        val attemptRequest: HotWalletPasswordRequester.AttemptRequest?,
    )

    private val contextualUnlockHotWallet: ConcurrentHashMap<HotWalletId, UnlockHotWallet?> = ConcurrentHashMap()

    override suspend fun signHashes(hotWalletId: HotWalletId, dataToSign: List<DataToSign>): List<SignedData> =
        hotSdkRequest(hotWalletId) { unlock ->
            tangemHotSdk.signHashes(unlockHotWallet = unlock, dataToSign = dataToSign)
        }

    override suspend fun derivePublicKeys(
        hotWalletId: HotWalletId,
        request: DeriveWalletRequest,
    ): DerivedPublicKeyResponse = hotSdkRequest(hotWalletId) { unlock ->
        tangemHotSdk.derivePublicKey(unlockHotWallet = unlock, request = request)
    }

    override suspend fun exportSeedPhrase(hotWalletId: HotWalletId): SeedPhrasePrivateInfo {
        val unlockHotWallet = contextualUnlockHotWallet[hotWalletId] ?: hotSdkRequest(hotWalletId) { it }
        return tangemHotSdk.exportMnemonic(unlockHotWallet = unlockHotWallet)
    }

    override suspend fun unlockContextual(hotWalletId: HotWalletId): UnlockHotWallet = hotSdkRequest(hotWalletId) {
        tangemHotSdk.getContextUnlock(it).also { unlockHotWallet ->
            contextualUnlockHotWallet[hotWalletId] = unlockHotWallet
        }
    }

    override fun getContextualUnlock(hotWalletId: HotWalletId): UnlockHotWallet? =
        contextualUnlockHotWallet[hotWalletId]

    override fun clearContextualUnlock(hotWalletId: HotWalletId) {
        contextualUnlockHotWallet.remove(hotWalletId)
        scope.launch {
            tangemHotSdk.clearUnlockContext(hotWalletId)
        }
    }

    override fun clearContextualUnlock(userWalletId: UserWalletId) {
        scope.launch {
            val userWallet = userWalletsListRepository.userWalletsSync()
                .find { it is UserWallet.Hot && it.walletId == userWalletId }
                as? UserWallet.Hot
                ?: return@launch
            clearContextualUnlock(userWallet.hotWalletId)
        }
    }

    override fun clearAllContextualUnlock() {
        val hotWalletsIds = contextualUnlockHotWallet.keys.toList()
        contextualUnlockHotWallet.clear()
        scope.launch {
            hotWalletsIds.forEach { tangemHotSdk.clearUnlockContext(it) }
        }
    }

    private suspend fun <T> hotSdkRequest(hotWalletId: HotWalletId, block: suspend (unlock: UnlockHotWallet) -> T): T {
        val isAccessCodeRequired = isAccessCodeRequired()

        val requestedAuth = when (hotWalletId.authType) {
            HotWalletId.AuthType.NoPassword -> RequestedAuth(HotAuth.NoAuth, attemptRequest = null)
            HotWalletId.AuthType.Password -> requestPassword(
                hotWalletId = hotWalletId,
                hasBiometry = false,
            )
            HotWalletId.AuthType.Biometry -> {
                if (isAccessCodeRequired) {
                    requestPassword(
                        hotWalletId = hotWalletId,
                        hasBiometry = false,
                    )
                } else {
                    RequestedAuth(HotAuth.Biometry, attemptRequest = null)
                }
            }
        }

        return runCatchingSdkErrors(hotWalletId, requestedAuth) {
            block(UnlockHotWallet(hotWalletId, it))
        }
    }

    private suspend fun <T> runCatchingSdkErrors(
        hotWalletId: HotWalletId,
        requestedAuth: RequestedAuth,
        block: suspend (auth: HotAuth) -> T,
    ): T {
        return runCatchingWrongPassInternal(
            hotWalletId = hotWalletId,
            originalAuth = requestedAuth,
            requestedAuth = requestedAuth,
            block = { blockAuth ->
                val result = block(blockAuth)

                // Update biometry auth if the original auth was password
                updateBiometryAuthIfNeeded(
                    hotWalletId = hotWalletId,
                    originalAuth = requestedAuth.auth,
                )

                result
            },
        )
    }

    private suspend fun updateBiometryAuthIfNeeded(hotWalletId: HotWalletId, originalAuth: HotAuth) {
        val isAccessCodeRequired = isAccessCodeRequired()
        val isUseBiometricAuthenticationEnabled = walletsRepository.useBiometricAuthentication()

        if (originalAuth is HotAuth.Password && isUseBiometricAuthenticationEnabled && isAccessCodeRequired.not()) {
            val userWallet = userWalletsListRepository.userWalletsSync()
                .first { it is UserWallet.Hot && it.hotWalletId == hotWalletId }
                as UserWallet.Hot

            val newHotWalletId = tangemHotSdk.changeAuth(
                unlockHotWallet = UnlockHotWallet(
                    walletId = hotWalletId,
                    auth = originalAuth,
                ),
                auth = HotAuth.Biometry,
            )

            userWalletsListRepository.saveWithoutLock(
                userWallet = userWallet.copy(
                    hotWalletId = newHotWalletId,
                ),
                canOverride = true,
            )
        }
    }

    private suspend fun <T> runCatchingWrongPassInternal(
        hotWalletId: HotWalletId,
        originalAuth: RequestedAuth,
        requestedAuth: RequestedAuth,
        block: suspend (auth: HotAuth) -> T,
    ): T = runSuspendCatching {
        block(requestedAuth.auth).also {
            val request = requestedAuth.attemptRequest
            if (request != null) {
                hotWalletPasswordRequester.successfulAuthentication(request)
            }
            hotWalletPasswordRequester.dismiss()
        }
    }.getOrElse { exception ->
        if (requestedAuth.auth is HotAuth.Biometry && (exception.isBiometryError() || exception.isBiometryReset())) {
            val shouldRetryBiometry = exception is TangemSdkError.AuthenticationCanceled

            // fallback to password if biometry fails
            val passAuth = requestPassword(
                hotWalletId = hotWalletId,
                hasBiometry = shouldRetryBiometry,
            )

            return@getOrElse runCatchingWrongPassInternal(
                hotWalletId = hotWalletId,
                originalAuth = originalAuth,
                requestedAuth = passAuth,
                block = block,
            )
        }

        if (exception !is WrongPasswordException) {
            throw exception
        }

        // If the exception is a wrong password, we need to request the password again

        requestedAuth.attemptRequest?.let { hotWalletPasswordRequester.wrongPassword(it) }
        val passResult = requestPassword(
            hotWalletId = hotWalletId,
            hasBiometry = originalAuth.auth is HotAuth.Biometry,
        )

        runCatchingWrongPassInternal(
            hotWalletId = hotWalletId,
            originalAuth = originalAuth,
            requestedAuth = passResult,
            block = block,
        )
    }

    private suspend fun requestPassword(hotWalletId: HotWalletId, hasBiometry: Boolean): RequestedAuth {
        val attemptRequest = HotWalletPasswordRequester.AttemptRequest(
            hotWalletId = hotWalletId,
            authMode = false,
            hasBiometry = hasBiometry,
        )

        val auth = hotWalletPasswordRequester.requestPassword(attemptRequest).toAuth()
            ?: throw TangemSdkError.UserCancelled()
        return RequestedAuth(auth, attemptRequest)
    }

    private suspend fun isAccessCodeRequired(): Boolean {
        return walletsRepository.requireAccessCode() || legacySettingsRepository.canUseBiometryStrict().not()
    }

    private fun Throwable.isBiometryReset(): Boolean {
        return this is IllegalStateException
    }

    private fun Throwable.isBiometryError(): Boolean {
        return this is TangemSdkError.AuthenticationFailed ||
            this is TangemSdkError.AuthenticationCanceled ||
            this is TangemSdkError.AuthenticationLockout ||
            this is TangemSdkError.AuthenticationUnavailable ||
            this is TangemSdkError.AuthenticationAlreadyInProgress ||
            this is TangemSdkError.AuthenticationPermanentLockout
    }

    private fun HotWalletPasswordRequester.Result.toAuth() = when (this) {
        HotWalletPasswordRequester.Result.UseBiometry -> HotAuth.Biometry
        HotWalletPasswordRequester.Result.Dismiss -> null
        is HotWalletPasswordRequester.Result.EnteredPassword -> this.password
    }
}