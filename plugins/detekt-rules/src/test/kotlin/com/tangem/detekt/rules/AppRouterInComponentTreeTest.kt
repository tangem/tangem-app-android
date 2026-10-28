package com.tangem.detekt.rules

import com.google.common.truth.Truth.assertThat
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.jetbrains.kotlin.psi.KtParameter
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppRouterInComponentTreeTest {

    private val rule = AppRouterInComponentTree(Config.empty)

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN class WHEN lint THEN AppRouter is reported only inside the component tree`(model: LintModel) {
        // Act
        val findings = rule.lint(model.code)

        // Assert
        val reportedParameters = findings.map { (it.entity.ktElement as? KtParameter)?.name }
        assertThat(reportedParameters).containsExactlyElementsIn(model.expectedParameters)
    }

    private fun provideTestModels() = listOf(
        LintModel(
            name = "model injecting AppRouter",
            code = """
                import com.tangem.common.routing.AppRouter
                class FooModel @Inject constructor(private val appRouter: AppRouter) : Model()
            """,
            expectedParameters = listOf("appRouter"),
        ),
        LintModel(
            name = "component delegating AppComponentContext",
            code = """
                import com.tangem.common.routing.AppRouter
                class DefaultFooComponent(
                    context: AppComponentContext,
                    private val appRouter: AppRouter,
                ) : AppComponentContext by context
            """,
            expectedParameters = listOf("appRouter"),
        ),
        LintModel(
            name = "component implementing a component interface",
            code = """
                import com.tangem.common.routing.AppRouter
                class DefaultFooComponent(private val appRouter: AppRouter?) : FooComponent
            """,
            expectedParameters = listOf("appRouter"),
        ),
        LintModel(
            name = "model injecting AppRouter through a secondary constructor",
            code = """
                import com.tangem.common.routing.AppRouter
                class FooModel : Model {
                    constructor(appRouter: AppRouter) : super()
                }
            """,
            expectedParameters = listOf("appRouter"),
        ),
        LintModel(
            name = "fully qualified AppRouter without import",
            code = """
                class FooModel(private val appRouter: com.tangem.common.routing.AppRouter) : Model()
            """,
            expectedParameters = listOf("appRouter"),
        ),
        LintModel(
            name = "model injecting contextual Router",
            code = """
                import com.tangem.core.decompose.navigation.Router
                class FooModel @Inject constructor(private val router: Router) : Model()
            """,
            expectedParameters = emptyList(),
        ),
        LintModel(
            name = "singleton outside the component tree",
            code = """
                import com.tangem.common.routing.AppRouter
                class LockTimer @Inject constructor(private val appRouter: AppRouter)
            """,
            expectedParameters = emptyList(),
        ),
        LintModel(
            name = "Android ViewModel",
            code = """
                import com.tangem.common.routing.AppRouter
                class FooViewModel @Inject constructor(private val appRouter: AppRouter) : ViewModel()
            """,
            expectedParameters = emptyList(),
        ),
        LintModel(
            name = "another AppRouter type",
            code = """
                import com.example.AppRouter
                class FooModel(private val appRouter: AppRouter) : Model()
            """,
            expectedParameters = emptyList(),
        ),
    )

    data class LintModel(val name: String, val code: String, val expectedParameters: List<String>) {
        override fun toString(): String = name
    }
}