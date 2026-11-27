package com.tangem.data.polymarket.entity

import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PolymarketPendingAuthorizationDTO(
    @SerialName("revocation") val revocation: Revocation,
    @SerialName("authorization") val authorization: Authorization,
    @SerialName("purpose") val purpose: PolymarketPendingAuthorization.Purpose,
) {

    @Serializable
    data class Revocation(
        @SerialName("owner_address") val ownerAddress: String,
        @SerialName("deposit_wallet_address") val depositWalletAddress: String,
        @SerialName("session_key_address") val sessionKeyAddress: String,
        @SerialName("nonce") val nonce: String,
        @SerialName("deadline") val deadline: String,
        @SerialName("call") val call: Call,
        @SerialName("signature") val signature: String,
    )

    @Serializable
    data class Authorization(
        @SerialName("owner_address") val ownerAddress: String,
        @SerialName("deposit_wallet_address") val depositWalletAddress: String,
        @SerialName("session_key_address") val sessionKeyAddress: String,
        @SerialName("device_id") val deviceId: String,
        @SerialName("device_name") val deviceName: String,
        @SerialName("valid_until") val validUntil: String,
        @SerialName("nonce") val nonce: String,
        @SerialName("deadline") val deadline: String,
        @SerialName("call") val call: Call,
        @SerialName("signature") val signature: String,
        @SerialName("replaces_session_key_address") val replacesSessionKeyAddress: String? = null,
    )

    @Serializable
    data class Call(
        @SerialName("target") val target: String,
        @SerialName("value") val value: String,
        @SerialName("data") val data: String,
    )
}

internal fun PolymarketPendingAuthorization.toDTO(): PolymarketPendingAuthorizationDTO =
    PolymarketPendingAuthorizationDTO(
        revocation = PolymarketPendingAuthorizationDTO.Revocation(
            ownerAddress = revocation.ownerAddress,
            depositWalletAddress = revocation.depositWalletAddress,
            sessionKeyAddress = revocation.sessionKeyAddress,
            nonce = revocation.nonce,
            deadline = revocation.deadline,
            call = revocation.call.toDTO(),
            signature = revocation.signature,
        ),
        authorization = PolymarketPendingAuthorizationDTO.Authorization(
            ownerAddress = authorization.ownerAddress,
            depositWalletAddress = authorization.depositWalletAddress,
            sessionKeyAddress = authorization.sessionKeyAddress,
            deviceId = authorization.deviceId,
            deviceName = authorization.deviceName,
            validUntil = authorization.validUntil,
            nonce = authorization.nonce,
            deadline = authorization.deadline,
            call = authorization.call.toDTO(),
            signature = authorization.signature,
            replacesSessionKeyAddress = authorization.replacesSessionKeyAddress,
        ),
        purpose = purpose,
    )

internal fun PolymarketPendingAuthorizationDTO.toDomain(): PolymarketPendingAuthorization =
    PolymarketPendingAuthorization(
        revocation = PolymarketSessionKeyRevocation(
            ownerAddress = revocation.ownerAddress,
            depositWalletAddress = revocation.depositWalletAddress,
            sessionKeyAddress = revocation.sessionKeyAddress,
            nonce = revocation.nonce,
            deadline = revocation.deadline,
            call = revocation.call.toDomain(),
            signature = revocation.signature,
        ),
        authorization = PolymarketSessionKeyAuthorization(
            ownerAddress = authorization.ownerAddress,
            depositWalletAddress = authorization.depositWalletAddress,
            sessionKeyAddress = authorization.sessionKeyAddress,
            deviceId = authorization.deviceId,
            deviceName = authorization.deviceName,
            validUntil = authorization.validUntil,
            nonce = authorization.nonce,
            deadline = authorization.deadline,
            call = authorization.call.toDomain(),
            signature = authorization.signature,
            replacesSessionKeyAddress = authorization.replacesSessionKeyAddress,
        ),
        purpose = purpose,
    )

private fun PolymarketApprovalCall.toDTO() = PolymarketPendingAuthorizationDTO.Call(
    target = target,
    value = value,
    data = data,
)

private fun PolymarketPendingAuthorizationDTO.Call.toDomain() = PolymarketApprovalCall(
    target = target,
    value = value,
    data = data,
)