package com.tangem.detekt.rules

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

class UnsafeStringResourceUsage(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "UnsafeStringResourceUsage",
        severity = Severity.Security,
        description = "Avoid using stringResource directly in the code.",
        debt = Debt.FIVE_MINS,
    )

    val unsafeFunctionNames = listOf("stringResource", "pluralStringResource")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val functionName = expression.calleeExpression?.text
        if (functionName in unsafeFunctionNames) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Usage of `$functionName` is unsafe. Use the version of the function with the `Safe` " +
                        "suffix. For example, `${functionName}Safe`."
                )
            )
        }
    }
}