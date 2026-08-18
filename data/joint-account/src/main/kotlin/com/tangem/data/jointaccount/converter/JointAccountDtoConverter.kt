package com.tangem.data.jointaccount.converter

import com.tangem.datasource.api.jointaccount.models.JointAccountDto
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import javax.inject.Inject
import javax.inject.Singleton

/** Network DTO → domain. Freshly fetched data is always [StatusSource.ACTUAL]. */
@Singleton
internal class JointAccountDtoConverter @Inject constructor() {

    fun convert(dto: JointAccountDto): JointAccount = JointAccount(
        cryptoAccountId = dto.cryptoAccountId,
        membersCount = dto.membersCount,
        threshold = dto.threshold,
        safeAddress = dto.safeAddress,
        status = JointAccountFieldsConverter.convertStatus(dto.status),
        members = dto.members.map { member ->
            JointAccount.Member(
                name = member.name,
                address = member.address,
                role = JointAccountFieldsConverter.convertRole(member.role),
            )
        },
        source = StatusSource.ACTUAL,
    )
}