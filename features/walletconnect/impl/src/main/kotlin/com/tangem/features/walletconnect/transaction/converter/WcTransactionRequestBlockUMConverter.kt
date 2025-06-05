package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

internal class WcTransactionRequestBlockUMConverter @Inject constructor() :
    Converter<WcTransactionRequestBlockUMConverter.Input, WcTransactionRequestBlockUM> {

    override fun convert(value: Input) = WcTransactionRequestBlockUM(
        persistentListOf(
            WcTransactionRequestInfoItemUM(
                title = resourceReference(R.string.wc_signature_type),
                description = value.rawSdkRequest.request.method,
            ),
            WcTransactionRequestInfoItemUM(
                title = resourceReference(R.string.wc_contents),
                description = value.signModel?.humanMsg ?: value.rawSdkRequest.request.params,
            ),
        ),
    )

    data class Input(
        val rawSdkRequest: WcSdkSessionRequest,
        val signModel: WcMessageSignUseCase.SignModel? = null,
    )
}