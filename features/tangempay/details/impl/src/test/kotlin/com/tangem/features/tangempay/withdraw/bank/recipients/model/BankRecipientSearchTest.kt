package com.tangem.features.tangempay.withdraw.bank.recipients.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankRecipientSearchTest {

    private val accounts = listOf(
        bankAccount(
            id = "acc-1",
            beneficiary = Beneficiary.Individual(firstName = "Basit", lastName = "Didenko"),
            accountNumber = "4830019271",
        ).copy(displayName = "Basit Didenko JPMorgan"),
        bankAccount(
            id = "acc-2",
            beneficiary = Beneficiary.Business(name = "Raxanax"),
            accountNumber = "1120938475",
        ).copy(displayName = "Raxanax JPMorgan"),
        bankAccount(
            id = "acc-3",
            beneficiary = Beneficiary.Individual(firstName = "Saburo", lastName = "Arasaka"),
            accountNumber = "7719004432",
        ).copy(displayName = "Saburo Arasaka JPMorgan"),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN query WHEN filter THEN matching accounts kept in order`(model: FilterModel) {
        // Act
        val actual = BankRecipientSearch.filter(accounts = accounts, query = model.query)

        // Assert
        assertThat(actual.map { it.id }).containsExactlyElementsIn(model.expectedIds).inOrder()
    }

    data class FilterModel(val query: String, val expectedIds: List<String>)

    private fun provideTestModels() = listOf(
        FilterModel(query = "", expectedIds = listOf("acc-1", "acc-2", "acc-3")),
        FilterModel(query = "   ", expectedIds = listOf("acc-1", "acc-2", "acc-3")),
        FilterModel(query = "basit", expectedIds = listOf("acc-1")),
        FilterModel(query = "DIDENKO", expectedIds = listOf("acc-1")),
        FilterModel(query = " ara ", expectedIds = listOf("acc-3")),
        FilterModel(query = "rax", expectedIds = listOf("acc-2")),
        FilterModel(query = "1120", expectedIds = listOf("acc-2")),
        FilterModel(query = "4432", expectedIds = listOf("acc-3")),
        FilterModel(query = "a", expectedIds = listOf("acc-1", "acc-2", "acc-3")),
        FilterModel(query = "zzz", expectedIds = emptyList()),
        FilterModel(query = "jpmorgan", expectedIds = emptyList()),
    )
}