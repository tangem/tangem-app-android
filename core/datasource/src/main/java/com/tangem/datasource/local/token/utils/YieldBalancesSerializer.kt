package com.tangem.datasource.local.token.utils

import androidx.datastore.core.Serializer
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.entity.YieldBalanceWrappersDTO
import java.io.InputStream
import java.io.OutputStream

internal class YieldBalancesSerializer(moshi: Moshi) : Serializer<YieldBalanceWrappersDTO> {

    private val adapter by lazy {
        val type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Types.newParameterizedType(Set::class.java, YieldBalanceWrapperDTO::class.java),
        )

        moshi.adapter<YieldBalanceWrappersDTO>(type)
    }

    override val defaultValue: YieldBalanceWrappersDTO = emptyMap()

    override suspend fun readFrom(input: InputStream): YieldBalanceWrappersDTO {
        return input.bufferedReader().use { reader ->
            adapter.fromJson(reader.readText()) ?: defaultValue
        }
    }

    override suspend fun writeTo(t: YieldBalanceWrappersDTO, output: OutputStream) {
        output.bufferedWriter().use { writer ->
            writer.write(adapter.toJson(t))
        }
    }
}