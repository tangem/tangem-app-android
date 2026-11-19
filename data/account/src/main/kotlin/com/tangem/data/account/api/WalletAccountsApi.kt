package com.tangem.data.account.api

import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.common.account.SupportedAccountTypes
import com.tangem.datasource.api.common.response.map
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsV2Response
import com.tangem.datasource.api.tangemTech.models.account.GetWalletArchivedAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsV1Request
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import javax.inject.Inject

/**
 * The accounts endpoints in the version this build is allowed to speak.
 *
 * `v1` knows only the wallet's own accounts and no record type; `api/v2` carries the type of every record, knows
 * about joint ones and counts the wallet's own accounts as `totalCryptoAccounts`. [isV2Enabled] picks the version
 * here, in one place: with it off every call goes to `v1` with the body exactly as it was before joint accounts
 * existed, with it on — to `api/v2`, whose document is brought to the persisted `v1`-named shape on the way in.
 */
internal class WalletAccountsApi @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val supportedAccountTypes: SupportedAccountTypes,
) {

    val isV2Enabled: Boolean
        get() = supportedAccountTypes.types.any { it != WalletAccountDTO.Type.CRYPTO }

    suspend fun getAccounts(walletId: String, eTag: String?): ApiResponse<GetWalletAccountsResponse> {
        return if (isV2Enabled) {
            tangemTechApi.getWalletAccountsV2(
                walletId = walletId,
                eTag = eTag,
                supportedAccountTypes = supportedAccountTypes.query,
            )
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
        return if (isV2Enabled) {
            // The backend answers 400 to a row of a type the request does not list
            val supportedRows = body.accounts.filter { supportedAccountTypes.isSupported(it.type) }

            tangemTechApi.saveWalletAccountsV2(
                walletId = walletId,
                eTag = eTag,
                body = body.copy(accounts = supportedRows),
                supportedAccountTypes = supportedAccountTypes.query,
            )
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
        return if (isV2Enabled) {
            tangemTechApi.getWalletArchivedAccounts(
                version = JOINT_AWARE_VERSION,
                walletId = walletId,
                eTag = eTag,
                supportedAccountTypes = supportedAccountTypes.query,
            )
        } else {
            tangemTechApi.getWalletArchivedAccounts(version = LEGACY_VERSION, walletId = walletId, eTag = eTag)
        }
    }

    private companion object {
        const val LEGACY_VERSION = "v1"
        const val JOINT_AWARE_VERSION = "api/v2"
    }
}