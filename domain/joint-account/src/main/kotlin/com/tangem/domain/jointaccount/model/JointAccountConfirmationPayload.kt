package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName

/**
 * The object signed on confirmation — `payload` of `POST /v1/wallets/{walletId}/joint-accounts/confirm`.
 *
 * @property safeAddress strictly lower-case: the backend rebuilds this payload from the stored account and
 * compares the canonical JSON byte by byte
 */
data class JointAccountConfirmationPayload(
    val config: JointAccountConfig,
    val safeAddress: String,
) : JointAccountSignablePayload {

    override fun toCanonicalMap(): Map<String, Any> = config.toCanonicalMap() + ("safeAddress" to safeAddress)

    companion object {

        /** `null` when [account] carries the default main account name, which no joint account can */
        fun of(
            account: Account.Joint,
            jointAccount: JointAccount,
            safeAddress: String,
        ): JointAccountConfirmationPayload? {
            val name = (account.accountName as? AccountName.Custom)?.value ?: return null

            return JointAccountConfirmationPayload(
                config = JointAccountConfig(
                    name = name,
                    icon = account.icon.value.name,
                    iconColor = account.icon.color.name,
                    membersCount = jointAccount.membersCount,
                    threshold = jointAccount.threshold,
                ),
                safeAddress = safeAddress.lowercase(),
            )
        }
    }
}