package com.tangem.data.tangempay.util

import com.tangem.spend.datasource.tangempay.models.response.CashbackAccrualDocsResponse
import com.tangem.domain.tangempay.model.CashbackDocument
import com.tangem.utils.converter.Converter

internal object CashbackAccrualDocsConverter : Converter<CashbackAccrualDocsResponse, List<CashbackDocument>> {

    override fun convert(value: CashbackAccrualDocsResponse): List<CashbackDocument> {
        return value.result?.docs.orEmpty().mapNotNull(::convertDoc)
    }

    private fun convertDoc(doc: CashbackAccrualDocsResponse.Doc): CashbackDocument? {
        return CashbackDocument(
            id = doc.id ?: return null,
            title = doc.title ?: return null,
            url = doc.url ?: return null,
        )
    }
}