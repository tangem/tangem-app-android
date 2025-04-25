package com.tangem.data.walletconnect.utils

import com.reown.android.Core
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.utils.converter.Converter

internal object WcAppMetaDataConverter : Converter<Core.Model.AppMetaData, WcAppMetaData> {

    val empty
        get() = WcAppMetaData(
            name = "",
            description = "",
            url = "",
            icons = listOf(),
            redirect = null,
            appLink = null,
            linkMode = false,
            verifyUrl = null,
        )

    override fun convert(value: Core.Model.AppMetaData): WcAppMetaData {
        return WcAppMetaData(
            name = value.name,
            description = value.description,
            url = value.url,
            icons = value.icons,
            redirect = value.redirect,
            appLink = value.appLink,
            linkMode = value.linkMode,
            verifyUrl = value.verifyUrl,
        )
    }
}