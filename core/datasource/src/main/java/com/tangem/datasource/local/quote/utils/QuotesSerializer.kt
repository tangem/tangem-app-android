package com.tangem.datasource.local.quote.utils

import androidx.datastore.core.Serializer
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.datasource.local.quote.model.QuoteDM
import com.tangem.datasource.local.quote.model.QuotesDM
import java.io.InputStream
import java.io.OutputStream

internal class QuotesSerializer(moshi: Moshi) : Serializer<QuotesDM> {

    private val adapter by lazy {
        val types = Types.newParameterizedType(Set::class.java, QuoteDM::class.java)

        moshi.adapter<QuotesDM>(types)
    }

    override val defaultValue: QuotesDM = emptySet()

    override suspend fun readFrom(input: InputStream): QuotesDM {
        return input.bufferedReader().use { reader ->
            adapter.fromJson(reader.readText()) ?: defaultValue
        }
    }

    override suspend fun writeTo(t: QuotesDM, output: OutputStream) {
        output.bufferedWriter().use { write ->
            write.write(adapter.toJson(t))
        }
    }
}