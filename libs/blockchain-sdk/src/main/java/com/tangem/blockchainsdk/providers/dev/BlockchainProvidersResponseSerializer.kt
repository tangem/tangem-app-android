package com.tangem.blockchainsdk.providers.dev

import androidx.datastore.core.Serializer
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.blockchainsdk.providers.models.ProviderModel
import com.tangem.utils.annotations.RemoveWithToggle
import java.io.InputStream
import java.io.OutputStream

/** DataStore serializer for [BlockchainProvidersResponse]. Implemented by [Moshi]. */
@RemoveWithToggle(
    toggleName = "AND_18288_KOTLINX_DATASTORE_ENABLED",
    description = "Only the toggle-off branch of the changed providers store uses it",
)
internal class BlockchainProvidersResponseSerializer(moshi: Moshi) : Serializer<BlockchainProvidersResponse> {

    private val adapter by lazy {
        val providersType = Types.newParameterizedType(List::class.java, ProviderModel::class.java)
        val type = Types.newParameterizedType(Map::class.java, String::class.java, providersType)

        moshi.adapter<BlockchainProvidersResponse>(type)
    }

    override val defaultValue: BlockchainProvidersResponse = emptyMap()

    override suspend fun readFrom(input: InputStream): BlockchainProvidersResponse {
        return input.bufferedReader().use { reader ->
            adapter.fromJson(reader.readText()) ?: defaultValue
        }
    }

    override suspend fun writeTo(t: BlockchainProvidersResponse, output: OutputStream) {
        output.bufferedWriter().use { writer ->
            writer.write(adapter.toJson(t))
        }
    }
}