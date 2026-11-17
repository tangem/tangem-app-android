package com.tangem.data.tangempay.store

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType
import com.tangem.domain.tangempay.model.KycAdditionalInfoAccount
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class DefaultKycAdditionalInfoPopupStoreTest {

    private val paymentAccount = KycAdditionalInfoAccount(UserWalletId("1234"), AccountType.PAYMENT)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TryMarkShown {

        @Test
        fun `GIVEN empty store WHEN tryMarkShown THEN returns true`() = runTest {
            // Arrange
            val store = DefaultKycAdditionalInfoPopupStore()

            // Act
            val actual = store.tryMarkShown(paymentAccount)

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN account already marked WHEN tryMarkShown THEN returns false`() = runTest {
            // Arrange
            val store = DefaultKycAdditionalInfoPopupStore()
            store.tryMarkShown(paymentAccount)

            // Act
            val actual = store.tryMarkShown(paymentAccount)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN payment account marked WHEN tryMarkShown for money account THEN returns true`() = runTest {
            // Arrange
            val store = DefaultKycAdditionalInfoPopupStore()
            store.tryMarkShown(paymentAccount)

            // Act
            val actual = store.tryMarkShown(paymentAccount.copy(accountType = AccountType.MONEY))

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN account marked WHEN tryMarkShown for another wallet THEN returns true`() = runTest {
            // Arrange
            val store = DefaultKycAdditionalInfoPopupStore()
            store.tryMarkShown(paymentAccount)

            // Act
            val actual = store.tryMarkShown(paymentAccount.copy(userWalletId = UserWalletId("5678")))

            // Assert
            assertThat(actual).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Reset {

        @Test
        fun `GIVEN account marked WHEN reset THEN next tryMarkShown returns true`() = runTest {
            // Arrange
            val store = DefaultKycAdditionalInfoPopupStore()
            store.tryMarkShown(paymentAccount)

            // Act
            store.reset(paymentAccount)

            // Assert
            assertThat(store.tryMarkShown(paymentAccount)).isTrue()
        }

        @Test
        fun `GIVEN both accounts marked WHEN reset payment account THEN money account stays marked`() = runTest {
            // Arrange
            val store = DefaultKycAdditionalInfoPopupStore()
            val moneyAccount = paymentAccount.copy(accountType = AccountType.MONEY)
            store.tryMarkShown(paymentAccount)
            store.tryMarkShown(moneyAccount)

            // Act
            store.reset(paymentAccount)

            // Assert
            assertThat(store.tryMarkShown(moneyAccount)).isFalse()
        }
    }
}