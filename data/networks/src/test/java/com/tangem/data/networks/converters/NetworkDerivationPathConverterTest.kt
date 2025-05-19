package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.network.Network
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
[REDACTED_AUTHOR]
 */
class NetworkDerivationPathConverterTest {

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class Convert {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun convert(model: ConvertModel) {
            // Act
            val actual = NetworkDerivationPathConverter.convert(value = model.value)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): Collection<ConvertModel> = listOf(
            ConvertModel(
                value = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
                expected = Network.DerivationPath.Card("card"),
            ),
            ConvertModel(
                value = NetworkStatusDM.DerivationPath(
                    value = "custom",
                    type = NetworkStatusDM.DerivationPath.Type.CUSTOM,
                ),
                expected = Network.DerivationPath.Custom("custom"),
            ),
            ConvertModel(
                value = NetworkStatusDM.DerivationPath(
                    value = "",
                    type = NetworkStatusDM.DerivationPath.Type.NONE,
                ),
                expected = Network.DerivationPath.None,
            ),
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class ConvertBack {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun convertBack(model: ConvertBackModel) {
            // Act
            val actual = NetworkDerivationPathConverter.convertBack(value = model.value)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): Collection<ConvertBackModel> = listOf(
            ConvertBackModel(
                value = Network.DerivationPath.Card("card"),
                expected = NetworkStatusDM.DerivationPath(
                    value = "card",
                    type = NetworkStatusDM.DerivationPath.Type.CARD,
                ),
            ),
            ConvertBackModel(
                value = Network.DerivationPath.Custom("custom"),
                expected = NetworkStatusDM.DerivationPath(
                    value = "custom",
                    type = NetworkStatusDM.DerivationPath.Type.CUSTOM,
                ),
            ),
            ConvertBackModel(
                value = Network.DerivationPath.None,
                expected = NetworkStatusDM.DerivationPath(
                    value = "",
                    type = NetworkStatusDM.DerivationPath.Type.NONE,
                ),
            ),
        )
    }

    data class ConvertModel(
        val value: NetworkStatusDM.DerivationPath,
        val expected: Network.DerivationPath,
    )

    data class ConvertBackModel(
        val value: Network.DerivationPath,
        val expected: NetworkStatusDM.DerivationPath,
    )
}