package com.tangem.domain.jointaccount.converter

import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.utils.converter.Converter

/**
 * The lifecycle state of a joint account as the wallet screen consumes it.
 *
 * [JointAccount.Status.CANCELLED] and [JointAccount.Status.UNKNOWN] degrade to
 * [JointAccountStatusValue.Error.Unavailable]: nothing on this stage sets `cancelled`, and an unknown value is one
 * this build does not know — showing such an account as operational would be the dangerous reading. So does an
 * active account without a Safe address: there is nothing to read its money by.
 */
object JointAccountStatusValueConverter : Converter<JointAccount, JointAccountStatusValue> {

    override fun convert(value: JointAccount): JointAccountStatusValue = when (value.status) {
        JointAccount.Status.PENDING -> JointAccountStatusValue.Pending(
            source = value.source,
            joinedMembers = value.members.size,
            totalMembers = value.membersCount,
        )
        JointAccount.Status.CONFIRMING -> JointAccountStatusValue.Confirming(
            source = value.source,
            totalMembers = value.membersCount,
        )
        JointAccount.Status.ACTIVE -> {
            val safeAddress = value.safeAddress

            if (safeAddress == null) {
                JointAccountStatusValue.Error.Unavailable
            } else {
                JointAccountStatusValue.Active(
                    source = value.source,
                    totalMembers = value.membersCount,
                    safeAddress = safeAddress,
                )
            }
        }
        JointAccount.Status.CANCELLED,
        JointAccount.Status.UNKNOWN,
        -> JointAccountStatusValue.Error.Unavailable
    }
}