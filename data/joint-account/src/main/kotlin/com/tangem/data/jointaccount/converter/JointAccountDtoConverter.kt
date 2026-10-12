package com.tangem.data.jointaccount.converter

import com.tangem.store.datasource.jointaccount.models.JointAccountDto
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.utils.converter.Converter
import javax.inject.Inject
import javax.inject.Singleton

/** Network DTO → domain. Freshly fetched data is always [StatusSource.ACTUAL]. */
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
                inviteId = member.inviteId,
            )
        },
        source = StatusSource.ACTUAL,
        inFlightOperations = operationConverter.convertInFlight(dtos = value.operations.orEmpty()),
    )
}