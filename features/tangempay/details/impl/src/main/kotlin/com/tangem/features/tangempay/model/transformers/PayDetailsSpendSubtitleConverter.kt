package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.converter.Converter

internal object PayDetailsSpendSubtitleConverter : Converter<TangemPayTxHistoryItem.Spend, TextReference> {

    override fun convert(value: TangemPayTxHistoryItem.Spend): TextReference {
        val merchantCategory = value.merchantCategory
        val enrichedMerchantCategory = value.enrichedMerchantCategory
        val merchantCategoryCode = value.merchantCategoryCode

        val categoryCodeText = if (!merchantCategoryCode.isNullOrEmpty()) {
            resourceReference(
                id = R.string.tangem_pay_history_item_spend_mcc,
                formatArgs = wrappedList(merchantCategoryCode),
            )
        } else {
            null
        }

        val categoryText = when {
            // if merchantCategory isNotEmpty use this
            !merchantCategory.isNullOrEmpty() -> stringReference(merchantCategory)

            // If merchantCategory empty or null but enrichedMerchantCategory is not empty
            !enrichedMerchantCategory.isNullOrEmpty() -> stringReference(enrichedMerchantCategory)

            else -> resourceReference(R.string.tangem_pay_other)
        }

        return if (categoryCodeText != null) {
            resourceReference(
                id = R.string.tangem_pay_history_item_spend_mc_title_format,
                formatArgs = wrappedList(categoryText, categoryCodeText),
            )
        } else {
            categoryText
        }
    }
}