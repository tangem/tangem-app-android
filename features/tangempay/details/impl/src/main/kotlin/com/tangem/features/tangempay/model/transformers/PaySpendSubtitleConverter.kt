package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.converter.Converter

internal object PaySpendSubtitleConverter : Converter<TangemPayTxHistoryItem.Spend, TextReference> {

    override fun convert(value: TangemPayTxHistoryItem.Spend): TextReference {
        val merchantCategory = value.merchantCategory
        val enrichedMerchantCategory = value.enrichedMerchantCategory
        val merchantCategoryCode = value.merchantCategoryCode
        return when {
            // if merchantCategory isNotEmpty use this
            !merchantCategory.isNullOrEmpty() -> stringReference(merchantCategory)

            // If merchantCategory empty or null but enrichedMerchantCategory is not empty
            !enrichedMerchantCategory.isNullOrEmpty() -> stringReference(enrichedMerchantCategory)

            // If both empty/null but merchantCategoryCode is not empty
            !merchantCategoryCode.isNullOrEmpty() -> stringReference(merchantCategoryCode)

            // If all are empty/null, display "Other"
            else -> resourceReference(R.string.tangem_pay_other)
        }
    }
}