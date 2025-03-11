package com.tangem.domain.walletmanager.utils

import com.tangem.domain.txhistory.models.Page
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.blockchain.common.pagination.Page as SdkPage

class SdkPageConverter : TwoWayConverter<SdkPage, Page> {
    override fun convert(value: SdkPage): Page {
        return when (value) {
            is SdkPage.Initial -> Page.Initial
            is SdkPage.LastPage -> Page.LastPage
            is SdkPage.Next -> Page.Next(value = value.value)
        }
    }

    override fun convertBack(value: Page): SdkPage {
        return when (value) {
            is Page.Initial -> SdkPage.Initial
            is Page.LastPage -> SdkPage.LastPage
            is Page.Next -> SdkPage.Next(value = value.value)
        }
    }
}