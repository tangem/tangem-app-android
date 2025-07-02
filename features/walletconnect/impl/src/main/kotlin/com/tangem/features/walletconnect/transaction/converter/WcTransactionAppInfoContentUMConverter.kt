package com.tangem.features.walletconnect.transaction.converter

import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.features.walletconnect.connections.model.transformers.WcAppSubtitleConverter
import com.tangem.features.walletconnect.connections.model.transformers.WcDAppVerifiedStateConverter
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class WcTransactionAppInfoContentUMConverter @Inject constructor() :
    Converter<WcTransactionAppInfoContentUMConverter.Input, WcTransactionAppInfoContentUM> {

    override fun convert(value: Input) = WcTransactionAppInfoContentUM(
        appName = value.session.sdkModel.appMetaData.name,
        appIcon = value.session.sdkModel.appMetaData.icons.firstOrNull().orEmpty(),
        verifiedState = WcDAppVerifiedStateConverter(onVerifiedClick = value.onShowVerifiedAlert).convert(
            value.session.securityStatus to value.session.sdkModel.appMetaData.name,
        ),
        appSubtitle = WcAppSubtitleConverter.convert(value.session.sdkModel.appMetaData),
    )

    data class Input(
        val session: WcSession,
        val onShowVerifiedAlert: (String) -> Unit,
    )
}