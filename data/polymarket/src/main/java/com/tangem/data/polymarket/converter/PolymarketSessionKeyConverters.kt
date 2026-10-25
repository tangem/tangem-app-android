package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketSessionSignersResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketApprovalCallDto
import com.tangem.grow.datasource.polymarket.models.PolymarketAuthorizeSessionKeyRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketRevokeSessionKeyRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketSessionKeyDto
import com.tangem.utils.converter.Converter
import java.time.Instant

internal object PolymarketSessionKeyConverter : Converter<PolymarketSessionKeyDto, PolymarketSessionKey> {

    override fun convert(value: PolymarketSessionKeyDto): PolymarketSessionKey = PolymarketSessionKey(
        address = value.address.lowercase(),
        deviceId = value.deviceId,
        deviceName = value.deviceName,
        isEnabled = value.isEnabled,
        status = PolymarketSessionKeyStatus.fromRaw(value.status),
        scopes = value.scopes,
        validUntil = value.validUntil?.let(Instant::ofEpochSecond),
        renewAfter = value.renewAfter?.let(Instant::ofEpochSecond),
        authorizedAt = value.authorizedAt?.let(Instant::ofEpochSecond),
        createdAt = Instant.ofEpochSecond(value.createdAt),
    )
}

internal object PolymarketSessionSignersConverter :
    Converter<PolymarketSessionSignersResponse, List<PolymarketSessionSigner>> {

    override fun convert(value: PolymarketSessionSignersResponse): List<PolymarketSessionSigner> =
        value.signers.orEmpty().map { signer ->
            PolymarketSessionSigner(
                address = signer.address.lowercase(),
                scopes = signer.scopes.orEmpty(),
                validUntil = signer.validUntil?.let(Instant::ofEpochSecond),
            )
        }
}

internal object PolymarketAuthorizeSessionKeyRequestConverter :
    Converter<PolymarketSessionKeyAuthorization, PolymarketAuthorizeSessionKeyRequest> {

    override fun convert(value: PolymarketSessionKeyAuthorization) = PolymarketAuthorizeSessionKeyRequest(
        ownerAddress = value.ownerAddress,
        depositWalletAddress = value.depositWalletAddress,
        sessionKeyAddress = value.sessionKeyAddress,
        deviceId = value.deviceId,
        deviceName = value.deviceName,
        validUntil = value.validUntil,
        nonce = value.nonce,
        deadline = value.deadline,
        calls = listOf(value.call.toDto()),
        signature = value.signature,
        replacesSessionKeyAddress = value.replacesSessionKeyAddress,
    )
}

internal object PolymarketRevokeSessionKeyRequestConverter :
    Converter<PolymarketSessionKeyRevocation, PolymarketRevokeSessionKeyRequest> {

    override fun convert(value: PolymarketSessionKeyRevocation) = PolymarketRevokeSessionKeyRequest(
        ownerAddress = value.ownerAddress,
        depositWalletAddress = value.depositWalletAddress,
        sessionKeyAddress = value.sessionKeyAddress,
        nonce = value.nonce,
        deadline = value.deadline,
        calls = listOf(value.call.toDto()),
        signature = value.signature,
    )
}

private fun PolymarketApprovalCall.toDto() = PolymarketApprovalCallDto(target = target, value = value, data = data)