package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber
import javax.inject.Inject

internal class WcTransactionRequestBlockUMConverter @Inject constructor(
    private val transactionParamsConverter: TransactionParamsConverter,
) : Converter<WcTransactionRequestBlockUMConverter.Input, List<WcTransactionRequestBlockUM>> {

    override fun convert(value: Input): List<WcTransactionRequestBlockUM> {
        val params = value.rawSdkRequest.request.params
        val humanMsg = value.signModel?.humanMsg
        return buildList {
            add(
                WcTransactionRequestBlockUM(
                    buildList {
                        add(
                            WcTransactionRequestInfoItemUM(
                                title = resourceReference(R.string.wc_signature_type),
                                description = value.rawSdkRequest.request.method,
                            ),
                        )
                        if (!humanMsg.isNullOrEmpty()) {
                            add(
                                WcTransactionRequestInfoItemUM(
                                    title = resourceReference(R.string.wc_contents),
                                    description = humanMsg,
                                ),
                            )
                        }
                    }.toImmutableList(),
                ),
            )
            try {
                addAll(transactionParamsConverter.convert(params))
            } catch (exception: Exception) {
                Timber.e(exception, "Error while parsing transaction params - %s", params)
            }
        }
    }

    data class Input(
        val rawSdkRequest: WcSdkSessionRequest,
        val signModel: WcMessageSignUseCase.SignModel? = null,
    )
}