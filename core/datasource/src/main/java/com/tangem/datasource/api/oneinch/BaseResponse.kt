package com.tangem.datasource.api.oneinch

import com.tangem.datasource.api.oneinch.models.SwapErrorDto

data class BaseOneInchResponse<T>(
    val body: T?,
    val errorDto: SwapErrorDto?,
)
