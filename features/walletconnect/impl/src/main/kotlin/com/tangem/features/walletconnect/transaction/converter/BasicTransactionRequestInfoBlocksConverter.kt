package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.usecase.method.WcMethodContext
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

internal class BasicTransactionRequestInfoBlocksConverter @Inject constructor() :
    Converter<WcMethodContext, ImmutableList<WcTransactionRequestBlockUM>> {

    override fun convert(value: WcMethodContext) = persistentListOf(
        WcTransactionRequestBlockUM(
            persistentListOf(
                WcTransactionRequestInfoItemUM(
                    title = resourceReference(R.string.wc_signature_type),
                    description = value.rawSdkRequest.request.method,
                ),
                WcTransactionRequestInfoItemUM(
                    title = resourceReference(R.string.wc_contents),
                    description = value.rawSdkRequest.request.params,
                ),
            ),
        ),
    )
}