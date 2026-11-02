package com.tangem.data.onramp

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultOnrampTransactionRepositoryTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val keys = GrowPreferencesKeys()
    private val preferences = createAppPreferencesStore(
        moshi = Moshi.Builder().add(BigDecimalAdapter()).build(),
        dispatchers = dispatchers,
        preferencesDataStore = MockStateDataStore(emptyPreferences()),
    )
    private val repository = DefaultOnrampTransactionRepository(preferences, keys, dispatchers)

    @Test
    fun `GIVEN transaction WHEN storeTransaction THEN transaction can be read by id`() = runTest {
        // Arrange
        val transaction = transaction(txId = "tx-1")

        // Act
        repository.storeTransaction(transaction)

        // Assert
        assertThat(repository.getTransactionById("tx-1")).isEqualTo(transaction)
    }

    @Test
    fun `GIVEN transaction with same id WHEN storeTransaction THEN existing transaction is replaced`() = runTest {
        // Arrange
        repository.storeTransaction(transaction(txId = "tx-1", status = OnrampStatus.Status.Created))

        // Act
        val updated = transaction(txId = "tx-1", status = OnrampStatus.Status.Finished)
        repository.storeTransaction(updated)

        // Assert
        assertThat(repository.getAllTransactions().first()).containsExactly(updated)
    }

    @Test
    fun `GIVEN transactions for several wallets and currencies WHEN getTransactions THEN matching transactions returned`() = runTest {
        // Arrange
        val matching = transaction(txId = "tx-1", walletId = WALLET_ID, currencyId = CURRENCY_ID.value)
        repository.storeTransaction(matching)
        repository.storeTransaction(transaction(txId = "tx-2", walletId = OTHER_WALLET_ID, currencyId = "ethereum"))
        repository.storeTransaction(transaction(txId = "tx-3", walletId = WALLET_ID, currencyId = "bitcoin"))

        // Act
        val actual = repository.getTransactions(
            WALLET_ID,
            CURRENCY_ID,
        )
            .first()

        // Assert
        assertThat(actual).containsExactly(matching)
    }

    @Test
    fun `GIVEN stored transaction WHEN updateTransactionStatus THEN status and external data are updated`() = runTest {
        // Arrange
        repository.storeTransaction(transaction(txId = "tx-1"))

        // Act
        repository.updateTransactionStatus(
            txId = "tx-1",
            externalTxId = "external-1",
            externalTxUrl = "https://provider/tx-1",
            status = OnrampStatus.Status.Finished,
        )

        // Assert
        assertThat(repository.getTransactionById("tx-1")).isEqualTo(
            transaction(
                txId = "tx-1",
                status = OnrampStatus.Status.Finished,
                externalTxId = "external-1",
                externalTxUrl = "https://provider/tx-1",
            ),
        )
    }

    @Test
    fun `GIVEN transaction WHEN removeTransaction THEN transaction is absent`() = runTest {
        // Arrange
        repository.storeTransaction(transaction(txId = "tx-1"))

        // Act
        repository.removeTransaction("tx-1")

        // Assert
        assertThat(repository.getTransactionById("tx-1")).isNull()
    }

    @Test
    fun `GIVEN unhandled transaction WHEN storeHandledTransaction THEN it is marked as handled`() = runTest {
        // Act
        repository.storeHandledTransaction("tx-1")

        // Assert
        assertThat(repository.isHandledTransaction("tx-1")).isTrue()
    }

    private fun transaction(
        txId: String,
        walletId: UserWalletId = WALLET_ID,
        currencyId: String = CURRENCY_ID.value,
        status: OnrampStatus.Status = OnrampStatus.Status.Created,
        externalTxId: String? = null,
        externalTxUrl: String? = null,
    ) = OnrampTransaction(
        txId = txId,
        userWalletId = walletId,
        fromAmount = BigDecimal("10.50"),
        fromCurrency = OnrampCurrency("US Dollar", "USD", null, 2, "$"),
        toAmount = BigDecimal("0.01"),
        toCurrencyId = currencyId,
        status = status,
        externalTxUrl = externalTxUrl,
        externalTxId = externalTxId,
        timestamp = 1L,
        providerName = "Mercuryo",
        providerImageUrl = "https://provider/image.png",
        providerType = "mercuryo",
        redirectUrl = "",
        paymentMethod = "card",
        residency = "USA",
    )

    private class BigDecimalAdapter {
        @FromJson fun fromJson(value: String) = BigDecimal(value)
        @ToJson fun toJson(value: BigDecimal) = value.toString()
    }

    private companion object {
        val WALLET_ID = UserWalletId("0011")
        val OTHER_WALLET_ID = UserWalletId("0022")
        val CURRENCY_ID = com.tangem.domain.models.currency.CryptoCurrency.ID.fromValue("coin⟨ethereum⟩ethereum")
    }
}