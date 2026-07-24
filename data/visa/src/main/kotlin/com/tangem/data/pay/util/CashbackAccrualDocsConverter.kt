package com.tangem.data.pay.util

import com.tangem.datasource.api.pay.models.response.CashbackAccrualDocsResponse
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.utils.converter.Converter

/** Maps [CashbackAccrualDocsResponse] (BFF) to domain [CashbackDocument]s, dropping malformed entries. */
internal object CashbackAccrualDocsConverter : Converter<CashbackAccrualDocsResponse, List<CashbackDocument>> {

    override fun convert(value: CashbackAccrualDocsResponse): List<CashbackDocument> {
        return value.docs.orEmpty().mapNotNull(::convertDoc)
    }

    private fun convertDoc(doc: CashbackAccrualDocsResponse.Doc): CashbackDocument? {
        return CashbackDocument(
            id = doc.id ?: return null,
            title = doc.title ?: return null,
            url = doc.url ?: return null,
        )
    }
}