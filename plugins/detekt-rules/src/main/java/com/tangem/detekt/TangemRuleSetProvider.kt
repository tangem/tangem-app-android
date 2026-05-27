package com.tangem.detekt

import com.tangem.detekt.rules.UnsafeStringResourceUsage
import com.tangem.detekt.rules.UnsafeToHexStringUsage
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class TangemRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "tangem-rules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            id = ruleSetId,
            rules = listOf(
                UnsafeStringResourceUsage(config),
                UnsafeToHexStringUsage(config),
            )
        )
    }
}