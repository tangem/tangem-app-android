package com.tangem.data.jointaccount.converter

import com.tangem.store.datasource.jointaccount.models.JointAccountDto
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.domain.models.StatusSource
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network DTO → domain. Freshly fetched data is always [StatusSource.ACTUAL]. An invite whose expiry cannot be read
 * is dropped alone: its slot then reads as free and offers a reissue, which is the recovery anyway.
 */
@Singleton
internal class JointAccountDtoConverter @Inject constructor(
    private val operationConverter: JointOperationConverter,
) : Converter<JointAccountDto, JointAccount> {

    override fun convert(value: JointAccountDto): JointAccount = JointAccount(
        cryptoAccountId = value.cryptoAccountId,
        membersCount = value.membersCount,
        threshold = value.threshold,
        safeAddress = value.safeAddress?.lowercase(),
        status = JointAccountFieldsConverter.convertStatus(value.status),
        members = value.members.map { member ->
            JointAccount.Member(
                name = member.name,
                address = member.address.lowercase(),
                role = JointAccountFieldsConverter.convertRole(member.role),
                slot = member.slot,
                signature = member.signature?.lowercase(),
            )
        },
        source = StatusSource.ACTUAL,
        invites = value.invites.orEmpty().mapNotNull(::convertInvite),
        inFlightOperations = operationConverter.convertInFlight(dtos = value.operations.orEmpty()),
    )

    private fun convertInvite(invite: JointAccountDto.Invite): JointAccountInvite? {
        val expiresAtMillis = try {
            OffsetDateTime.parse(invite.expiresAt).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            TangemLogger.e("Dropped the invite of slot ${invite.slot}: unreadable expiry", e)
            return null
        }

        return JointAccountInvite(slot = invite.slot, expiresAtMillis = expiresAtMillis)
    }
}