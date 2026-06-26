package com.tangem.data.yield.supply

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.YieldSupplyError
import org.junit.jupiter.api.Test
import java.io.IOException

internal class DefaultYieldSupplyErrorResolverTest {

    @Test
    fun `GIVEN a YieldSupplyError WHEN resolve THEN returns the same instance`() {
        // Arrange
        val error = YieldSupplyError.DataError(IOException("boom"))

        // Act
        val result = DefaultYieldSupplyErrorResolver.resolve(error)

        // Assert
        assertThat(result).isSameInstanceAs(error)
    }

    @Test
    fun `GIVEN a generic throwable WHEN resolve THEN wraps it into DataError`() {
        // Arrange
        val throwable = IllegalStateException("unexpected")

        // Act
        val result = DefaultYieldSupplyErrorResolver.resolve(throwable)

        // Assert
        assertThat(result).isEqualTo(YieldSupplyError.DataError(throwable))
    }
}