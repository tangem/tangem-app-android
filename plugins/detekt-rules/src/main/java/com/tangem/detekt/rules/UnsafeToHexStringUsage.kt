package com.tangem.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class UnsafeToHexStringUsage(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "UnsafeToHexStringUsage",
        severity = Severity.Warning,
        description = "Use one of the canonical `toHexString` extensions " +
            "(`com.tangem.common.extensions.toHexString` or `com.tangem.utils.extensions.toHexString`). " +
            "Other `toHexString` implementations (e.g. `okhttp3.internal.toHexString`, the experimental " +
            "stdlib one, custom locals) may disappear on dependency bumps or behave differently.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text != TO_HEX_STRING) return

        if (bindingContext == BindingContext.EMPTY) return

        val descriptor = expression.getResolvedCall(bindingContext)?.resultingDescriptor ?: return
        val fqName = descriptor.fqNameOrNull()?.asString() ?: return

        if (fqName in ALLOWED_FQNS) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Usage of `$fqName` is forbidden. Use one of: " +
                    ALLOWED_FQNS.joinToString { "`$it`" } + ".",
            ),
        )
    }

    private companion object {
        const val TO_HEX_STRING = "toHexString"
        val ALLOWED_FQNS = setOf(
            "com.tangem.common.extensions.toHexString",
            "com.tangem.utils.extensions.toHexString",
        )
    }
}