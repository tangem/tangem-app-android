package com.tangem.datasource.local.network.utils

import androidx.datastore.core.Serializer
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.datasource.local.network.entity.NetworkStatusesDM
import java.io.InputStream
import java.io.OutputStream

internal class NetworkStatusesSerializer(moshi: Moshi) : Serializer<NetworkStatusesDM> {

    private val adapter by lazy {
        val type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Types.newParameterizedType(Set::class.java, NetworkStatusDM::class.java),
        )

        moshi.adapter<NetworkStatusesDM>(type)
    }

    override val defaultValue: NetworkStatusesDM = emptyMap()

    override suspend fun readFrom(input: InputStream): NetworkStatusesDM {
        return input.bufferedReader().use { reader ->
            adapter.fromJson(reader.readText()) ?: defaultValue
        }
    }

    override suspend fun writeTo(t: NetworkStatusesDM, output: OutputStream) {
        output.bufferedWriter().use { writer ->
            writer.write(adapter.toJson(t))
        }
    }
}