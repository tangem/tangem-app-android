package com.tangem.tap.domain.hot

import com.tangem.common.core.TangemSdkError
import com.tangem.features.hotwallet.HotWalletPasswordRequester
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.exception.WrongPasswordException
import com.tangem.hot.sdk.model.*
import javax.inject.Inject

class HotWalletAccessor @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val hotWalletPasswordRequester: HotWalletPasswordRequester,
) {

    suspend fun signHashes(hotWalletId: HotWalletId, dataToSign: List<DataToSign>): List<SignedData> {
        val auth = when (hotWalletId.authType) {
            HotWalletId.AuthType.NoPassword -> HotAuth.NoAuth
            HotWalletId.AuthType.Password -> requestPassword(false)
            HotWalletId.AuthType.Biometry -> HotAuth.Biometry
        }

        return runCatchingSdkErrors(hotWalletId, auth) {
            tangemHotSdk.signHashes(
                unlockHotWallet = UnlockHotWallet(
                    walletId = hotWalletId,
                    auth = it,
                ),
                dataToSign = dataToSign,
            ).also {
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
            originalAuth = auth,
            auth = auth,
            block = { blockAuth ->
                block(blockAuth).also {
                    // TODO [REDACTED_TASK_KEY] if user has biometry enabled, we set it as the new auth method
                    if (blockAuth is HotAuth.Password /*&& has biometry enabled */) {
                        tangemHotSdk.changeAuth(
                            unlockHotWallet = UnlockHotWallet(
                                walletId = hotWalletId,
                                auth = blockAuth,
                            ),
                            auth = HotAuth.Biometry,
                        )
                    }
                }
            },
        )
    }

    private suspend fun <T> runCatchingWrongPassInternal(
        originalAuth: HotAuth,
        auth: HotAuth,
        block: suspend (auth: HotAuth) -> T,
    ): T = runCatching {
        block(auth)
    }.getOrElse { exception ->
        if (auth is HotAuth.Biometry && exception.isBiometryError()) {
            // fallback to password if biometry fails
            val passAuth = requestPassword(true)

            return@getOrElse runCatchingWrongPassInternal(
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
        val passResult = requestPassword(originalAuth is HotAuth.Biometry)

        runCatchingWrongPassInternal(
            originalAuth = originalAuth,
            auth = passResult,
            block = block,
        )
    }

    private suspend fun requestPassword(hasBiometry: Boolean): HotAuth {
        return hotWalletPasswordRequester.requestPassword(hasBiometry).toAuth() ?: throw TangemSdkError.UserCancelled()
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