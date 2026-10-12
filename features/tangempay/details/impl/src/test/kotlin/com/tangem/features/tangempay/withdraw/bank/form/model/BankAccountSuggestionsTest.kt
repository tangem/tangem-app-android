package com.tangem.features.tangempay.withdraw.bank.form.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import org.junit.jupiter.api.Test

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

    private val accounts = listOf(basit, arasaka, saburo)

    @Test
    fun `GIVEN saved accounts WHEN prefix matches two THEN both returned sorted by name`() {
        // Act
        val actual = BankAccountSuggestions.match(accounts = accounts, prefix = "48")

        // Assert
        assertThat(actual).containsExactly(arasaka, basit).inOrder()
    }

    @Test
    fun `GIVEN blank prefix WHEN match THEN empty list`() {
        // Act
        val actual = BankAccountSuggestions.match(accounts = accounts, prefix = "   ")

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN prefix in the middle of a number WHEN match THEN not returned`() {
        // Act
        val actual = BankAccountSuggestions.match(accounts = accounts, prefix = "0019")

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN full number typed WHEN match THEN account returned`() {
        // Act
        val actual = BankAccountSuggestions.match(accounts = accounts, prefix = "4830019271")

        // Assert
        assertThat(actual).containsExactly(basit)
    }
}