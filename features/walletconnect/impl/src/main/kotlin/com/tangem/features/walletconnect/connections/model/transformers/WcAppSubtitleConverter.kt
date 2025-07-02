package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.utils.converter.Converter
import java.net.URI

internal object WcAppSubtitleConverter : Converter<WcAppMetaData, String> {
    override fun convert(value: WcAppMetaData): String {
        val uri = URI(value.url.trim())
        return uri.host
    }
}