package com.tangem.datasource.api.common.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.math.BigInteger

internal class BigIntegerAdapter {
    @FromJson
    fun fromJson(value: String) = BigInteger(value)

    @ToJson
    fun toJson(value: BigInteger) = value.toString()
}