package com.tangem.data.settings

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.getEmittedValues
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProdHotWalletRestrictionManagerTest {

    private val manager = ProdHotWalletRestrictionManager()

    @Test
    fun `WHEN isCreationEnabled THEN always emits true`() = runTest {
        val emitted = getEmittedValues(manager.isCreationEnabled())

        assertThat(emitted).containsExactly(true)
    }

    @Test
    fun `WHEN isCreationEnabledSync THEN returns true`() = runTest {
        assertThat(manager.isCreationEnabledSync()).isTrue()
    }

    @Test
    fun `WHEN toggleCreationEnabled THEN isCreationEnabledSync still returns true`() = runTest {
        manager.toggleCreationEnabled()
        assertThat(manager.isCreationEnabledSync()).isTrue()
    }
}