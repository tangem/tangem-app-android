package com.tangem.data.walletconnect.utils

import com.reown.android.Core
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.utils.converter.Converter

internal object WcAppMetaDataConverter : Converter<WcAppMetaDataConverter.Input, WcAppMetaData> {

    val empty
        get() = WcAppMetaData(
            name = "",
            description = "",
            url = "",
            icons = emptyList(),
            redirect = null,
            appLink = null,
            isLinkMode = false,
            verifyUrl = null,
        )

    override fun convert(value: Input): WcAppMetaData {
        return WcAppMetaData(
            name = value.peerMetaData.name,
            description = value.peerMetaData.description,
            url = value.originUrl,
            icons = value.peerMetaData.icons,
            redirect = value.peerMetaData.redirect,
            appLink = value.peerMetaData.appLink,
            isLinkMode = value.peerMetaData.linkMode,
            verifyUrl = value.peerMetaData.verifyUrl,
        )
    }

    internal data class Input(val originUrl: String, val peerMetaData: Core.Model.AppMetaData)
}