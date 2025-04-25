package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.utils.converter.Converter

internal object WcSdkSessionConverter : Converter<Wallet.Model.Session, WcSdkSession> {

    override fun convert(value: Wallet.Model.Session): WcSdkSession {
        return WcSdkSession(
            topic = value.topic,
            appMetaData = value.metaData?.let { WcAppMetaDataConverter.convert(it) } ?: WcAppMetaDataConverter.empty,
        )
    }
}