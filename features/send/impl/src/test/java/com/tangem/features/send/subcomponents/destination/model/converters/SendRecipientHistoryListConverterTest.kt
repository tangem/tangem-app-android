package com.tangem.features.send.subcomponents.destination.model.converters

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.send.impl.R
import com.tangem.features.send.subcomponents.destination.model.transformers.RECENT_DEFAULT_COUNT
import com.tangem.features.send.subcomponents.destination.model.transformers.RECENT_KEY_TAG
import com.tangem.features.send.subcomponents.destination.model.transformers.emptyListState
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendRecipientHistoryListConverterTest {

    private val cryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val converter = SendRecipientHistoryListConverter(cryptoCurrency)

    @BeforeEach
    fun setUp() {
        // Mapping formats the timestamp via DateTimeFormatters -> DateFormat.getBestDateTimePattern,
        // which is an Android stub on the JVM. Mirror the project pattern so convert() runs.
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
    }

    private fun txInfo(
        isOutgoing: Boolean = true,
        type: TxInfo.TransactionType = TxInfo.TransactionType.Transfer,
        interactionAddressType: TxInfo.InteractionAddressType? = TxInfo.InteractionAddressType.User(RECIPIENT),
        destinationType: TxInfo.DestinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User(RECIPIENT)),
        sourceType: TxInfo.SourceType = TxInfo.SourceType.Single(SOURCE),
        amount: BigDecimal = BigDecimal.ONE,
        txHash: String = "hash",
    ) = TxInfo(
        txHash = txHash,
        timestampInMillis = 1_700_000_000_000L,
        isOutgoing = isOutgoing,
        destinationType = destinationType,
        sourceType = sourceType,
        interactionAddressType = interactionAddressType,
        status = TxInfo.TransactionStatus.Confirmed,
        type = type,
        amount = amount,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Filtering {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN excluded transaction WHEN convert THEN filtered out leaving empty placeholder`(model: FilterModel) {
            // Act
            val actual = converter.convert(listOf(model.tx))

            // Assert
            assertThat(actual).isEqualTo(emptyListState(RECENT_KEY_TAG, RECENT_DEFAULT_COUNT))
        }

        private fun provideTestModels() = listOf(
            FilterModel("non-transfer type", txInfo(type = TxInfo.TransactionType.Swap)),
            FilterModel(
                "contract interaction",
                txInfo(interactionAddressType = TxInfo.InteractionAddressType.Contract(RECIPIENT)),
            ),
            FilterModel("null interaction", txInfo(interactionAddressType = null)),
            FilterModel("incoming", txInfo(isOutgoing = false)),
            FilterModel(
                "multiple destinations",
                txInfo(destinationType = TxInfo.DestinationType.Multiple(listOf(TxInfo.AddressType.User(RECIPIENT)))),
            ),
            FilterModel("zero amount", txInfo(amount = BigDecimal.ZERO)),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Mapping {

        @Test
        fun `GIVEN valid outgoing transfer WHEN convert THEN mapped to recipient item`() {
            // Act
            val actual = converter.convert(listOf(txInfo()))

            // Assert
            assertThat(actual).hasSize(1)
            val item = actual.first()
            assertThat(item.id).isEqualTo("${RECENT_KEY_TAG}0")
            assertThat(item.title).isEqualTo(stringReference(RECIPIENT))
            assertThat(item.subtitleEndOffset).isEqualTo(cryptoCurrency.symbol.length)
            assertThat(item.subtitleIconRes).isEqualTo(R.drawable.ic_arrow_up_24)
            assertThat(item.isVisible).isTrue()
        }

        @Test
        fun `GIVEN more than ten valid transactions WHEN convert THEN capped at ten`() {
            // Arrange
            val txs = (1..12).map { txInfo(txHash = "hash$it") }

            // Act
            val actual = converter.convert(txs)

            // Assert
            assertThat(actual).hasSize(10)
            assertThat(actual.first().id).isEqualTo("${RECENT_KEY_TAG}0")
            assertThat(actual.last().id).isEqualTo("${RECENT_KEY_TAG}9")
        }
    }

    data class FilterModel(val case: String, val tx: TxInfo)

    private companion object {
        private const val RECIPIENT = "0xRecipientAddress"
        private const val SOURCE = "0xSourceAddress"
    }
}