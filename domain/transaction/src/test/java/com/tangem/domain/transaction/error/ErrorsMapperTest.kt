package com.tangem.domain.transaction.error

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import org.junit.Test

/**
 * Tests for [mapToFeeError] — the [Result.Failure] -> [GetFeeError] mapper. Focuses on the
 * [REDACTED_TASK_KEY] addition: [BlockchainSdkError.Ethereum.EstimateOverrideError] must be mapped to
 * [GetFeeError.EstimateOverrideError] field-by-field; all other errors fall through to
 * [GetFeeError.DataError].
 */
internal class ErrorsMapperTest {

    @Test
    fun `GIVEN EstimateOverrideError THEN maps to GetFeeError EstimateOverrideError field by field`() {
        val sdkError = BlockchainSdkError.Ethereum.EstimateOverrideError(
            blockchain = "ethereum",
            tokenSymbol = "USDT",
            rpcProvider = "infura",
            underlyingError = "execution reverted",
        )

        val result = Result.Failure(sdkError).mapToFeeError()

        assertThat(result).isInstanceOf(GetFeeError.EstimateOverrideError::class.java)
        val mapped = result as GetFeeError.EstimateOverrideError
        assertThat(mapped.blockchain).isEqualTo("ethereum")
        assertThat(mapped.tokenSymbol).isEqualTo("USDT")
        assertThat(mapped.rpcProvider).isEqualTo("infura")
        assertThat(mapped.error).isEqualTo("execution reverted")
    }

    @Test
    fun `GIVEN TronActivationError THEN maps to TronActivationError`() {
        // AccountActivationError is a class taking an int code, not an object.
        val result = Result.Failure(BlockchainSdkError.Tron.AccountActivationError(code = 0)).mapToFeeError()

        assertThat(result).isEqualTo(GetFeeError.BlockchainErrors.TronActivationError)
    }

    @Test
    fun `GIVEN KaspaZeroUtxoError THEN maps to KaspaZeroUtxo`() {
        val result = Result.Failure(BlockchainSdkError.Kaspa.ZeroUtxoError).mapToFeeError()

        assertThat(result).isEqualTo(GetFeeError.BlockchainErrors.KaspaZeroUtxo)
    }

    @Test
    fun `GIVEN SuiOneSuiRequired THEN maps to SuiOneCoinRequired`() {
        val result = Result.Failure(BlockchainSdkError.Sui.OneSuiRequired).mapToFeeError()

        assertThat(result).isEqualTo(GetFeeError.BlockchainErrors.SuiOneCoinRequired)
    }

    @Test
    fun `GIVEN unknown error THEN maps to DataError`() {
        val sdkError = BlockchainSdkError.CustomError("boom")

        val result = Result.Failure(sdkError).mapToFeeError()

        assertThat(result).isInstanceOf(GetFeeError.DataError::class.java)
        assertThat((result as GetFeeError.DataError).cause).isEqualTo(sdkError)
    }
}