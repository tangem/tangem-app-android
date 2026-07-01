package com.tangem.data.settings

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.settings.UsedeskTokenTtlManager
import com.tangem.test.core.getEmittedValues
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProdUsedeskTokenTtlManagerTest {

    private val manager = ProdUsedeskTokenTtlManager()

    @Test
    fun `WHEN getTokenTtlMillis THEN always emits default`() = runTest {
        val emitted = getEmittedValues(manager.getTokenTtlMillis())

        assertThat(emitted).containsExactly(UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS)
    }

    @Test
    fun `WHEN getTokenTtlMillisSync THEN returns default`() = runTest {
        assertThat(manager.getTokenTtlMillisSync()).isEqualTo(UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS)
    }

    @Test
    fun `GIVEN override attempt WHEN setTokenTtlMillis THEN sync still returns default`() = runTest {
        manager.setTokenTtlMillis(millis = 15L * 60 * 1000)

        assertThat(manager.getTokenTtlMillisSync()).isEqualTo(UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS)
    }
}