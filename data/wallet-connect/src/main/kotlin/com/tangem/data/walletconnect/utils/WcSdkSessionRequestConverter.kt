package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.utils.converter.Converter

internal object WcSdkSessionRequestConverter : Converter<Wallet.Model.SessionRequest, WcSdkSessionRequest> {

    override fun convert(value: Wallet.Model.SessionRequest): WcSdkSessionRequest {
        return WcSdkSessionRequest(
            topic = value.topic,
            chainId = value.chainId,
            request = JSONRPCRequestConverter.convert(value.request),
        )
    }
}