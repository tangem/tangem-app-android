package com.tangem.data.account.api

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.common.response.map
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsV2Response
import com.tangem.datasource.api.tangemTech.models.account.GetWalletArchivedAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsV1Request
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import javax.inject.Inject

/**
 * The accounts endpoints in the version this build is allowed to speak.
 *
 * `v1` knows only the wallet's own accounts and no record type; `api/v2` carries the type of every record, knows
 * about joint ones and counts the wallet's own accounts as `totalCryptoAccounts`. The joint feature toggle picks
 * the version here, in one place: with it off every call goes to `v1` with the body exactly as it was before joint
 * accounts existed, with it on — to `api/v2`, whose document is brought to the persisted `v1`-named shape on the
 * way in.
 */
internal class WalletAccountsApi @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
) {

    private val isJointAccountsEnabled: Boolean
        get() = jointAccountFeatureToggles.isJointAccountEnabled

    suspend fun getAccounts(walletId: String, eTag: String?): ApiResponse<GetWalletAccountsResponse> {
        return if (isJointAccountsEnabled) {
            tangemTechApi.getWalletAccountsV2(walletId = walletId, eTag = eTag)
                .map(GetWalletAccountsV2Response::toResponse)
        } else {
            tangemTechApi.getWalletAccountsV1(walletId = walletId, eTag = eTag)
        }
    }

    suspend fun saveAccounts(
        walletId: String,
        eTag: String,
        body: SaveWalletAccountsResponse,
    ): ApiResponse<GetWalletAccountsResponse> {
        return if (isJointAccountsEnabled) {
            tangemTechApi.saveWalletAccountsV2(walletId = walletId, eTag = eTag, body = body)
                .map(GetWalletAccountsV2Response::toResponse)
        } else {
            tangemTechApi.saveWalletAccountsV1(
                walletId = walletId,
                eTag = eTag,
                body = SaveWalletAccountsV1Request.of(body),
            )
        }
    }

    suspend fun getArchivedAccounts(walletId: String, eTag: String?): ApiResponse<GetWalletArchivedAccountsResponse> {
        val version = if (isJointAccountsEnabled) JOINT_AWARE_VERSION else LEGACY_VERSION

        return tangemTechApi.getWalletArchivedAccounts(version = version, walletId = walletId, eTag = eTag)
    }

    private companion object {
        const val LEGACY_VERSION = "v1"
        const val JOINT_AWARE_VERSION = "api/v2"
    }
}