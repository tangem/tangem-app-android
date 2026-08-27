package com.tangem.features.tangempay.addfunds

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.features.tangempay.details.impl.R
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayAddFundsUMConverterTest {

    private val listener: AddFundsListener = mockk(relaxed = true)
    private val data: TangemPayTopUpData = mockk()

    private fun convert(shouldShowBankTransfer: Boolean, isMultichainEnabled: Boolean) = TangemPayAddFundsUMConverter(
        listener = listener,
        shouldShowBankTransfer = shouldShowBankTransfer,
        isMultichainEnabled = isMultichainEnabled,
    ).convert(data)

    private fun titleIds(um: TangemPayAddFundsUM): List<Int> {
        return um.items.map { (it.title as TextReference.Res).id }
    }

    @Test
    fun `GIVEN multichain off WHEN convert THEN legacy order with bank transfer last`() {
        // Act
        val um = convert(shouldShowBankTransfer = true, isMultichainEnabled = false)

        // Assert
        assertThat(titleIds(um)).containsExactly(
            R.string.tangempay_topup_swap_title,
            R.string.tangempay_topup_receive_title,
            R.string.tangempay_topup_bank_transfer_title,
        ).inOrder()
    }

    @Test
    fun `GIVEN multichain on WHEN convert THEN design order with bank transfer first`() {
        // Act
        val um = convert(shouldShowBankTransfer = true, isMultichainEnabled = true)

        // Assert
        assertThat(titleIds(um)).containsExactly(
            R.string.tangempay_topup_bank_transfer_title,
            R.string.tangempay_topup_swap_title,
            R.string.tangempay_topup_receive_title,
        ).inOrder()
    }

    @Test
    fun `GIVEN multichain on without bank transfer WHEN convert THEN swap then receive`() {
        // Act
        val um = convert(shouldShowBankTransfer = false, isMultichainEnabled = true)

        // Assert
        assertThat(titleIds(um)).containsExactly(
            R.string.tangempay_topup_swap_title,
            R.string.tangempay_topup_receive_title,
        ).inOrder()
    }

    @Test
    fun `GIVEN multichain off WHEN convert THEN receive row uses the single-network body`() {
        // Act
        val um = convert(shouldShowBankTransfer = false, isMultichainEnabled = false)

        // Assert
        val receive = um.items.single { (it.title as TextReference.Res).id == R.string.tangempay_topup_receive_title }
        assertThat((receive.description as TextReference.Res).id).isEqualTo(R.string.tangempay_topup_receive_body)
    }

    @Test
    fun `GIVEN multichain on WHEN convert THEN receive row uses the multichain body`() {
        // Act
        val um = convert(shouldShowBankTransfer = false, isMultichainEnabled = true)

        // Assert
        val receive = um.items.single { (it.title as TextReference.Res).id == R.string.tangempay_topup_receive_title }
        assertThat((receive.description as TextReference.Res).id)
            .isEqualTo(R.string.tangempay_topup_receive_body_multichain)
    }
}