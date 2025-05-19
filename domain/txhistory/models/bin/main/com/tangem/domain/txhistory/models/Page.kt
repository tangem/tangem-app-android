package com.tangem.domain.txhistory.models

sealed class Page {
    data object Initial : Page()
    data class Next(val value: String) : Page()
    data object LastPage : Page()
}