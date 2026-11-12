package com.tangem.features.earn.staking.impl.di

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.product.api.product.EarnRateSource
import com.tangem.features.earn.staking.impl.StakingRateSource
import dagger.multibindings.IntoSet
import org.junit.jupiter.api.Test

internal class StakingRateSourceModuleTest {

    @Test
    fun `GIVEN module WHEN reading its bindings THEN StakingRateSource is bound into the EarnRateSource set`() {
        // Act
        val setBindings = StakingRateSourceModule::class.java.declaredMethods
            .filter { it.isAnnotationPresent(IntoSet::class.java) }

        // Assert
        assertThat(setBindings.map { it.returnType to it.parameterTypes.toList() })
            .containsExactly(EarnRateSource::class.java to listOf(StakingRateSource::class.java))
    }
}