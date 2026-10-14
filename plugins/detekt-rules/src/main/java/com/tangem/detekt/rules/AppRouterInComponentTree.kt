package com.tangem.detekt.rules

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

/**
 * Reports `AppRouter` injected into a Decompose model or component. Inside the component tree navigation goes through
 * the contextual `Router`, so nested stacks and slots receive their own navigation.
 */
class AppRouterInComponentTree(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "AppRouterInComponentTree",
        severity = Severity.Maintainability,
        description = "Components and models must navigate via the contextual Router instead of AppRouter.",
        debt = Debt.TWENTY_MINS,
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        if (!klass.isModel() && !klass.isComponent()) return

        val isAppRouterImported = klass.containingKtFile.importDirectives
            .any { it.importedFqName?.asString() == APP_ROUTER_FQ_NAME }

        klass.primaryConstructorParameters
            .filter { parameter -> parameter.isAppRouter(isAppRouterImported) }
            .forEach { parameter ->
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(parameter),
                        message = "`${klass.name}` injects AppRouter. Components and models navigate via the " +
                            "contextual `Router`; AppRouter is for code outside the component tree.",
                    ),
                )
            }
    }

    private fun KtClass.isModel(): Boolean = superTypeListEntries.any { entry ->
        entry is KtSuperTypeCallEntry && entry.typeReference?.text == MODEL
    }

    private fun KtClass.isComponent(): Boolean = superTypeListEntries.any { entry ->
        val typeName = entry.typeReference?.text.orEmpty()

        (entry is KtDelegatedSuperTypeEntry && typeName == APP_COMPONENT_CONTEXT) || typeName.endsWith(COMPONENT)
    }

    private fun KtParameter.isAppRouter(isAppRouterImported: Boolean): Boolean {
        val typeName = typeReference?.text?.removeSuffix("?") ?: return false

        return typeName == APP_ROUTER_FQ_NAME || (isAppRouterImported && typeName == APP_ROUTER)
    }

    private companion object {
        const val APP_ROUTER = "AppRouter"
        const val APP_ROUTER_FQ_NAME = "com.tangem.common.routing.AppRouter"
        const val MODEL = "Model"
        const val APP_COMPONENT_CONTEXT = "AppComponentContext"
        const val COMPONENT = "Component"
    }
}