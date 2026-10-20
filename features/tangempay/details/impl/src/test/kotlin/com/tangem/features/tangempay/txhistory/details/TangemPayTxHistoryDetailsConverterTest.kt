package com.tangem.features.tangempay.txhistory.details

import android.text.format.DateFormat
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_24
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.collateralTransaction
import com.tangem.features.tangempay.details.impl.R
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayTxHistoryDetailsConverterTest {

    private val defaultLocale = Locale.getDefault()
    private val onDisputeClick: () -> Unit = {}
    private val onDismiss: () -> Unit = {}

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
        Locale.setDefault(defaultLocale)
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN collateral type WHEN convert THEN sheet shows type title, icon, signed amount and no subtitle`(
        model: CollateralModel,
    ) {
        // Arrange
        val input = TangemPayTxHistoryDetailsConverter.Input(
            item = collateralTransaction(type = model.type, amount = model.amount),
            isBalanceHidden = false,
            transactionLoadState = TransactionLoadState.Loading,
            cashbackDetails = null,
            cashbackLoadState = TransactionLoadState.Loading,
            onExplorerClick = {},
            onDisputeClick = onDisputeClick,
            onCardRefreshClick = {},
            onCashbackRefreshClick = {},
            onDismiss = onDismiss,
        )

        // Act
        val actual = TangemPayTxHistoryDetailsConverter.convert(input)

        // Assert
        val expected = TangemPayTxHistoryDetailsUM(
            isBalanceHidden = false,
            title = model.expectedTitle,
            subtitle = actual.subtitle,
            iconState = TangemIconUM.Icon(
                imageVector = model.expectedIcon,
                tintReference = (actual.iconState as TangemIconUM.Icon).tint,
            ),
            transactionTitle = null,
            detail = null,
            cashbackDetail = null,
            transactionCategory = resourceReference(R.string.common_transfer),
            mcc = null,
            transactionAmount = model.expectedAmount,
            localTransactionText = null,
            label = null,
            buttonState = ButtonState(text = resourceReference(R.string.tangem_pay_get_help), onClick = onDisputeClick),
            dismiss = onDismiss,
        )
        assertThat(actual).isEqualTo(expected)
    }

    data class CollateralModel(
        val type: TangemPayTxHistoryItem.Type,
        val amount: BigDecimal,
        val expectedTitle: TextReference,
        val expectedAmount: String,
        val expectedIcon: ImageVector,
    )

    private fun provideTestModels() = listOf(
        CollateralModel(
            type = TangemPayTxHistoryItem.Type.Deposit,
            amount = BigDecimal("50.00"),
            expectedTitle = resourceReference(R.string.tangem_pay_deposit),
            expectedAmount = "+$50.00",
            expectedIcon = Icons.ic_arrow_down_24,
        ),
        CollateralModel(
            type = TangemPayTxHistoryItem.Type.Withdrawal,
            amount = BigDecimal("-20.00"),
            expectedTitle = resourceReference(R.string.tangem_pay_withdrawal),
            expectedAmount = "-$20.00",
            expectedIcon = Icons.ic_arrow_up_24,
        ),
        CollateralModel(
            type = TangemPayTxHistoryItem.Type.Cashback,
            amount = BigDecimal("19.00"),
            expectedTitle = resourceReference(R.string.tangempay_cashback_title),
            expectedAmount = "+$19.00",
            expectedIcon = Icons.ic_percent_backward_24,
        ),
    )
}