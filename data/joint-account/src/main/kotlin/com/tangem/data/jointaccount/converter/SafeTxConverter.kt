package com.tangem.data.jointaccount.converter

import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.store.datasource.jointaccount.models.JointOperationDto
import com.tangem.utils.converter.TwoWayConverter
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SafeTxConverter @Inject constructor() :
    TwoWayConverter<JointOperationDto.SafeTxDto, SafeTx> {

    override fun convert(value: JointOperationDto.SafeTxDto): SafeTx = SafeTx(
        to = value.to.lowercase(),
        value = BigInteger(value.value),
        data = value.data.lowercase(),
        operation = value.operation,
        safeTxGas = BigInteger(value.safeTxGas),
        baseGas = BigInteger(value.baseGas),
        gasPrice = BigInteger(value.gasPrice),
        gasToken = value.gasToken.lowercase(),
        refundReceiver = value.refundReceiver.lowercase(),
        nonce = value.nonce.toContractNonce(),
    )

    /**
     * The backend validates the field as `^(0|[1-9]\d{0,17})$` — at most 18 digits, so it always fits the `Long`
     * the persisted copy keeps. A wider value is refused here, where only that operation is dropped, rather than at
     * the store, where the narrowing would take the whole fetch down.
     */
    private fun String.toContractNonce(): BigInteger {
        val nonce = BigInteger(this)
        require(nonce.signum() >= 0 && nonce <= MAX_CONTRACT_NONCE) {
            "nonce is outside the range the contract allows: $this"
        }

        return nonce
    }

    override fun convertBack(value: SafeTx): JointOperationDto.SafeTxDto = JointOperationDto.SafeTxDto(
        to = value.to,
        value = value.value.toString(),
        data = value.data,
        operation = value.operation,
        safeTxGas = value.safeTxGas.toString(),
        baseGas = value.baseGas.toString(),
        gasPrice = value.gasPrice.toString(),
        gasToken = value.gasToken,
        refundReceiver = value.refundReceiver,
        nonce = value.nonce.toString(),
    )

    private companion object {
        /** The largest value `^(0|[1-9]\d{0,17})$` can express */
        val MAX_CONTRACT_NONCE: BigInteger = BigInteger("999999999999999999")
    }
}