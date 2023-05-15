package com.tangem.feature.referral.data

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.StartReferralBody
import com.tangem.datasource.demo.DemoModeDatasource
import com.tangem.feature.referral.converters.ReferralConverter
import com.tangem.feature.referral.domain.ReferralRepository
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.lib.auth.AuthProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class ReferralRepositoryImpl @Inject constructor(
    private val referralApi: TangemTechApi,
    private val referralConverter: ReferralConverter,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val demoModeDatasource: DemoModeDatasource,
    private val authProvider: AuthProvider,
) : ReferralRepository {

    override val isDemoMode: Boolean
        get() = demoModeDatasource.isDemoModeActive

    override suspend fun getReferralStatus(walletId: String): ReferralData {
        return withContext(coroutineDispatcher.io) {
            referralConverter.convert(
                referralApi.getReferralStatus(
                    cardPublicKey = authProvider.getCardPublicKey(),
                    cardId = authProvider.getCardId(),
                    walletId = walletId,
                ),
            )
        }
    }

    override suspend fun startReferral(
        walletId: String,
        networkId: String,
        tokenId: String,
        address: String,
    ): ReferralData {
        return withContext(coroutineDispatcher.io) {
            referralConverter.convert(
                referralApi.startReferral(
                    cardPublicKey = authProvider.getCardPublicKey(),
                    cardId = authProvider.getCardId(),
                    startReferralBody = StartReferralBody(
                        walletId = walletId,
                        networkId = networkId,
                        tokenId = tokenId,
                        address = address,
                    ),
                ),
            )
        }
    }
}