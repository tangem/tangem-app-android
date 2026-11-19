package com.tangem.features.tangempay.withdraw.bank.form.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.bankofframp.BankAccount
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankAccountSuggestionsTest {

    private val basit = bankAccount(
        id = "acc-1",
        beneficiary = Beneficiary.Individual(firstName = "Basit", lastName = "Didenko"),
        accountNumber = "4830019271",
    )
    private val arasaka = bankAccount(
        id = "acc-2",
        beneficiary = Beneficiary.Business(name = "arasaka corp"),
        accountNumber = "4830555555",
    )
    private val saburo = bankAccount(
        id = "acc-3",
        beneficiary = Beneficiary.Individual(firstName = "Saburo", lastName = "Arasaka"),
        accountNumber = "7719004432",
    )
    private val hanako = bankAccount(
        id = "acc-4",
        beneficiary = Beneficiary.Individual(firstName = "Hanako", lastName = "Arasaka"),
        accountNumber = "483010000000000001",
        rail = BankRail.SPEI,
    )
    private val panam = bankAccount(
        id = "acc-5",
        beneficiary = Beneficiary.Individual(firstName = "Panam", lastName = "Palmer"),
        accountNumber = "483020000000000002",
        rail = BankRail.SPEI,
    )
    private val unknownRail = bankAccount(
        id = "acc-6",
        beneficiary = Beneficiary.Individual(firstName = "Jackie", lastName = "Welles"),
        accountNumber = "4830777777",
        rail = null,
    )

    private val accounts = listOf(basit, arasaka, saburo, hanako, panam, unknownRail)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN recipients on every rail WHEN number field matches THEN only that field's rail suggested`(
        model: FieldRailModel,
    ) {
        // Act
        val actual = BankAccountSuggestions.match(accounts = accounts, field = model.field, prefix = "4830")

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
    }

    @Test
    fun `GIVEN saved accounts WHEN prefix matches two THEN both returned sorted by name`() {
        // Act
        val actual = BankAccountSuggestions.match(
            accounts = listOf(basit, arasaka, saburo),
            field = BankWithdrawFormField.AccountNumber,
            prefix = "48",
        )

        // Assert
        assertThat(actual).containsExactly(arasaka, basit).inOrder()
    }

    @Test
    fun `GIVEN blank prefix WHEN match THEN empty list`() {
        // Act
        val actual = BankAccountSuggestions.match(
            accounts = accounts,
            field = BankWithdrawFormField.AccountNumber,
            prefix = "   ",
        )

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN prefix in the middle of a number WHEN match THEN not returned`() {
        // Act
        val actual = BankAccountSuggestions.match(
            accounts = accounts,
            field = BankWithdrawFormField.AccountNumber,
            prefix = "0019",
        )

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN full number typed WHEN match THEN account returned`() {
        // Act
        val actual = BankAccountSuggestions.match(
            accounts = accounts,
            field = BankWithdrawFormField.AccountNumber,
            prefix = "4830019271",
        )

        // Assert
        assertThat(actual).containsExactly(basit)
    }

    private fun provideTestModels() = listOf(
        FieldRailModel(field = BankWithdrawFormField.AccountNumber, expected = listOf(arasaka, basit, unknownRail)),
        FieldRailModel(field = BankWithdrawFormField.Clabe, expected = listOf(hanako, panam)),
        FieldRailModel(field = BankWithdrawFormField.RoutingNumber, expected = emptyList()),
    )

    data class FieldRailModel(val field: BankWithdrawFormField, val expected: List<BankAccount>)
}