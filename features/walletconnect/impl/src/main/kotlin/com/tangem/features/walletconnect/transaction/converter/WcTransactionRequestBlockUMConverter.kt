package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import org.json.JSONArray
import javax.inject.Inject

private const val DATA_FIELD = "data"

internal class WcTransactionRequestBlockUMConverter @Inject constructor() :
    Converter<WcTransactionRequestBlockUMConverter.Input, WcTransactionRequestBlockUM> {

    override fun convert(value: Input): WcTransactionRequestBlockUM {
        val params = value.rawSdkRequest.request.params
        return WcTransactionRequestBlockUM(
            persistentListOf(
                WcTransactionRequestInfoItemUM(
                    title = resourceReference(R.string.wc_signature_type),
                    description = value.rawSdkRequest.request.method,
                ),
                WcTransactionRequestInfoItemUM(
                    title = resourceReference(R.string.wc_contents),
                    description = value.signModel?.humanMsg ?: params.tryExtractData() ?: params,
                ),
            ),
        )
    }

    private fun String.tryExtractData() = try {
        val array = JSONArray(this)
        if (array.length() > 0 && array.getJSONObject(0).has(DATA_FIELD)) {
            array.getJSONObject(0).getString(DATA_FIELD)
        } else {
            null
        }
    } catch (ignore: Exception) {
        null
    }

    data class Input(
        val rawSdkRequest: WcSdkSessionRequest,
        val signModel: WcMessageSignUseCase.SignModel? = null,
    )
}