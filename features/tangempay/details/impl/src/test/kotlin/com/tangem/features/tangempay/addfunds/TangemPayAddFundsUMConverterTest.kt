package com.tangem.features.tangempay.addfunds

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.features.tangempay.details.impl.R
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class TangemPayAddFundsUMConverterTest {

    private val listener: AddFundsListener = mockk(relaxed = true)
    private val data: TangemPayTopUpData = mockk()

    private fun convert(shouldShowBankTransfer: Boolean, isBankTransferLoading: Boolean = false) =
        TangemPayAddFundsUMConverter(
            listener = listener,
            shouldShowBankTransfer = shouldShowBankTransfer,
        ).convert(TangemPayAddFundsUMConverter.Input(data = data, isBankTransferLoading = isBankTransferLoading))

    private fun titleIds(um: TangemPayAddFundsUM): List<Int> {
        return um.items.map { (it.title as TextReference.Res).id }
    }

    private fun TangemPayAddFundsUM.bankTransfer(): TangemPayAddFundsItemUM = items.single {
        (it.title as TextReference.Res).id == R.string.tangempay_topup_bank_transfer_title
    }

    @Test
    fun `GIVEN bank transfer shown WHEN convert THEN bank transfer first then swap then receive`() {
        // Act
        val um = convert(shouldShowBankTransfer = true)

        // Assert
        assertThat(titleIds(um)).containsExactly(
            R.string.tangempay_topup_bank_transfer_title,
            R.string.tangempay_topup_swap_title,
            R.string.tangempay_topup_receive_title,
        ).inOrder()
    }

    @Test
    fun `GIVEN bank transfer hidden WHEN convert THEN swap then receive`() {
        // Act
        val um = convert(shouldShowBankTransfer = false)

        // Assert
        assertThat(titleIds(um)).containsExactly(
            R.string.tangempay_topup_swap_title,
            R.string.tangempay_topup_receive_title,
        ).inOrder()
    }

    @Test
    fun `GIVEN any data WHEN convert THEN receive row uses the multichain body`() {
        // Act
        val um = convert(shouldShowBankTransfer = false)

        // Assert
        val receive = um.items.single { (it.title as TextReference.Res).id == R.string.tangempay_topup_receive_title }
        assertThat((receive.description as TextReference.Res).id)
            .isEqualTo(R.string.tangempay_topup_receive_body_multichain)
    }

    @Test
    fun `GIVEN banking details idle WHEN convert THEN bank transfer row is clickable`() {
        // Act
        val row = convert(shouldShowBankTransfer = true).bankTransfer()

        // Assert
        assertThat(row.isLoading).isFalse()
        assertThat(row.onClick).isNotNull()
    }

    @Test
    fun `GIVEN banking details loading WHEN convert THEN bank transfer row loads and is not clickable`() {
        // Act
        val row = convert(shouldShowBankTransfer = true, isBankTransferLoading = true).bankTransfer()

        // Assert
        assertThat(row.isLoading).isTrue()
        assertThat(row.onClick).isNull()
    }
}