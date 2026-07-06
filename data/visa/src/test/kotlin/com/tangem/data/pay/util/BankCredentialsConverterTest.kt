package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.pay.models.response.BankCredentialsResponse
import com.tangem.domain.models.account.BankCredentials
import org.junit.jupiter.api.Test

internal class BankCredentialsConverterTest {

    @Test
    fun `GIVEN full response WHEN convert THEN all fields mapped`() {
        // Arrange
        val response = BankCredentialsResponse(
            type = "fiat",
            beneficiaryName = "Ivan Ivanov",
            beneficiaryAddress = "18, Rue Rubens 20, Paris, Ile-de-France 75013, US",
            beneficiaryBankName = "SSB BANK",
            beneficiaryBankAddress = "8700 Perry Highway, Pittsburgh, PA 15237, US",
            accountNumber = "707613210122",
            routingNumber = "043087080",
        )

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        val expected = BankCredentials(
            type = "fiat",
            beneficiaryName = "Ivan Ivanov",
            beneficiaryAddress = "18, Rue Rubens 20, Paris, Ile-de-France 75013, US",
            beneficiaryBankName = "SSB BANK",
            beneficiaryBankAddress = "8700 Perry Highway, Pittsburgh, PA 15237, US",
            accountNumber = "707613210122",
            routingNumber = "043087080",
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN null fields WHEN convert THEN mapped to empty strings`() {
        // Arrange
        val response = BankCredentialsResponse(
            type = null,
            beneficiaryName = null,
            beneficiaryAddress = null,
            beneficiaryBankName = null,
            beneficiaryBankAddress = null,
            accountNumber = null,
            routingNumber = null,
        )

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        val expected = BankCredentials(
            type = "",
            beneficiaryName = "",
            beneficiaryAddress = "",
            beneficiaryBankName = "",
            beneficiaryBankAddress = "",
            accountNumber = "",
            routingNumber = "",
        )
        assertThat(actual).isEqualTo(expected)
    }
}