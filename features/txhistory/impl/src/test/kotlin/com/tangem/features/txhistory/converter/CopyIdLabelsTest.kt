package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * The id a row exposes is named after the operation behind it, because an express deal copies the provider's own id
 * while an on-chain row copies its hash.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CopyIdLabelsTest : TxDetailsConverterTestBase() {

    @ParameterizedTest
    @ProvideTestModels
    fun copyIdLabels(model: CopyIdLabelsModel) {
        // Act
        val actual = model.tx.copyIdLabels

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class CopyIdLabelsModel(val tx: TxHistoryInfo, val expected: CopyIdLabels)

    private fun provideTestModels() = listOf(
        CopyIdLabelsModel(
            tx = expressSwap(status = ExpressExchangeStatus.Finished, externalTxId = "provider-swap-1"),
            expected = CopyIdLabels.Swap,
        ),
        CopyIdLabelsModel(
            tx = expressOnramp(status = ExpressOnrampStatus.Finished, externalTxId = "provider-onramp-1"),
            expected = CopyIdLabels.Onramp,
        ),
        CopyIdLabelsModel(
            tx = onChain(type = TransactionType.Transfer),
            expected = CopyIdLabels.Transaction,
        ),
    )
}