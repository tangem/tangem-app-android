package com.tangem.features.earn.staking.impl.di

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.staking.impl.PooledStakingEarnProduct
import dagger.multibindings.IntoSet
import org.junit.jupiter.api.Test

internal class PooledStakingEarnModuleTest {

    @Test
    fun `GIVEN module WHEN reading its bindings THEN PooledStakingEarnProduct is bound into the EarnProduct set`() {
        // Act
        val setBindings = PooledStakingEarnModule::class.java.declaredMethods
            .filter { it.isAnnotationPresent(IntoSet::class.java) }

        // Assert
        assertThat(setBindings.map { it.returnType to it.parameterTypes.toList() })
            .containsExactly(EarnProduct::class.java to listOf(PooledStakingEarnProduct::class.java))
    }
}