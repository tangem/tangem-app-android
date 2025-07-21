package com.tangem.tap.domain.hot

import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.*
import javax.inject.Inject

class HotWalletAccessor @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val hotWalletPasswordRequester: HotWalletPasswordRequester,
) {

    suspend fun signHashes(hotWalletId: HotWalletId, dataToSign: List<DataToSign>): List<SignedData> {
        val auth = when (hotWalletId.authType) {
            HotWalletId.AuthType.NoPassword -> HotAuth.NoAuth
            HotWalletId.AuthType.Password -> {
                hotWalletPasswordRequester.requestPassword(hotWalletId)
            }
            HotWalletId.AuthType.Biometry -> HotAuth.Biometry
        }

        return runCatching {
            tangemHotSdk.signHashes(
                unlockHotWallet = UnlockHotWallet(
                    walletId = hotWalletId,
                    auth = auth,
                ),
                dataToSign = dataToSign,
            )
        }.getOrElse {
            if (hotWalletId.authType == HotWalletId.AuthType.Biometry) {
                val passwordAuth = hotWalletPasswordRequester.requestPassword(hotWalletId)
                tangemHotSdk.signHashes(
                    unlockHotWallet = UnlockHotWallet(
                        walletId = hotWalletId,
                        auth = passwordAuth,
                    ),
                    dataToSign = dataToSign,
                )
            } else {
                throw it
            }
        }
    }
}