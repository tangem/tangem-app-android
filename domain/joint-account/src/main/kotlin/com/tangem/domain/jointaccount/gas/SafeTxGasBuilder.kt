package com.tangem.domain.jointaccount.gas

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigInteger

/**
 * Assembles the [SafeTx] around an inner call. Everything the backend validates against the chain
 * (nonce, deployment state, gas estimate, gas price) is read right before signing — a stale value
 * earns a `NONCE_MISMATCH` / `GAS_PRICE_TOO_LOW` and a second tap.
 */
class SafeTxGasBuilder(
    private val networkReader: JointNetworkReader,
    private val refundReceiverProvider: JointRefundReceiverProvider,
) {

    suspend fun build(userWalletId: UserWalletId, network: Network, call: SafeInnerCall): Either<Throwable, SafeTx> =
        either {
            val refundReceiver = Either
                .catch { refundReceiverProvider.refundReceiver() }
                .bind()
                .lowercase()
            ensure(EVM_ADDRESS.matches(refundReceiver)) {
                IllegalStateException("The fee recipient is not an EVM address: $refundReceiver")
            }

            val code = networkReader.getCode(userWalletId = userWalletId, network = network).bind()
            val isDeployed = code.length > EMPTY_CODE.length

            val nonce = if (isDeployed) {
                networkReader.getContractNonce(userWalletId = userWalletId, network = network).bind()
            } else {
                BigInteger.ZERO
            }

            val estimate = networkReader
                .estimateGasLimit(
                    userWalletId = userWalletId,
                    network = network,
                    to = call.to,
                    value = call.value,
                    data = call.data,
                )
                .bind()

            val gasPrice = networkReader.getGasPrice(userWalletId = userWalletId, network = network).bind()

            SafeTx(
                to = call.to,
                value = call.value,
                data = call.data,
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = estimate.withSafeTxGasMargin(),
                baseGas = if (isDeployed) SAFE_BASE_GAS else SAFE_BASE_GAS + DEPLOY_GAS,
                gasPrice = gasPrice * GAS_PRICE_CAP_MULTIPLIER,
                gasToken = NATIVE_GAS_TOKEN,
                refundReceiver = refundReceiver,
                nonce = nonce,
            )
        }

    /** Rounded up, so an odd estimate never lands below the margin */
    private fun BigInteger.withSafeTxGasMargin(): BigInteger {
        val (quotient, remainder) = (this * SAFE_TX_GAS_MARGIN_NUMERATOR)
            .divideAndRemainder(SAFE_TX_GAS_MARGIN_DENOMINATOR)
        return if (remainder.signum() == 0) quotient else quotient + BigInteger.ONE
    }

    companion object {
        /**
         * What the Safe machinery itself burns per execution; validated by the backend as an exact match.
         * One shared value for native and token payloads.
         */
        val SAFE_BASE_GAS: BigInteger = BigInteger.valueOf(110_000)

        /** Added to `baseGas` when the first operation in the network also deploys the Safe */
        val DEPLOY_GAS: BigInteger = BigInteger.valueOf(300_000)

        /**
         * `address(0)` — the refund is paid in the native coin regardless of what is being sent.
         * Payment in an arbitrary token (as in gasless) is a later phase.
         */
        const val NATIVE_GAS_TOKEN = "0x0000000000000000000000000000000000000000"

        /** +50 % over the estimate: the inner call must not run out of gas mid-execution */
        private val SAFE_TX_GAS_MARGIN_NUMERATOR = BigInteger.valueOf(3)
        private val SAFE_TX_GAS_MARGIN_DENOMINATOR = BigInteger.valueOf(2)

        /** ×2 over the network estimate: the backend accepts down to 70 % of its own estimate */
        private val GAS_PRICE_CAP_MULTIPLIER = BigInteger.valueOf(2)

        private const val EMPTY_CODE = "0x"
        private val EVM_ADDRESS = Regex(pattern = "^0x[0-9a-f]{40}$")
    }
}