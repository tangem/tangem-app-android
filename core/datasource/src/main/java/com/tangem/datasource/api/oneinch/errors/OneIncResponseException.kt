package com.tangem.datasource.api.oneinch.errors

import com.tangem.datasource.api.oneinch.models.SwapErrorDto

class OneIncResponseException(val data: SwapErrorDto) : Exception()
