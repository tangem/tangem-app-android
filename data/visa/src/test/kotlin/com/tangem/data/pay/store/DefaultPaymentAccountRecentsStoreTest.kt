package com.tangem.data.pay.store

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.spend.datasource.preferences.SpendPreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultPaymentAccountRecentsStoreTest {

    private var nowMillis = 100L
    private val spendPreferencesKeys = SpendPreferencesKeys()
    private val prefs = createAppPreferencesStore(
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )
    private val store = DefaultPaymentAccountRecentsStore(
        prefs = prefs,
        spendPreferencesKeys = spendPreferencesKeys,
        currentTimeMillis = { nowMillis },
    )

    @Test
    fun `GIVEN empty store WHEN getUsage THEN empty map`() = runTest {
        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN two pairs used WHEN getUsage THEN both timestamps kept`() = runTest {
        // Arrange
        nowMillis = 100L
        store.markUsed(WALLET_A, accountId = ACCOUNT_1, currencyId = COIN_ID)
        nowMillis = 200L
        store.markUsed(WALLET_A, accountId = ACCOUNT_2, currencyId = TOKEN_ID)

        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).containsExactly(ACCOUNT_1 to COIN_ID, 100L, ACCOUNT_2 to TOKEN_ID, 200L)
    }

    @Test
    fun `GIVEN the same pair used twice WHEN getUsage THEN latest timestamp wins`() = runTest {
        // Arrange
        nowMillis = 100L
        store.markUsed(WALLET_A, accountId = ACCOUNT_1, currencyId = COIN_ID)
        nowMillis = 300L
        store.markUsed(WALLET_A, accountId = ACCOUNT_1, currencyId = COIN_ID)

        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).containsExactly(ACCOUNT_1 to COIN_ID, 300L)
    }

    @Test
    fun `GIVEN usage on one wallet WHEN getUsage for another THEN empty`() = runTest {
        // Arrange
        store.markUsed(WALLET_A, accountId = ACCOUNT_1, currencyId = COIN_ID)

        // Act
        val actual = store.getUsage(WALLET_B)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN a malformed entry WHEN getUsage THEN it is dropped instead of throwing`() = runTest {
        // Arrange — the first row has a valid account id and a garbage currency id, so it reaches
        // CryptoCurrency.ID.fromValue; the second one is rejected earlier, by the account id validator.
        val key = spendPreferencesKeys.paymentAccountRecents(WALLET_A)
        prefs.editData { preferences ->
            preferences[key] = """
                {
                  "not-an-id":{"accountId":"${"a".repeat(64)}","usedAtMillis":1},
                  "coin⟨ethereum⟩ethereum":{"accountId":"not-hex","usedAtMillis":2}
                }
            """.trimIndent()
        }

        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN a malformed journal blob WHEN getUsage THEN empty map`() = runTest {
        // Arrange - the whole blob is not a map of usages, as a renamed or retyped entity would leave behind.
        val key = spendPreferencesKeys.paymentAccountRecents(WALLET_A)
        prefs.editData { preferences ->
            preferences[key] = """["coin⟨ethereum⟩ethereum"]"""
        }

        // Act
        val actual = store.getUsage(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN a malformed journal blob WHEN markUsed THEN the entry is written and getUsage returns it`() = runTest {
        // Arrange
        val key = spendPreferencesKeys.paymentAccountRecents(WALLET_A)
        prefs.editData { preferences ->
            preferences[key] = """["coin⟨ethereum⟩ethereum"]"""
        }
        nowMillis = 400L

        // Act
        store.markUsed(WALLET_A, accountId = ACCOUNT_1, currencyId = COIN_ID)

        // Assert
        assertThat(store.getUsage(WALLET_A)).containsExactly(ACCOUNT_1 to COIN_ID, 400L)
    }

    private companion object {
        val WALLET_A = UserWalletId("0123456789abcdef")
        val WALLET_B = UserWalletId("fedcba9876543210")

        val ACCOUNT_1: AccountId = AccountId
            .forCryptoPortfolio(userWalletId = WALLET_A, value = "a".repeat(64))
            .getOrNull()!!
        val ACCOUNT_2: AccountId = AccountId
            .forCryptoPortfolio(userWalletId = WALLET_A, value = "b".repeat(64))
            .getOrNull()!!

        val COIN_ID: CryptoCurrency.ID = CryptoCurrency.ID.fromValue("coin⟨ethereum⟩ethereum")
        val TOKEN_ID: CryptoCurrency.ID = CryptoCurrency.ID.fromValue("token⟨polygon-pos⟩usd-coin⚓0xUSDC")
    }
}