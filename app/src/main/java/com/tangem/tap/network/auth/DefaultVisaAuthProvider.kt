package com.tangem.tap.network.auth

import com.tangem.datasource.api.common.visa.TangemVisaAuthProvider
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import javax.inject.Inject

internal class DefaultVisaAuthProvider @Inject constructor(
    private val authStorage: VisaAuthTokenStorage,
) : TangemVisaAuthProvider {

    override suspend fun getAuthHeader(cardId: String): String {
        return authStorage.get(cardId)?.accessToken?.let { "Bearer $it" } ?: "Error in the app!"
    }
}