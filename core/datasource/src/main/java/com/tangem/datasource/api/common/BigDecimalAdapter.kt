package com.tangem.datasource.api.common

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 15.11.2022.
 */
class BigDecimalAdapter {
    @FromJson
    fun fromJson(value: String) = BigDecimal(value)

    @ToJson
    fun toJson(value: BigDecimal) = value.toString()
}
