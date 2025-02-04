package com.tangem.tap.network.auth

import com.tangem.datasource.api.common.visa.TangemVisaAuthProvider
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import javax.inject.Inject

internal class DefaultVisaAuthProvider @Inject constructor(
    private val userWalletsListManager: UserWalletsListManager,
) : TangemVisaAuthProvider {

    override suspend fun getAuthHeader(cardId: String): String {
        val card = userWalletsListManager.userWalletsSync.firstOrNull { it.cardId == cardId }
        val status = card?.scanResponse?.visaCardActivationStatus as? VisaCardActivationStatus.Activated
            ?: return "Error in the app!"
        val accessToken = status.visaAuthTokens.accessToken

        return "Bearer $accessToken"
    }
}