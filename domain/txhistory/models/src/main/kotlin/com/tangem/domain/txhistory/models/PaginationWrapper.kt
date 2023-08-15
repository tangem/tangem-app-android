package com.tangem.domain.txhistory.models

data class PaginationWrapper<T>(
    val page: Int,
    val totalPages: Int,
    val itemsOnPage: Int,
    val items: List<T>,
)
