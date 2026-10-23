package com.tangem.data.collectibles.error

import com.tangem.core.remote.json.NetworkJson
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.domain.collectibles.models.CollectibleException
import com.tangem.grow.datasource.collectibles.models.response.CollectiblesErrorResponse
import com.tangem.utils.logging.TangemLogger
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Reads the collectibles backend's own error code from the body: the HTTP status alone does not tell them apart. */
internal class CollectiblesErrorResolver @Inject constructor(@NetworkJson private val json: Json) {

    fun <T : Any> getOrThrow(response: ApiResponse<T>): T {
        val error = (response as? ApiResponse.Error)?.cause as? ApiResponseError.HttpException
            ?: return response.getOrThrow()
        val body = error.errorBody?.let { body ->
            runCatching { json.decodeFromString<CollectiblesErrorResponse>(body) }.getOrNull()
        }

        TangemLogger.e("Collectibles backend error: code=${body?.code}, detail=${body?.detail}", error)

        throw if (body?.code == LOW_INVENTORY_CODE) CollectibleException.LowInventory() else error
    }

    private companion object {

        const val LOW_INVENTORY_CODE = 3016
    }
}