package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.BankCredentialsResponse
import com.tangem.domain.models.account.BankCredentials
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankCredentialsConverterTest {

    @Test
    fun `GIVEN full response WHEN convert THEN all fields mapped`() {
        // Arrange
        val response = result()

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
            transferMessage = "REF-A1B2C3D4",
            depositFee = BigDecimal("1.00"),
            depositLimit = BankCredentials.DepositLimit(
                amount = BigDecimal("10000"),
                period = BankCredentials.DepositLimit.Period.DAY,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN null fields WHEN convert THEN mapped to empty strings`() {
        // Arrange
        val response = BankCredentialsResponse.Result(
            type = null,
            beneficiaryName = null,
            beneficiaryAddress = null,
            beneficiaryBankName = null,
            beneficiaryBankAddress = null,
            accountNumber = null,
            routingNumber = null,
            rail = null,
            transferMessage = null,
            fee = null,
            depositLimit = null,
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
            transferMessage = null,
            depositFee = null,
            depositLimit = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN response without the extension WHEN convert THEN new fields are null`() {
        // Arrange
        val response = result(transferMessage = null, fee = null, depositLimit = null)

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.transferMessage).isNull()
        assertThat(actual.depositFee).isNull()
        assertThat(actual.depositLimit).isNull()
        assertThat(actual.accountNumber).isEqualTo("707613210122")
    }

    @Test
    fun `GIVEN blank transfer message WHEN convert THEN it is dropped`() {
        // Arrange
        val response = result(transferMessage = "   ")

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.transferMessage).isNull()
    }

    @Test
    fun `GIVEN unrecognised period WHEN convert THEN period is unknown`() {
        // Arrange
        val response = result(
            depositLimit = BankCredentialsResponse.DepositLimit(amount = "10000", period = "month"),
        )

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.depositLimit?.period).isEqualTo(BankCredentials.DepositLimit.Period.UNKNOWN)
    }

    @Test
    fun `GIVEN limit without amount WHEN convert THEN limit is dropped`() {
        // Arrange
        val response = result(
            depositLimit = BankCredentialsResponse.DepositLimit(amount = null, period = "day"),
        )

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.depositLimit).isNull()
    }

    private fun result(
        transferMessage: String? = "REF-A1B2C3D4",
        fee: BankCredentialsResponse.Fee? = BankCredentialsResponse.Fee(type = "flat", amount = "1.00"),
        depositLimit: BankCredentialsResponse.DepositLimit? = BankCredentialsResponse.DepositLimit(
            amount = "10000",
            period = "day",
        ),
    ) = BankCredentialsResponse.Result(
        type = "fiat",
        beneficiaryName = "Ivan Ivanov",
        beneficiaryAddress = "18, Rue Rubens 20, Paris, Ile-de-France 75013, US",
        beneficiaryBankName = "SSB BANK",
        beneficiaryBankAddress = "8700 Perry Highway, Pittsburgh, PA 15237, US",
        accountNumber = "707613210122",
        routingNumber = "043087080",
        rail = "ACH",
        transferMessage = transferMessage,
        fee = fee,
        depositLimit = depositLimit,
    )
}