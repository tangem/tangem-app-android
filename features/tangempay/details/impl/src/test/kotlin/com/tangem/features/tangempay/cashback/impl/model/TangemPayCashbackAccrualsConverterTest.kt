package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.pay.model.CashbackDocument
import org.junit.jupiter.api.Test

internal class TangemPayCashbackAccrualsConverterTest {

    @Test
    fun `GIVEN docs present WHEN convert THEN doc rows carry titles and info rows are static`() {
        // Arrange
        val converter = TangemPayCashbackAccrualsConverter(onDocClick = {})
        val docs = listOf(
            CashbackDocument(id = "1", title = "All categories without cashback", url = "https://a"),
            CashbackDocument(id = "2", title = "Full terms of cashback program", url = "https://b"),
        )

        // Act
        val result = converter.convert(docs)

        // Assert
        assertThat(result.title).isEqualTo(resourceReference(R.string.tangempay_cashback_accruals_title))
        assertThat(result.infoRows).hasSize(3)
        assertThat(result.docRows.map { it.title }).containsExactly(
            stringReference("All categories without cashback"),
            stringReference("Full terms of cashback program"),
        ).inOrder()
    }

    @Test
    fun `GIVEN empty docs WHEN convert THEN no doc rows but info rows remain`() {
        // Arrange
        val converter = TangemPayCashbackAccrualsConverter(onDocClick = {})

        // Act
        val result = converter.convert(emptyList())

        // Assert
        assertThat(result.docRows).isEmpty()
        assertThat(result.infoRows).hasSize(3)
    }

    @Test
    fun `GIVEN a doc row WHEN its onClick invoked THEN the doc url is opened`() {
        // Arrange
        var openedUrl: String? = null
        val converter = TangemPayCashbackAccrualsConverter(onDocClick = { openedUrl = it })
        val docs = listOf(CashbackDocument(id = "1", title = "Terms", url = "https://terms"))

        // Act
        val result = converter.convert(docs)
        result.docRows.single().onClick()

        // Assert
        assertThat(openedUrl).isEqualTo("https://terms")
    }
}