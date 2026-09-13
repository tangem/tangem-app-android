package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CashbackAccrualDocsResponse
import com.tangem.domain.pay.model.CashbackDocument
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