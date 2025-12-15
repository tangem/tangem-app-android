package com.tangem.domain.account.status.utils

import com.google.common.truth.Truth
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountNameIndexerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TestModel) {
        // Act
        val actual = AccountNameIndexer.transform(model.input)

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TestModel(input = "Bitcoin", expected = "Bitcoin(1)"),
        TestModel(input = "Bitcoin(5)", expected = "Bitcoin(6)"),
        TestModel(input = "Ethereum(123)", expected = "Ethereum(124)"),
        TestModel(input = "Account(0)", expected = "Account(1)"),
        TestModel(input = "Bitcoin(old)new", expected = "Bitcoin(old)new(1)"),
        TestModel(input = "Bitcoin(10)new", expected = "Bitcoin(10)new(1)"),
        TestModel(input = "", expected = "(1)"),
        TestModel(input = "(42)", expected = "(43)"),
        TestModel(input = "Account(999999)", expected = "Account(1000000)"),
        TestModel(input = "Wallet(9)", expected = "Wallet(10)"),
        TestModel(input = "My Account (5)", expected = "My Account (6)"),
        TestModel(input = "Bitcoin💰(2)", expected = "Bitcoin💰(3)"),
        TestModel(input = "Bitcoin(abc)", expected = "Bitcoin(abc)(1)"),
        TestModel(input = "Bitcoin()", expected = "Bitcoin()(1)"),
        TestModel(input = "Bitcoin(", expected = "Bitcoin((1)"),
        TestModel(input = "Bitcoin)", expected = "Bitcoin)(1)"),
        TestModel(input = "EthereumEthereumEthereum(999)", expected = "EthereumEthere(1000)"),
        TestModel(input = "a".repeat(100), expected = "a".repeat(17) + "(1)"),
    )

    data class TestModel(val input: String, val expected: String)
}