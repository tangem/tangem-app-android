package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSession
import com.tangem.utils.converter.Converter

internal object WcSdkSessionConverter : Converter<WcSdkSessionConverter.Input, WcSdkSession> {

    override fun convert(value: Input): WcSdkSession {
        return WcSdkSession(
            topic = value.session.topic,
            appMetaData = value.session.metaData
                ?.let {
                    WcAppMetaDataConverter.convert(
                        value = WcAppMetaDataConverter.Input(
                            originUrl = value.originUrl,
                            peerMetaData = it,
                        ),
                    )
                }
                ?: WcAppMetaDataConverter.empty,
            namespaces = value.session.namespaces.mapValues { (_, session) ->
                WcSdkSession.Session(
                    chains = session.chains ?: listOf(),
                    accounts = session.accounts,
                    methods = session.methods,
                    events = session.events,
                )
            },
        )
    }

    internal data class Input(val originUrl: String, val session: Wallet.Model.Session)
}