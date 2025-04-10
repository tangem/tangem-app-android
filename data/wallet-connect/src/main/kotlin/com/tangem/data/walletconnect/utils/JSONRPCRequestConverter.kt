package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest.JSONRPCRequest
import com.tangem.utils.converter.Converter

internal object JSONRPCRequestConverter : Converter<Wallet.Model.SessionRequest.JSONRPCRequest, JSONRPCRequest> {
    override fun convert(value: Wallet.Model.SessionRequest.JSONRPCRequest): JSONRPCRequest {
        return JSONRPCRequest(
            id = value.id,
            method = value.method,
            params = value.params,
        )
    }
}