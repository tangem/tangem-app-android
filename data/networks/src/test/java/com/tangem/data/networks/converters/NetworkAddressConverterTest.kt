package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.network.NetworkAddress
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
[REDACTED_AUTHOR]
 */
internal class NetworkAddressConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun convert(model: ConvertModel) {
            // Act
            val actual = runCatching { NetworkAddressConverter.convert(value = model.value) }

            // Assert
            actual
                .onSuccess {
                    Truth.assertThat(it).isEqualTo(model.expected.getOrNull())
                }
                .onFailure {
                    val expected = model.expected.exceptionOrNull()!!

                    Truth.assertThat(it).isInstanceOf(expected::class.java)
                    Truth.assertThat(it).hasMessageThat().isEqualTo(expected.message)
                }
        }

        private fun provideTestModels(): Collection<ConvertModel> = listOf(
            ConvertModel(
                value = NetworkAddressConverter.Value(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "0x1",
                            type = NetworkStatusDM.Address.Type.Primary,
                        ),
                    ),
                ),
                expected = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x1",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ).let(Result.Companion::success),
            ),
            ConvertModel(
                value = NetworkAddressConverter.Value(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "0x1",
                            type = NetworkStatusDM.Address.Type.Primary,
                        ),
                        NetworkStatusDM.Address(
                            value = "0x2",
                            type = NetworkStatusDM.Address.Type.Secondary,
                        ),
                    ),
                ),
                expected = NetworkAddress.Selectable(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x1",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                    availableAddresses = setOf(
                        NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                        NetworkAddress.Address(
                            value = "0x2",
                            type = NetworkAddress.Address.Type.Secondary,
                        ),
                    ),
                ).let(Result.Companion::success),
            ),
            ConvertModel(
                value = NetworkAddressConverter.Value(
                    selectedAddress = "",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "0x1",
                            type = NetworkStatusDM.Address.Type.Primary,
                        ),
                        NetworkStatusDM.Address(
                            value = "0x2",
                            type = NetworkStatusDM.Address.Type.Secondary,
                        ),
                    ),
                ),
                expected = IllegalArgumentException("Selected address must not be null").let(Result.Companion::failure),
            ),
            ConvertModel(
                value = NetworkAddressConverter.Value(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "",
                            type = NetworkStatusDM.Address.Type.Primary,
                        ),
                        NetworkStatusDM.Address(
                            value = "0x2",
                            type = NetworkStatusDM.Address.Type.Secondary,
                        ),
                    ),
                ),
                expected = IllegalArgumentException("Selected address must not be null").let(Result.Companion::failure),
            ),
            ConvertModel(
                value = NetworkAddressConverter.Value(
                    selectedAddress = "",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "",
                            type = NetworkStatusDM.Address.Type.Primary,
                        ),
                    ),
                ),
                expected = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ).let(Result.Companion::success),
            ),
            ConvertModel(
                value = NetworkAddressConverter.Value(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "0x1",
                            type = NetworkStatusDM.Address.Type.Secondary,
                        ),
                    ),
                ),
                expected = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x1",
                        type = NetworkAddress.Address.Type.Secondary,
                    ),
                ).let(Result.Companion::success),
            ),
            ConvertModel(
                value = NetworkAddressConverter.Value(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "0x1",
                            type = NetworkStatusDM.Address.Type.Secondary,
                        ),
                        NetworkStatusDM.Address(
                            value = "0x2",
                            type = NetworkStatusDM.Address.Type.Secondary,
                        ),
                    ),
                ),
                expected = NetworkAddress.Selectable(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x1",
                        type = NetworkAddress.Address.Type.Secondary,
                    ),
                    availableAddresses = setOf(
                        NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Secondary,
                        ),
                        NetworkAddress.Address(
                            value = "0x2",
                            type = NetworkAddress.Address.Type.Secondary,
                        ),
                    ),
                ).let(Result.Companion::success),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun convertBack(model: ConvertBackModel) {
            // Act
            val actual = NetworkAddressConverter.convertBack(value = model.value)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): Collection<ConvertBackModel> = listOf(
            ConvertBackModel(
                value = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x1",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                expected = NetworkAddressConverter.Value(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "0x1",
                            type = NetworkStatusDM.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
            ConvertBackModel(
                value = NetworkAddress.Selectable(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x1",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                    availableAddresses = setOf(
                        NetworkAddress.Address(
                            value = "0x1",
                            type = NetworkAddress.Address.Type.Primary,
                        ),
                        NetworkAddress.Address(
                            value = "0x2",
                            type = NetworkAddress.Address.Type.Secondary,
                        ),
                    ),
                ),
                expected = NetworkAddressConverter.Value(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "0x1",
                            type = NetworkStatusDM.Address.Type.Primary,
                        ),
                        NetworkStatusDM.Address(
                            value = "0x2",
                            type = NetworkStatusDM.Address.Type.Secondary,
                        ),
                    ),
                ),
            ),
            ConvertBackModel(
                value = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                expected = NetworkAddressConverter.Value(
                    selectedAddress = "",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "",
                            type = NetworkStatusDM.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
            ConvertBackModel(
                value = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Secondary,
                    ),
                ),
                expected = NetworkAddressConverter.Value(
                    selectedAddress = "",
                    addresses = setOf(
                        NetworkStatusDM.Address(
                            value = "",
                            type = NetworkStatusDM.Address.Type.Secondary,
                        ),
                    ),
                ),
            ),
        )
    }

    data class ConvertModel(val value: NetworkAddressConverter.Value, val expected: Result<NetworkAddress>)

    data class ConvertBackModel(val value: NetworkAddress, val expected: NetworkAddressConverter.Value)
}