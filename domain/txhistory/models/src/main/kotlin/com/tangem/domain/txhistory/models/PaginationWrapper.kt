package com.tangem.domain.txhistory.models

data class PaginationWrapper<T>(
    val currentPage: Page,
    val nextPage: Page,
    val items: List<T>,
)