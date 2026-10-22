package com.tangem.domain.walletconnect.model

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class WcEthTransactionParamsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DeclaredChainId {

        private val adapter = Moshi.Builder().build().adapter<List<WcEthTransactionParams>>(
            Types.newParameterizedType(List::class.java, WcEthTransactionParams::class.java),
        )

        @ParameterizedTest
        @ProvideTestModels
        fun declaredChainId(model: DeclaredChainIdModel) {
            // Act
            val actual = transaction(chainId = model.chainId).declaredChainId

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN chainId as a JSON string WHEN parsed THEN chain is read`() {
            // Act
            val actual = adapter.fromJson("""[{"from": "0xSender", "to": "0xTo", "chainId": "0x89"}]""")

            // Assert
            assertThat(actual?.single()?.declaredChainId).isEqualTo(POLYGON_CHAIN_ID)
        }

        @Test
        fun `GIVEN chainId as a JSON number WHEN parsed THEN chain is read`() {
            // Act: dApps send the chain ID unquoted too, and Moshi reads the numeric literal into the String field.
            val actual = adapter.fromJson("""[{"from": "0xSender", "to": "0xTo", "chainId": 137}]""")

            // Assert
            assertThat(actual?.single()?.declaredChainId).isEqualTo(POLYGON_CHAIN_ID)
        }

        @Test
        fun `GIVEN no chainId WHEN parsed THEN no chain is declared`() {
            // Act
            val actual = adapter.fromJson("""[{"from": "0xSender", "to": "0xTo"}]""")

            // Assert
            assertThat(actual?.single()?.declaredChainId).isNull()
        }

        private fun provideTestModels() = listOf(
            DeclaredChainIdModel(chainId = "0x89", expected = POLYGON_CHAIN_ID),
            DeclaredChainIdModel(chainId = "0x1", expected = 1),
            DeclaredChainIdModel(chainId = "137", expected = POLYGON_CHAIN_ID),
            DeclaredChainIdModel(chainId = null, expected = null),
            DeclaredChainIdModel(chainId = "", expected = null),
            DeclaredChainIdModel(chainId = "0x", expected = null),
            DeclaredChainIdModel(chainId = "not-a-number", expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HasCallData {

        @ParameterizedTest
        @ProvideTestModels
        fun hasCallData(model: HasCallDataModel) {
            // Act
            val actual = transaction(data = model.data).hasCallData

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            HasCallDataModel(data = null, expected = false),
            HasCallDataModel(data = "", expected = false),
            HasCallDataModel(data = "0x", expected = false),
            HasCallDataModel(data = "0xa9059cbb", expected = true),
            HasCallDataModel(data = "a9059cbb", expected = true),
        )
    }

    internal data class DeclaredChainIdModel(val chainId: String?, val expected: Int?)

    internal data class HasCallDataModel(val data: String?, val expected: Boolean)

    private fun transaction(chainId: String? = null, data: String? = null) = WcEthTransactionParams(
        from = "0xSender",
        to = "0xRecipient",
        data = data,
        gas = null,
        gasPrice = null,
        value = null,
        nonce = null,
        chainId = chainId,
    )

    private companion object {
        const val POLYGON_CHAIN_ID = 137
    }
}