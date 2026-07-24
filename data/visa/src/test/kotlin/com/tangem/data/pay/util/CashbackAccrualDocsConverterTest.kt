package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.pay.models.response.CashbackAccrualDocsResponse
import com.tangem.datasource.api.pay.models.response.CashbackAccrualDocsResponse.Doc
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CashbackAccrualDocsConverterTest {

    @Test
    fun `GIVEN valid docs WHEN convert THEN all docs mapped in order`() {
        // Arrange
        val response = response(
            doc(id = "1", title = "All categories", url = "https://a"),
            doc(id = "2", title = "Full terms", url = "https://b"),
        )

        // Act
        val result = CashbackAccrualDocsConverter.convert(response)

        // Assert
        assertThat(result).containsExactly(
            CashbackDocument(id = "1", title = "All categories", url = "https://a"),
            CashbackDocument(id = "2", title = "Full terms", url = "https://b"),
        ).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN doc missing a required field WHEN convert THEN it is dropped`(malformed: Doc) {
        // Arrange
        val response = response(doc(), malformed)

        // Act
        val result = CashbackAccrualDocsConverter.convert(response)

        // Assert
        assertThat(result).containsExactly(CashbackDocument(id = "1", title = "Title", url = "https://a"))
    }

    @Test
    fun `GIVEN null docs WHEN convert THEN empty list`() {
        // Act
        val result = CashbackAccrualDocsConverter.convert(CashbackAccrualDocsResponse(docs = null))

        // Assert
        assertThat(result).isEmpty()
    }

    private fun provideTestModels() = listOf(
        doc(id = null),
        doc(title = null),
        doc(url = null),
    )

    private fun response(vararg docs: Doc) = CashbackAccrualDocsResponse(docs = docs.toList())

    private fun doc(id: String? = "1", title: String? = "Title", url: String? = "https://a") =
        Doc(id = id, title = title, url = url)
}