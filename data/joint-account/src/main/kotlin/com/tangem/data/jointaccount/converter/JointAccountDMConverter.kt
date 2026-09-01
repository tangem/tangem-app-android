package com.tangem.data.jointaccount.converter

import com.tangem.data.jointaccount.store.JointAccountDM
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain ↔ persisted form. Restored data is always [StatusSource.CACHE]. A status the build does not know is
 * persisted as `"unknown"` — it shows as [JointAccount.Status.UNKNOWN] until the next fetch, same as it did live.
 */
@Singleton
internal class JointAccountDMConverter @Inject constructor() {

    fun convert(account: JointAccount): JointAccountDM = JointAccountDM(
        cryptoAccountId = account.cryptoAccountId,
        membersCount = account.membersCount,
        threshold = account.threshold,
        safeAddress = account.safeAddress,
        status = account.status.toRawString(),
        members = account.members.map { member ->
            JointAccountDM.MemberDM(
                name = member.name,
                address = member.address,
                role = member.role.toRawString(),
            )
        },
    )

    fun convertBack(dm: JointAccountDM): JointAccount = JointAccount(
        cryptoAccountId = dm.cryptoAccountId,
        membersCount = dm.membersCount,
        threshold = dm.threshold,
        safeAddress = dm.safeAddress,
        status = JointAccountFieldsConverter.convertStatus(dm.status),
        members = dm.members.map { member ->
            JointAccount.Member(
                name = member.name,
                address = member.address,
                role = JointAccountFieldsConverter.convertRole(member.role),
            )
        },
        source = StatusSource.CACHE,
    )

    private fun JointAccount.Status.toRawString(): String = when (this) {
        JointAccount.Status.PENDING -> "pending"
        JointAccount.Status.CONFIRMING -> "confirming"
        JointAccount.Status.ACTIVE -> "active"
        JointAccount.Status.CANCELLED -> "cancelled"
        JointAccount.Status.UNKNOWN -> "unknown"
    }

    private fun JointAccount.Role.toRawString(): String = when (this) {
        JointAccount.Role.CREATOR -> "creator"
        JointAccount.Role.MEMBER -> "member"
        JointAccount.Role.UNKNOWN -> "unknown"
    }
}