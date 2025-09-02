package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.utils.converter.Converter

internal object WcSdkSessionRequestConverter : Converter<WcSdkSessionRequestConverter.Input, WcSdkSessionRequest> {

    override fun convert(value: Input): WcSdkSessionRequest {
        return WcSdkSessionRequest(
            topic = value.sessionRequest.topic,
            chainId = value.sessionRequest.chainId,
            dAppMetaData = value.sessionRequest.peerMetaData
                ?.let {
                    WcAppMetaDataConverter.convert(
                        value = WcAppMetaDataConverter.Input(
                            originUrl = value.originUrl,
                            peerMetaData = it,
                        ),
                    )
                }
                ?: WcAppMetaDataConverter.empty,
            request = JSONRPCRequestConverter.convert(value.sessionRequest.request),
        )
    }

    internal data class Input(val originUrl: String, val sessionRequest: Wallet.Model.SessionRequest)
}