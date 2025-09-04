package com.tangem.data.wallets.hot

import com.tangem.common.core.TangemSdkError
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.exception.WrongPasswordException
import com.tangem.hot.sdk.model.*
import javax.inject.Inject

class DefaultHotWalletAccessor @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val hotWalletPasswordRequester: HotWalletPasswordRequester,
    private val walletsRepository: WalletsRepository,
) : HotWalletAccessor {

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

    private suspend fun <T> hotSdkRequest(hotWalletId: HotWalletId, block: suspend (unlock: UnlockHotWallet) -> T): T {
        val isAccessCodeRequired = walletsRepository.requireAccessCode()

        val auth = when (hotWalletId.authType) {
            HotWalletId.AuthType.NoPassword -> HotAuth.NoAuth
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
                    HotAuth.Biometry
                }
            }
        }

        return runCatchingSdkErrors(hotWalletId, auth) {
            block(UnlockHotWallet(hotWalletId, it)).also {
                hotWalletPasswordRequester.dismiss()
            }
        }
    }

    private suspend fun <T> runCatchingSdkErrors(
        hotWalletId: HotWalletId,
        auth: HotAuth,
        block: suspend (auth: HotAuth) -> T,
    ): T {
        return runCatchingWrongPassInternal(
            hotWalletId = hotWalletId,
            originalAuth = auth,
            auth = auth,
            block = { blockAuth ->
                block(blockAuth).also {
                    // Update biometry auth if the original auth was password
                    updateBiometryAuthIfNeeded(
                        hotWalletId = hotWalletId,
                        originalAuth = blockAuth,
                    )
                }
            },
        )
    }

    private suspend fun updateBiometryAuthIfNeeded(hotWalletId: HotWalletId, originalAuth: HotAuth) {
        val isAccessCodeRequired = walletsRepository.requireAccessCode()

        if (originalAuth is HotAuth.Password && isAccessCodeRequired.not()) {
            val userWallet = userWalletsListRepository.userWalletsSync()
                .find { it is UserWallet.Hot && it.hotWalletId == hotWalletId }
                as? UserWallet.Hot
                ?: return

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
        originalAuth: HotAuth,
        auth: HotAuth,
        block: suspend (auth: HotAuth) -> T,
    ): T = runCatching {
        block(auth)
    }.getOrElse { exception ->
        if (auth is HotAuth.Biometry && exception.isBiometryError()) {
            // fallback to password if biometry fails
            val passAuth = requestPassword(
                hotWalletId = hotWalletId,
                hasBiometry = true,
            )

            return@getOrElse runCatchingWrongPassInternal(
                hotWalletId = hotWalletId,
                originalAuth = originalAuth,
                auth = passAuth,
                block = block,
            )
        }

        if (exception !is WrongPasswordException) {
            throw exception
        }

        // If the exception is a wrong password, we need to request the password again

        hotWalletPasswordRequester.wrongPassword()
        val passResult = requestPassword(
            hotWalletId = hotWalletId,
            hasBiometry = originalAuth is HotAuth.Biometry,
        )

        runCatchingWrongPassInternal(
            hotWalletId = hotWalletId,
            originalAuth = originalAuth,
            auth = passResult,
            block = block,
        )
    }

    private suspend fun requestPassword(hotWalletId: HotWalletId, hasBiometry: Boolean): HotAuth {
        val attemptRequest = HotWalletPasswordRequester.AttemptRequest(
            hotWalletId = hotWalletId,
            authMode = false,
            hasBiometry = hasBiometry,
        )

        return hotWalletPasswordRequester.requestPassword(attemptRequest).toAuth()
            ?: throw TangemSdkError.UserCancelled()
    }

    private fun Throwable.isBiometryError(): Boolean {
        return this is TangemSdkError.AuthenticationFailed ||
            this is TangemSdkError.AuthenticationCanceled ||
            this is TangemSdkError.AuthenticationLockout ||
            this is TangemSdkError.AuthenticationUnavailable ||
            this is TangemSdkError.AuthenticationAlreadyInProgress ||
            this is TangemSdkError.AuthenticationNotInitialized ||
            this is TangemSdkError.AuthenticationPermanentLockout
    }

    private fun HotWalletPasswordRequester.Result.toAuth() = when (this) {
        HotWalletPasswordRequester.Result.UseBiometry -> HotAuth.Biometry
        HotWalletPasswordRequester.Result.Dismiss -> null
        is HotWalletPasswordRequester.Result.EnteredPassword -> this.password
    }
}