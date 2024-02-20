package com.tangem.domain.txhistory.models

sealed class Page {
    object Initial : Page()
    data class Next(val value: String) : Page()
    object LastPage : Page()
}