package com.tangem.data.transaction.error

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.domain.transaction.error.GetFeeError
import org.junit.jupiter.api.Test

/**
 * Tests for [DefaultFeeErrorResolver] — the [Throwable] -> [GetFeeError] resolver. Mirrors the
 * mapping matrix of `ErrorsMapper.mapToFeeError` but driven through `resolve(throwable)`.
 *
 * Focuses on the [REDACTED_TASK_KEY] addition: [BlockchainSdkError.Ethereum.EstimateOverrideError] must be
 * resolved to [GetFeeError.EstimateOverrideError] field-by-field; representative other chains map
 * to their dedicated [GetFeeError.BlockchainErrors]; everything else falls through to
 * [GetFeeError.DataError].
 */
internal class DefaultFeeErrorResolverTest {

    private val resolver = DefaultFeeErrorResolver()

    @Test
    fun `GIVEN EstimateOverrideError THEN resolves to GetFeeError EstimateOverrideError field by field`() {
        val sdkError = BlockchainSdkError.Ethereum.EstimateOverrideError(
            blockchain = "ethereum",
            tokenSymbol = "USDT",
            rpcProvider = "infura",
            underlyingError = "execution reverted",
        )

        val result = resolver.resolve(sdkError)

        assertThat(result).isInstanceOf(GetFeeError.EstimateOverrideError::class.java)
        val mapped = result as GetFeeError.EstimateOverrideError
        assertThat(mapped.blockchain).isEqualTo("ethereum")
        assertThat(mapped.tokenSymbol).isEqualTo("USDT")
        assertThat(mapped.rpcProvider).isEqualTo("infura")
        assertThat(mapped.error).isEqualTo("execution reverted")
    }

    @Test
    fun `GIVEN TronActivationError THEN resolves to TronActivationError`() {
        // AccountActivationError is a class taking an int code, not an object.
        val result = resolver.resolve(BlockchainSdkError.Tron.AccountActivationError(code = 0))

        assertThat(result).isEqualTo(GetFeeError.BlockchainErrors.TronActivationError)
    }

    @Test
    fun `GIVEN KaspaZeroUtxoError THEN resolves to KaspaZeroUtxo`() {
        val result = resolver.resolve(BlockchainSdkError.Kaspa.ZeroUtxoError)

        assertThat(result).isEqualTo(GetFeeError.BlockchainErrors.KaspaZeroUtxo)
    }

    @Test
    fun `GIVEN SuiOneSuiRequired THEN resolves to SuiOneCoinRequired`() {
        val result = resolver.resolve(BlockchainSdkError.Sui.OneSuiRequired)

        assertThat(result).isEqualTo(GetFeeError.BlockchainErrors.SuiOneCoinRequired)
    }

    @Test
    fun `GIVEN unknown error THEN resolves to DataError preserving the throwable`() {
        val sdkError = BlockchainSdkError.CustomError("boom")

        val result = resolver.resolve(sdkError)

        assertThat(result).isInstanceOf(GetFeeError.DataError::class.java)
        assertThat((result as GetFeeError.DataError).cause).isEqualTo(sdkError)
    }
}