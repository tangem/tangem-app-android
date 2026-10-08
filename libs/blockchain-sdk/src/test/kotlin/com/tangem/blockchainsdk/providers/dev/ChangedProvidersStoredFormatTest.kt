package com.tangem.blockchainsdk.providers.dev

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.tangem.blockchainsdk.providers.models.ProviderModel
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the stored format of the providers changed in the Tester Menu: files written by the Moshi serializer must
 * read back unchanged after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer that
 * is still used while the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChangedProvidersStoredFormatTest {

    /** The production Moshi tags providers by `"type"`, as registered in `BlockchainSDKFactoryModule`. */
    private val moshi: Moshi = legacyDataStoreMoshi.newBuilder()
        .add(
            PolymorphicJsonAdapterFactory.of(ProviderModel::class.java, "type")
                .withSubtype(ProviderModel.Public::class.java, "public")
                .withSubtype(ProviderModel.Private::class.java, "private")
                .withDefaultValue(ProviderModel.UnsupportedType),
        )
        .build()

    private val serializer = MapSerializer(String.serializer(), ListSerializer(ProviderModelSerializer))

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN providers WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = moshi.adapter<Map<String, List<ProviderModel>>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN providers are kept`(model: FormatModel) {
        // Act
        val actual = moshi.adapter<Map<String, List<ProviderModel>>>().fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN providers are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = emptyMap(), serializer = serializer)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN providers WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyMap(), serializer = serializer)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @Test
    fun `GIVEN provider with an unknown type WHEN read from DataStore THEN it reads as unsupported`() {
        // Arrange
        val json = """{"ethereum":[{"type":"grpc","host":"node"}]}"""

        // Act
        val actual = readStoredJson(json = json, defaultValue = emptyMap(), serializer = serializer)

        // Assert
        assertThat(actual).isEqualTo(mapOf("ethereum" to listOf(ProviderModel.UnsupportedType)))
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = mapOf(
                "ethereum" to listOf(
                    ProviderModel.Private(name = "nownodes"),
                    ProviderModel.Public(url = "https://rpc"),
                ),
                "solana" to listOf(ProviderModel.Public(url = "https://solana")),
            ),
            json = """{"ethereum":[{"type":"private","name":"nownodes"},{"type":"public","url":"https://rpc"}],""" +
                """"solana":[{"type":"public","url":"https://solana"}]}""",
        ),
        FormatModel(value = mapOf("bitcoin" to emptyList()), json = """{"bitcoin":[]}"""),
    )

    data class FormatModel(val value: Map<String, List<ProviderModel>>, val json: String)
}