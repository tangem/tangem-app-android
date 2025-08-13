package com.tangem.features.walletconnect.transaction.converter

import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.features.walletconnect.connections.routing.WcInnerRoute
import com.tangem.utils.converter.Converter

internal object WcHandleMethodErrorConverter :
    Converter<HandleMethodError, WcInnerRoute> {

    override fun convert(value: HandleMethodError): WcInnerRoute = when (value) {
        is HandleMethodError.Unsupported,
        is HandleMethodError.UnknownError,
        -> WcInnerRoute.UnsupportedMethodAlert
        HandleMethodError.UnknownSession -> WcInnerRoute.WcDappDisconnected
    }
}