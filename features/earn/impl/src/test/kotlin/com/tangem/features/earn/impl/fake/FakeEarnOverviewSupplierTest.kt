package com.tangem.features.earn.impl.fake

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnOverviews
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class FakeEarnOverviewSupplierTest {

    private val userWalletId = UserWalletId("0123456789abcdef")
    private val ethId = CryptoCurrency.ID.fromValue("coin⟨ETH⟩ethereum")

    private val supplier = FakeEarnOverviewSupplier()

    @Test
    fun `GIVEN two different wallets WHEN supplier invoked THEN the same overviews are emitted`() = runTest {
        // Arrange
        val otherUserWalletId = UserWalletId("fedcba9876543210")

        // Act
        val first = supplier(userWalletId).first()
        val second = supplier(otherUserWalletId).first()

        // Assert
        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `GIVEN default supplier WHEN invoked THEN ETH has headline and banner and the source is actual`() = runTest {
        // Act
        val actual = supplier(userWalletId).first()

        // Assert
        val overview = actual[ethId]
        assertThat(overview?.headline).isNotNull()
        assertThat(overview?.banner).isNotNull()
        assertThat(actual.source).isEqualTo(StatusSource.ACTUAL)
    }

    @Test
    fun `GIVEN supplier with custom overviews WHEN invoked THEN they are emitted as they are`() = runTest {
        // Arrange
        val customSupplier = FakeEarnOverviewSupplier(overviews = EarnOverviews.Empty)

        // Act
        val actual = customSupplier(userWalletId).first()

        // Assert
        assertThat(actual).isEqualTo(EarnOverviews.Empty)
    }
}