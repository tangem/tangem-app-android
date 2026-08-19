package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Shared account config, immutable once the account exists. Part of the signed payload of both creation
 * ([CreateJointAccountRequest]) and joining ([JoinJointAccountRequest]); the invite preview
 * ([JointAccountInvitePreviewDto]) returns it verbatim for the joiner to copy into his payload.
 *
 * @property name         account name, 1..20 chars, not normalized
 * @property icon         icon name, from the same set as crypto accounts
 * @property iconColor    color name, from the same set as crypto accounts
 * @property membersCount slots including the creator, 2..5
 * @property threshold    signatures required to execute an operation, 1..[membersCount]
 */
@JsonClass(generateAdapter = true)
data class JointAccountConfigDto(
    @Json(name = "name") val name: String,
    @Json(name = "icon") val icon: String,
    @Json(name = "iconColor") val iconColor: String,
    @Json(name = "membersCount") val membersCount: Int,
    @Json(name = "threshold") val threshold: Int,
)