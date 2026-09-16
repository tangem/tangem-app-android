package com.tangem.grow.datasource.preferences.migration

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SwapCurrencyIdMigrationTest {

    private val keys = GrowPreferencesKeys()
    private val migration = SwapCurrencyIdMigration(keys)

    // region shouldMigrate

    @Test
    fun `shouldMigrate returns true when swap transactions key exists`() = runTest {
        val prefs = mutablePreferencesOf(keys.swapTransactionsKey to "[]")

        assertThat(migration.shouldMigrate(prefs)).isTrue()
    }

    @Test
    fun `shouldMigrate returns true when last swapped currency key exists`() = runTest {
        val prefs = mutablePreferencesOf(keys.lastSwappedCryptoCurrencyIdKey to "[]")

        assertThat(migration.shouldMigrate(prefs)).isTrue()
    }

    @Test
    fun `shouldMigrate returns true when both keys exist`() = runTest {
        val prefs = mutablePreferencesOf(
            keys.swapTransactionsKey to "[]",
            keys.lastSwappedCryptoCurrencyIdKey to "[]",
        )

        assertThat(migration.shouldMigrate(prefs)).isTrue()
    }

    @Test
    fun `shouldMigrate returns false when no keys exist`() = runTest {
        val prefs = mutablePreferencesOf()

        assertThat(migration.shouldMigrate(prefs)).isFalse()
    }

    // endregion

    // region migrate — coin IDs without derivation path

    @Test
    fun `migrates simple coin ID - ETH to ethereum`() = runTest {
        val result = migrateLastSwapped(currencyIdJson("coin${BS}ETH${BE}ethereum"))

        assertThat(result).isEqualTo(currencyIdJson("coin${BS}ethereum${BE}ethereum"))
    }

    @Test
    fun `migrates simple coin ID - BTC to bitcoin`() = runTest {
        val result = migrateLastSwapped(currencyIdJson("coin${BS}BTC${BE}bitcoin"))

        assertThat(result).isEqualTo(currencyIdJson("coin${BS}bitcoin${BE}bitcoin"))
    }

    @Test
    fun `migrates BSC to binance-smart-chain`() = runTest {
        val result = migrateLastSwapped(currencyIdJson("coin${BS}BSC${BE}binancecoin"))

        assertThat(result).isEqualTo(currencyIdJson("coin${BS}binance-smart-chain${BE}binancecoin"))
    }

    // endregion

    // region migrate — coin IDs with derivation path

    @Test
    fun `migrates coin ID with derivation path`() = runTest {
        val result = migrateLastSwapped(currencyIdJson("coin${BS}ETH${DP}12367123${BE}ethereum"))

        assertThat(result).isEqualTo(currencyIdJson("coin${BS}ethereum${DP}12367123${BE}ethereum"))
    }

    @Test
    fun `migrates POLYGON with derivation path`() = runTest {
        val result = migrateLastSwapped(currencyIdJson("coin${BS}POLYGON${DP}99999${BE}polygon-pos"))

        assertThat(result).isEqualTo(currencyIdJson("coin${BS}polygon-pos${DP}99999${BE}polygon-pos"))
    }

    // endregion

    // region migrate — token IDs

    @Test
    fun `migrates token ID with contract address`() = runTest {
        val result = migrateLastSwapped(currencyIdJson("token${BS}ETH${BE}usdt${CA}0xdAC17"))

        assertThat(result).isEqualTo(currencyIdJson("token${BS}ethereum${BE}usdt${CA}0xdAC17"))
    }

    @Test
    fun `migrates token ID with derivation path and contract address`() = runTest {
        val result = migrateLastSwapped(
            currencyIdJson("token${BS}ETH${DP}12345${BE}usdt${CA}0xdAC17"),
        )

        assertThat(result).isEqualTo(
            currencyIdJson("token${BS}ethereum${DP}12345${BE}usdt${CA}0xdAC17"),
        )
    }

    // endregion

    // region migrate — swap transactions (both from and to IDs)

    @Test
    fun `migrates both fromCryptoCurrencyId and toCryptoCurrencyId`() = runTest {
        val from = "coin${BS}ETH${BE}ethereum"
        val to = "coin${BS}BTC${BE}bitcoin"
        val oldJson = "[{\"fromCryptoCurrencyId\":\"$from\",\"toCryptoCurrencyId\":\"$to\"}]"

        val expectedFrom = "coin${BS}ethereum${BE}ethereum"
        val expectedTo = "coin${BS}bitcoin${BE}bitcoin"
        val expected = "[{\"fromCryptoCurrencyId\":\"$expectedFrom\",\"toCryptoCurrencyId\":\"$expectedTo\"}]"

        val result = migrateSwapTransactions(oldJson)

        assertThat(result).isEqualTo(expected)
    }

    // endregion

    // region migrate — no-op cases

    @Test
    fun `does not modify already migrated IDs`() = runTest {
        val json = currencyIdJson("coin${BS}ethereum${BE}ethereum")

        val result = migrateLastSwapped(json)

        assertThat(result).isEqualTo(json)
    }

    @Test
    fun `does not modify IDs where blockchain id equals networkId`() = runTest {
        val json = currencyIdJson("coin${BS}cosmos${BE}cosmos")

        val result = migrateLastSwapped(json)

        assertThat(result).isEqualTo(json)
    }

    @Test
    fun `does not modify empty list`() = runTest {
        val result = migrateLastSwapped("[]")

        assertThat(result).isEqualTo("[]")
    }

    // endregion

    // region migrate — multiple entries

    @Test
    fun `migrates multiple entries in list`() = runTest {
        val old1 = currencyIdValue("coin${BS}ETH${BE}ethereum")
        val old2 = currencyIdValue("coin${BS}TRON${BE}tron")
        val oldJson = "[$old1,$old2]"

        val new1 = currencyIdValue("coin${BS}ethereum${BE}ethereum")
        val new2 = currencyIdValue("coin${BS}tron${BE}tron")
        val expected = "[$new1,$new2]"

        val result = migrateLastSwapped(oldJson)

        assertThat(result).isEqualTo(expected)
    }

    // endregion

    // region migrate — preserves unrelated keys

    @Test
    fun `preserves unrelated preference keys`() = runTest {
        val unrelatedKey = stringPreferencesKey(name = "unrelated_key")
        val unrelatedValue = "some_value"
        val prefs = mutablePreferencesOf(
            keys.lastSwappedCryptoCurrencyIdKey to currencyIdJson("coin${BS}ETH${BE}ethereum"),
            unrelatedKey to unrelatedValue,
        )

        val result = migration.migrate(prefs)

        assertThat(result[unrelatedKey]).isEqualTo(unrelatedValue)
    }

    // endregion

    // region helpers

    private suspend fun migrateLastSwapped(json: String): String? {
        val prefs = mutablePreferencesOf(keys.lastSwappedCryptoCurrencyIdKey to json)
        val result = migration.migrate(prefs)
        return result[keys.lastSwappedCryptoCurrencyIdKey]
    }

    private suspend fun migrateSwapTransactions(json: String): String? {
        val prefs = mutablePreferencesOf(keys.swapTransactionsKey to json)
        val result = migration.migrate(prefs)
        return result[keys.swapTransactionsKey]
    }

    private fun currencyIdJson(id: String): String = "[${currencyIdValue(id)}]"

    private fun currencyIdValue(id: String): String = "{\"cryptoCurrencyId\":\"$id\"}"

    private companion object {
        const val BS = '\u27E8' // ⟨ body start
        const val BE = '\u27E9' // ⟩ body end
        const val DP = '\u2192' // → derivation path delimiter
        const val CA = '\u2693' // ⚓ contract address delimiter
    }

    // endregion
}