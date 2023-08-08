package com.tangem.lib.crypto.models

data class ProxyPaginationWrapper<T>(
    val page: Int,
    val totalPages: Int,
    val itemsOnPage: Int,
    val items: List<T>,
)
