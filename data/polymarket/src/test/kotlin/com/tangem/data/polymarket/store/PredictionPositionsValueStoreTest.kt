package com.tangem.data.polymarket.store

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PredictionPositionsValueStoreTest {

    private val store = PredictionPositionsValueStore()

    @Test
    fun `GIVEN nothing stored WHEN getSyncOrNull THEN it is null`() = runTest {
        // Act
        val actual = store.getSyncOrNull(WALLET_A)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a value WHEN store THEN it is emitted`() = runTest {
        // Act
        store.store(userWalletId = WALLET_A, value = VALUE)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(VALUE)
    }

    @Test
    fun `GIVEN a stored value WHEN markUnrefreshed THEN only the source changes`() = runTest {
        // Arrange
        store.store(userWalletId = WALLET_A, value = VALUE)

        // Act
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(VALUE.copy(source = StatusSource.ONLY_CACHE))
    }

    @Test
    fun `GIVEN nothing stored WHEN markUnrefreshed THEN nothing is recorded`() = runTest {
        // Act
        store.markUnrefreshed(userWalletId = WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
    }

    @Test
    fun `GIVEN two wallets stored WHEN one is cleared THEN the other stays`() = runTest {
        // Arrange
        store.store(userWalletId = WALLET_A, value = VALUE)
        store.store(userWalletId = WALLET_B, value = VALUE)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.get(WALLET_B).first()).isEqualTo(VALUE)
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")

        val VALUE = PredictionPositionsValue.Known(amount = BigDecimal("7.5"), source = StatusSource.ACTUAL)
    }
}