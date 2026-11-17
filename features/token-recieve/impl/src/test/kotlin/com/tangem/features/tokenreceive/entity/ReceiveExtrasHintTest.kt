package com.tangem.features.tokenreceive.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReceiveExtrasHintTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN extras type and disclaimer flag WHEN receiveExtrasHint THEN returns the matching hint`(
        model: HintModel,
    ) {
        // Act
        val actual = receiveExtrasHint(model.shouldShowMemoDisclaimer, model.transactionExtrasType)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        HintModel(true, Network.TransactionExtrasType.MEMO, expected = ReceiveExtrasHint.NoMemo),
        HintModel(true, Network.TransactionExtrasType.DESTINATION_TAG, expected = ReceiveExtrasHint.NoTag),
        HintModel(true, Network.TransactionExtrasType.NONE, expected = ReceiveExtrasHint.None),
        HintModel(false, Network.TransactionExtrasType.MEMO, expected = ReceiveExtrasHint.None),
        HintModel(false, Network.TransactionExtrasType.DESTINATION_TAG, expected = ReceiveExtrasHint.None),
        HintModel(false, Network.TransactionExtrasType.NONE, expected = ReceiveExtrasHint.None),
    )

    data class HintModel(
        val shouldShowMemoDisclaimer: Boolean,
        val transactionExtrasType: Network.TransactionExtrasType,
        val expected: ReceiveExtrasHint,
    )
}