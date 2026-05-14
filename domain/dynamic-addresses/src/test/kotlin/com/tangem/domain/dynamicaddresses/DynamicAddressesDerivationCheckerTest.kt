package com.tangem.domain.dynamicaddresses

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamicAddressesDerivationCheckerTest {

    // region Conflicting: same account, non-zero change or index

    @Test
    fun `same account, non-zero address index`() {
        val result = check(custom = "m/44'/5'/0'/0/1", base = "m/44'/5'/0'/0/0")
        assertThat(result).isTrue()
    }

    @Test
    fun `same account, non-zero change`() {
        val result = check(custom = "m/44'/5'/0'/1/0", base = "m/44'/5'/0'/0/0")
        assertThat(result).isTrue()
    }

    @Test
    fun `same account, both change and index non-zero`() {
        val result = check(custom = "m/44'/5'/0'/1/5", base = "m/44'/5'/0'/0/0")
        assertThat(result).isTrue()
    }

    @Test
    fun `same account, large address index`() {
        val result = check(custom = "m/44'/5'/0'/0/8", base = "m/44'/5'/0'/0/0")
        assertThat(result).isTrue()
    }

    @Test
    fun `bitcoin BIP84, non-zero index`() {
        val result = check(custom = "m/84'/0'/0'/0/1", base = "m/84'/0'/0'/0/0")
        assertThat(result).isTrue()
    }

    @Test
    fun `litecoin BIP84, non-zero change`() {
        val result = check(custom = "m/84'/2'/0'/1/0", base = "m/84'/2'/0'/0/0")
        assertThat(result).isTrue()
    }

    @Test
    fun `dogecoin, non-zero index`() {
        val result = check(custom = "m/44'/3'/0'/0/8", base = "m/44'/3'/0'/0/0")
        assertThat(result).isTrue()
    }

    @Test
    fun `bitcoin cash, non-zero index`() {
        val result = check(custom = "m/44'/145'/0'/0/3", base = "m/44'/145'/0'/0/0")
        assertThat(result).isTrue()
    }

    // endregion

    // region Not conflicting: different account

    @Test
    fun `different account index, non-zero address index`() {
        val result = check(custom = "m/44'/5'/1'/0/1", base = "m/44'/5'/0'/0/0")
        assertThat(result).isFalse()
    }

    @Test
    fun `different account index, zero change and index`() {
        val result = check(custom = "m/44'/5'/1'/0/0", base = "m/44'/5'/0'/0/0")
        assertThat(result).isFalse()
    }

    @Test
    fun `different coin type`() {
        val result = check(custom = "m/44'/0'/0'/0/1", base = "m/44'/5'/0'/0/0")
        assertThat(result).isFalse()
    }

    @Test
    fun `different purpose`() {
        val result = check(custom = "m/84'/5'/0'/0/1", base = "m/44'/5'/0'/0/0")
        assertThat(result).isFalse()
    }

    @Test
    fun `BIP44 custom against BIP84 base`() {
        val result = check(custom = "m/44'/0'/0'/0/1", base = "m/84'/0'/0'/0/0")
        assertThat(result).isFalse()
    }

    // endregion

    // region Not conflicting: same account, zero change and index

    @Test
    fun `identical paths`() {
        val result = check(custom = "m/44'/5'/0'/0/0", base = "m/44'/5'/0'/0/0")
        assertThat(result).isFalse()
    }

    @Test
    fun `identical bitcoin BIP84 paths`() {
        val result = check(custom = "m/84'/0'/0'/0/0", base = "m/84'/0'/0'/0/0")
        assertThat(result).isFalse()
    }

    // endregion

    // region Edge cases: invalid or incomplete paths

    @Test
    fun `invalid custom path`() {
        val result = check(custom = "invalid", base = "m/44'/5'/0'/0/0")
        assertThat(result).isFalse()
    }

    @Test
    fun `invalid base path`() {
        val result = check(custom = "m/44'/5'/0'/0/1", base = "not_a_path")
        assertThat(result).isFalse()
    }

    @Test
    fun `empty custom path`() {
        val result = check(custom = "", base = "m/44'/5'/0'/0/0")
        assertThat(result).isFalse()
    }

    @Test
    fun `empty base path`() {
        val result = check(custom = "m/44'/5'/0'/0/1", base = "")
        assertThat(result).isFalse()
    }

    @Test
    fun `both paths invalid`() {
        val result = check(custom = "abc", base = "xyz")
        assertThat(result).isFalse()
    }

    @Test
    fun `custom path with fewer than 5 nodes`() {
        val result = check(custom = "m/44'/5'/0'", base = "m/44'/5'/0'/0/0")
        assertThat(result).isFalse()
    }

    @Test
    fun `base path with fewer than 5 nodes`() {
        val result = check(custom = "m/44'/5'/0'/0/1", base = "m/44'/5'")
        assertThat(result).isFalse()
    }

    // endregion

    // region isBaseDerivation

    @Test
    fun `isBaseDerivation - standard BIP44 base path`() {
        assertThat(DynamicAddressesDerivationChecker.isBaseDerivation("m/44'/5'/0'/0/0")).isTrue()
    }

    @Test
    fun `isBaseDerivation - BIP84 base path`() {
        assertThat(DynamicAddressesDerivationChecker.isBaseDerivation("m/84'/0'/0'/0/0")).isTrue()
    }

    @Test
    fun `isBaseDerivation - non-zero index`() {
        assertThat(DynamicAddressesDerivationChecker.isBaseDerivation("m/44'/5'/0'/0/1")).isFalse()
    }

    @Test
    fun `isBaseDerivation - non-zero change`() {
        assertThat(DynamicAddressesDerivationChecker.isBaseDerivation("m/44'/5'/0'/1/0")).isFalse()
    }

    @Test
    fun `isBaseDerivation - non-zero account with zero change and index`() {
        assertThat(DynamicAddressesDerivationChecker.isBaseDerivation("m/44'/5'/2'/0/0")).isTrue()
    }

    @Test
    fun `isBaseDerivation - invalid path`() {
        assertThat(DynamicAddressesDerivationChecker.isBaseDerivation("invalid")).isFalse()
    }

    @Test
    fun `isBaseDerivation - too few nodes`() {
        assertThat(DynamicAddressesDerivationChecker.isBaseDerivation("m/44'/5'")).isFalse()
    }

    // endregion

    private fun check(custom: String, base: String): Boolean {
        return DynamicAddressesDerivationChecker.hasSameAccountWithNonZeroChangeOrIndex(
            customPath = custom,
            basePath = base,
        )
    }
}