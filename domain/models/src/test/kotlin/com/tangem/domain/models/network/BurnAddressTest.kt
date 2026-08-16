package com.tangem.domain.models.network

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BurnAddressTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isBurnAddress(model: TestModel) {
        // Act
        val actual = model.address.isBurnAddress()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class TestModel(val address: String, val expected: Boolean)

    private fun provideTestModels() = listOf(
        TestModel(address = "0x0000000000000000000000000000000000000000", expected = true),
        TestModel(address = "0x000000000000000000000000000000000000dEaD", expected = true),
        // The dead address is usually written EIP-55 checksummed, but nothing forces it
        TestModel(address = "0x000000000000000000000000000000000000dead", expected = true),
        TestModel(address = "0x000000000000000000000000000000000000DEAD", expected = true),
        // A recipient decoded back from call data comes without the prefix
        TestModel(address = "0000000000000000000000000000000000000000", expected = true),
        TestModel(address = "000000000000000000000000000000000000dEaD", expected = true),
        TestModel(address = " 0x000000000000000000000000000000000000dEaD ", expected = true),
        // A regular recipient
        TestModel(address = "0xfc9013965447f804042a03ae4b98130a8c300a2f", expected = false),
        // Only the exact addresses are blacklisted, not everything that merely looks dead
        TestModel(address = "0x000000000000000000000000000000000000dEaE", expected = false),
        TestModel(address = "0xdEaD000000000000000042069420694206942069", expected = false),
        // Not an EVM address at all
        TestModel(address = "TWd4WrZ9wn84f5x1hZhL4DHvk738ns5jwb", expected = false),
        TestModel(address = "", expected = false),
    )
}