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
        nonce = BigInteger.valueOf(value.nonce),
    )

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
        // exact conversion: the wire contract pins nonce as an integer — fail fast rather than truncate
        nonce = value.nonce.longValueExactCompat(),
    )
}