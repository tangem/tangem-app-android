package com.tangem.features.promobanners.impl.campaigns.service

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCampaignsService @Inject constructor() : CampaignsService {

    private val _campaignFlow: Channel<String> = Channel(Channel.BUFFERED)
    override val campaignFlow: Flow<String> = _campaignFlow.receiveAsFlow()

    override fun show(campaignId: String) {
        _campaignFlow.trySend(campaignId)
    }
}