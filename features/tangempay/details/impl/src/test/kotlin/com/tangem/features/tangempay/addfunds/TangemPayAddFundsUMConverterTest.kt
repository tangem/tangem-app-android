package com.tangem.features.tangempay.addfunds

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_government_20
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_20
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.features.tangempay.details.impl.R
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayAddFundsUMConverterTest {

    private val listener: AddFundsListener = mockk(relaxed = true)
    private val data: TangemPayTopUpData = mockk()

    private fun convert(
        onramp: VirtualAccountOnramp?,
        isVisaOnrampV2Enabled: Boolean = false,
        isBankTransferLoading: Boolean = false,
    ) = TangemPayAddFundsUMConverter(
        listener = listener,
        virtualAccountOnramp = onramp,
        isVisaOnrampV2Enabled = isVisaOnrampV2Enabled,
    ).convert(TangemPayAddFundsUMConverter.Input(data = data, isBankTransferLoading = isBankTransferLoading))

    private fun titleIds(um: TangemPayAddFundsUM): List<Int> {
        return um.items.map { (it.title as TextReference.Res).id }
    }

    private fun TangemPayAddFundsUM.bankTransfer(): TangemPayAddFundsItemUM = items.single {
        it.title == resourceReference(R.string.tangempay_topup_bank_transfer_title)
    }

    @Test
    fun `GIVEN bank transfer available WHEN convert THEN swap then receive then bank transfer last`() {
        // Act
        val um = convert(onramp = VirtualAccountOnramp.Eligible)

        // Assert
        assertThat(titleIds(um)).containsExactly(
            R.string.tangempay_topup_swap_title,
            R.string.tangempay_topup_receive_title,
            R.string.tangempay_topup_bank_transfer_title,
        ).inOrder()
    }

    @Test
    fun `GIVEN no virtual account onramp WHEN convert THEN swap then receive`() {
        // Act
        val um = convert(onramp = null)

        // Assert
        assertThat(titleIds(um)).containsExactly(
            R.string.tangempay_topup_swap_title,
            R.string.tangempay_topup_receive_title,
        ).inOrder()
    }

    @Test
    fun `GIVEN any data WHEN convert THEN receive row uses the multichain body`() {
        // Act
        val um = convert(onramp = null)

        // Assert
        val receive = um.items.single { (it.title as TextReference.Res).id == R.string.tangempay_topup_receive_title }
        assertThat((receive.description as TextReference.Res).id)
            .isEqualTo(R.string.tangempay_topup_receive_body_multichain)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN onramp state WHEN convert THEN bank transfer row matches the state`(model: BankTransferRowModel) {
        // Act
        val row = convert(
            onramp = model.onramp,
            isVisaOnrampV2Enabled = model.isV2Enabled,
            isBankTransferLoading = model.isBankTransferLoading,
        ).bankTransfer()

        // Assert
        assertThat((row.icon as TangemIconUM.Icon).copy(tint = null))
            .isEqualTo(TangemIconUM.Icon(imageVector = model.expectedIcon))
        assertThat(row.description).isEqualTo(model.expectedDescription)
        if (model.isClickable) assertThat(row.onClick).isNotNull() else assertThat(row.onClick).isNull()
        assertThat(row.accent).isEqualTo(model.expectedAccent)
    }

    private fun provideTestModels() = listOf(
        BankTransferRowModel(
            isV2Enabled = false,
            onramp = VirtualAccountOnramp.Processing,
            expectedIcon = Icons.ic_sign_usd_20,
            expectedDescription = resourceReference(R.string.tangempay_topup_bank_transfer_body),
            isClickable = true,
            expectedAccent = TangemPayAddFundsItemUM.Accent.Info,
        ),
        BankTransferRowModel(
            isV2Enabled = true,
            onramp = VirtualAccountOnramp.Eligible,
            expectedIcon = Icons.ic_government_20,
            expectedDescription = resourceReference(R.string.tangempay_topup_bank_transfer_body),
            isClickable = true,
            expectedAccent = TangemPayAddFundsItemUM.Accent.Info,
        ),
        BankTransferRowModel(
            isV2Enabled = true,
            onramp = VirtualAccountOnramp.Available(productInstanceId = "pi_account"),
            expectedIcon = Icons.ic_government_20,
            expectedDescription = resourceReference(R.string.tangempay_topup_bank_transfer_body),
            isClickable = true,
            expectedAccent = TangemPayAddFundsItemUM.Accent.Info,
        ),
        BankTransferRowModel(
            isV2Enabled = true,
            onramp = VirtualAccountOnramp.Failed,
            expectedIcon = Icons.ic_government_20,
            expectedDescription = resourceReference(R.string.tangempay_topup_bank_transfer_body),
            isClickable = true,
            expectedAccent = TangemPayAddFundsItemUM.Accent.Info,
        ),
        BankTransferRowModel(
            isV2Enabled = true,
            onramp = VirtualAccountOnramp.Processing,
            expectedIcon = Icons.ic_clock_20,
            expectedDescription = resourceReference(R.string.tangempay_topup_bank_transfer_pending_body),
            isClickable = false,
            expectedAccent = TangemPayAddFundsItemUM.Accent.Warning,
        ),
        BankTransferRowModel(
            isV2Enabled = true,
            onramp = VirtualAccountOnramp.Available(productInstanceId = "pi_account"),
            isBankTransferLoading = true,
            expectedIcon = Icons.ic_clock_20,
            expectedDescription = resourceReference(R.string.tangempay_topup_bank_transfer_pending_body),
            isClickable = false,
            expectedAccent = TangemPayAddFundsItemUM.Accent.Warning,
        ),
    )

    data class BankTransferRowModel(
        val isV2Enabled: Boolean,
        val onramp: VirtualAccountOnramp,
        val isBankTransferLoading: Boolean = false,
        val expectedIcon: ImageVector,
        val expectedDescription: TextReference,
        val isClickable: Boolean,
        val expectedAccent: TangemPayAddFundsItemUM.Accent,
    ) {
        override fun toString(): String =
            "v2 enabled = $isV2Enabled, ${onramp::class.simpleName}, loading = $isBankTransferLoading"
    }
}