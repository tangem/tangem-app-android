package com.tangem.data.polymarket.error

import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.grow.datasource.polymarket.models.ProblemDetailResponse
import javax.inject.Inject

internal class ProblemDetailParser @Inject constructor(
    @NetworkMoshi moshi: Moshi,
) {

    private val adapter = moshi.adapter(ProblemDetailResponse::class.java)

    /** @return `null` when [errorBody] is absent, unparsable or carries no `detail` */
    fun parseDetail(errorBody: String?): String? =
        errorBody?.let { body -> runCatching { adapter.fromJson(body)?.detail }.getOrNull() }
}