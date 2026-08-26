package com.tangem.data.account.api

import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletArchivedAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsV1Request
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import javax.inject.Inject

/**
 * The accounts endpoints in the version this build is allowed to speak.
 *
 * `v1` knows only the wallet's own accounts and no record type; `/api/v2` carries the type of every record and knows
 * about joint ones. The joint feature toggle picks the version here, in one place: with it off every call goes to
 * `v1` with the body exactly as it was before joint accounts existed, with it on — to `/api/v2`.
 *
 * The versions do not share a validator: [eTagKey] is versioned, because an `ETag` stored for one version would
 * answer `304 Not Modified` for the other and leave a document of the wrong shape looking fresh.
 */
internal class WalletAccountsApi @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
) {

    private val knowsJointAccounts: Boolean
        get() = jointAccountFeatureToggles.isJointAccountCreationEnabled

    val eTagKey: ETagsStore.Key
        get() = if (knowsJointAccounts) ETagsStore.Key.WalletAccountsV2 else ETagsStore.Key.WalletAccountsV1

    suspend fun getAccounts(walletId: String, eTag: String?): ApiResponse<GetWalletAccountsResponse> {
        return if (knowsJointAccounts) {
            tangemTechApi.getWalletAccountsV2(walletId = walletId, eTag = eTag)
        } else {
            tangemTechApi.getWalletAccountsV1(walletId = walletId, eTag = eTag)
        }
    }

    suspend fun saveAccounts(
        walletId: String,
        eTag: String,
        body: SaveWalletAccountsResponse,
    ): ApiResponse<GetWalletAccountsResponse> {
        return if (knowsJointAccounts) {
            tangemTechApi.saveWalletAccountsV2(walletId = walletId, eTag = eTag, body = body)
        } else {
            tangemTechApi.saveWalletAccountsV1(
                walletId = walletId,
                eTag = eTag,
                body = SaveWalletAccountsV1Request.of(body),
            )
        }
    }

    suspend fun getArchivedAccounts(walletId: String, eTag: String?): ApiResponse<GetWalletArchivedAccountsResponse> {
        return if (knowsJointAccounts) {
            tangemTechApi.getWalletArchivedAccountsV2(walletId = walletId, eTag = eTag)
        } else {
            tangemTechApi.getWalletArchivedAccountsV1(walletId = walletId, eTag = eTag)
        }
    }
}