package com.tangem.features.onramp.success.entity.conterter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetOnrampSuccessContentConverterTest {

    private val cryptoCurrency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
    private val currencyName = cryptoCurrency.name

    private val transaction = OnrampTransaction(
        txId = "tx",
        userWalletId = UserWalletId(stringValue = "0011"),
        fromAmount = BigDecimal("100"),
        fromCurrency = OnrampCurrency(name = "Euro", code = "EUR", image = null, precision = 2, unit = "€"),
        toAmount = BigDecimal("0.05"),
        toCurrencyId = "ethereum",
        status = OnrampStatus.Status.Created,
        externalTxUrl = null,
        externalTxId = null,
        timestamp = 0L,
        providerName = "Provider",
        providerImageUrl = "",
        providerType = "type",
        redirectUrl = "",
        paymentMethod = "card",
        residency = "ES",
    )

    private val converter = SetOnrampSuccessContentConverter(
        cryptoCurrency = cryptoCurrency,
        transaction = transaction,
        goToProviderClick = {},
        onCopyClick = {},
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN status WHEN convert THEN status title matches status`(model: StatusTitleModel) {
        // Arrange
        val status = onrampStatus(status = model.status)

        // Act
        val actual = converter.convert(status).asContent()

        // Assert
        assertThat(actual.activeStatus).isEqualTo(model.status)
        assertThat(actual.statusTitle).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN external tx id WHEN convert THEN it is passed through`() {
        // Arrange
        val status = onrampStatus(externalTxId = "ext-123")

        // Act
        val actual = converter.convert(status).asContent()

        // Assert
        assertThat(actual.externalTxId).isEqualTo("ext-123")
    }

    @Test
    fun `GIVEN no external tx id WHEN convert THEN it is null`() {
        // Arrange
        val status = onrampStatus(externalTxId = null)

        // Act
        val actual = converter.convert(status).asContent()

        // Assert
        assertThat(actual.externalTxId).isNull()
    }

    private fun OnrampSuccessComponentUM.asContent() = this as OnrampSuccessComponentUM.Content

    private fun onrampStatus(
        status: OnrampStatus.Status = OnrampStatus.Status.Paid,
        externalTxId: String? = null,
    ) = OnrampStatus(
        txId = "tx",
        providerId = "provider",
        payoutAddress = "0xaddress",
        status = status,
        failReason = null,
        externalTxId = externalTxId,
        externalTxUrl = null,
        payoutHash = null,
        createdAt = "2024-01-01T00:00:00Z",
        fromCurrencyCode = "EUR",
        fromAmount = "100",
        toContractAddress = "",
        toNetwork = "ethereum",
        toDecimals = "18",
        toAmount = "0.05",
        toActualAmount = null,
        paymentMethod = "card",
        countryCode = "ES",
    )

    data class StatusTitleModel(val status: OnrampStatus.Status, val expected: TextReference)

    private fun provideTestModels(): List<StatusTitleModel> {
        val name = wrappedList<Any>(currencyName)
        val receiving = resourceReference(R.string.express_exchange_status_receiving_active)
        val failed = resourceReference(R.string.express_exchange_status_failed)
        return listOf(
            StatusTitleModel(OnrampStatus.Status.Created, receiving),
            StatusTitleModel(OnrampStatus.Status.WaitingForPayment, receiving),
            StatusTitleModel(
                OnrampStatus.Status.PaymentProcessing,
                resourceReference(R.string.express_exchange_status_confirming_active),
            ),
            StatusTitleModel(
                OnrampStatus.Status.Verifying,
                resourceReference(R.string.express_exchange_status_verifying),
            ),
            StatusTitleModel(OnrampStatus.Status.Paid, resourceReference(R.string.express_status_buying_active, name)),
            StatusTitleModel(
                OnrampStatus.Status.Sending,
                resourceReference(R.string.express_exchange_status_sending_active, name),
            ),
            StatusTitleModel(OnrampStatus.Status.Finished, resourceReference(R.string.express_status_bought, name)),
            StatusTitleModel(
                OnrampStatus.Status.RefundInProgress,
                resourceReference(R.string.express_exchange_status_refunding),
            ),
            StatusTitleModel(
                OnrampStatus.Status.Refunded,
                resourceReference(R.string.express_exchange_status_refunded),
            ),
            StatusTitleModel(OnrampStatus.Status.Paused, resourceReference(R.string.express_exchange_status_paused)),
            StatusTitleModel(OnrampStatus.Status.Expired, failed),
            StatusTitleModel(OnrampStatus.Status.Failed, failed),
        )
    }
}