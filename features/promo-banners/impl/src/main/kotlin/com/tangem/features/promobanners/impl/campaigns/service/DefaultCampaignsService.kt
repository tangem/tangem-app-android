package com.tangem.features.promobanners.impl.campaigns.service

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCampaignsService @Inject constructor() : CampaignsService {

    private val _campaignFlow: Channel<CampaignRequest> = Channel(Channel.BUFFERED)
    override val campaignFlow: Flow<CampaignRequest> = _campaignFlow.receiveAsFlow()

    override fun show(campaignId: String, userWalletId: UserWalletId) {
        _campaignFlow.trySend(CampaignRequest(campaignId = campaignId, userWalletId = userWalletId))
    }
}