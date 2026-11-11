package com.tangem.tests.e2e.utils

import android.content.Context
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.common.cache.etag.WalletAccountsETags
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.wallet.UserWalletId
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Clears a wallet's backend token list for the E2E add-network suite, so each run starts from an empty
 * portfolio. Test-only (androidTest APK); build it with [from], which pulls its collaborators from the
 * app's live Hilt graph via [Dependencies].
 */
class E2ePortfolioResetter(
    private val tangemTechApi: TangemTechApi,
    private val walletAccountsETags: WalletAccountsETags,
) {

    suspend fun clearTokens(walletId: String) {
        val userWalletId = UserWalletId(stringValue = walletId)

        // Both calls return ApiResponse.Error instead of throwing, so an unchecked failure would be read as a
        // successful reset — the add-network tests would then run against a wallet that still has the token,
        // passing without exercising the addition. Fail loudly instead.
        val accountsResponse = tangemTechApi.getWalletAccountsV2(walletId = walletId)
        check(accountsResponse is ApiResponse.Success) {
            "E2E reset: fetching wallet accounts failed: ${(accountsResponse as? ApiResponse.Error)?.cause}"
        }
        walletAccountsETags.store(userWalletId = userWalletId, apiResponse = accountsResponse)

        val saveResponse = tangemTechApi.saveTokens(
            userId = walletId,
            userTokens = UserTokensResponse(
                group = UserTokensResponse.GroupType.NONE,
                sort = UserTokensResponse.SortType.MANUAL,
                tokens = emptyList(),
                notifyStatus = false,
            ),
            eTag = walletAccountsETags.getSyncOrNull(userWalletId = userWalletId),
        )
        check(saveResponse is ApiResponse.Success) {
            "E2E reset: clearing wallet tokens failed: ${(saveResponse as? ApiResponse.Error)?.cause}"
        }

        // Drop the accounts ETag. The reset runs before the app has fetched accounts, so it has no local
        // accounts document cached; leaving an ETag behind makes the app's next GET /accounts return 304
        // NOT_MODIFIED, and the fetcher then fails with "Saved accounts response is null" and the balance
        // never loads. Clearing it forces a full 200 fetch that repopulates the cache.
        walletAccountsETags.clear(userWalletId = userWalletId)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface Dependencies {
        fun tangemTechApi(): TangemTechApi
        fun walletAccountsETags(): WalletAccountsETags
    }

    companion object {
        fun from(context: Context): E2ePortfolioResetter {
            val deps = EntryPointAccessors.fromApplication(context, Dependencies::class.java)
            return E2ePortfolioResetter(
                tangemTechApi = deps.tangemTechApi(),
                walletAccountsETags = deps.walletAccountsETags(),
            )
        }
    }
}