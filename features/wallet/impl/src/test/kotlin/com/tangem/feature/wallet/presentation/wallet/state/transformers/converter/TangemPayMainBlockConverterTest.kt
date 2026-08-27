package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.TangemPayIntents
import com.tangem.features.tangempay.entity.TangemPayMainUM
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayMainBlockConverterTest {

    private val intents: TangemPayIntents = mockk(relaxed = true)

    private val converter = TangemPayMainBlockConverter(
        tangemPayClickIntents = intents,
        isMultipleCardsEnabled = false,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(intents)
    }

    @Test
    fun `GIVEN card issue failed WHEN converting THEN block click opens the payment account`() {
        // Arrange
        val status = paymentStatus(PaymentAccountStatusValue.Error.CardIssueFailed(customerId = "cust_1"))

        // Act
        val block = converter.convert(status)

        // Assert
        assertThat(block).isInstanceOf(TangemPayMainUM.FailedToIssue::class.java)
        (block as TangemPayMainUM.FailedToIssue).onClick()
        verify(exactly = 1) { intents.openDetails(status) }
    }

    private fun paymentStatus(value: PaymentAccountStatusValue) = AccountStatus.Payment(
        account = Account.Payment(userWalletId = UserWalletId("011")),
        value = value,
    )
}