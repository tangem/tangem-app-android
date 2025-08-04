plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.swap.v2.impl"
    packaging {
        resources {
            // To build and run composable preview
            merges += "paymentrequest.proto"
        }
    }
}


dependencies {
    /** Feature */
    implementation(projects.features.swapV2.api)
    implementation(projects.features.manageTokens.api)
    implementation(projects.features.sendV2.api)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.navigation)
    implementation(projects.core.configToggles)
    implementation(projects.core.datasource)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Libs */
    implementation(projects.libs.crypto)

    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.card)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.express.models)
    implementation(projects.domain.swap.models)
    implementation(projects.domain.swap)
    implementation(projects.domain.manageTokens.models)
    implementation(projects.domain.manageTokens)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.legacy)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.settings)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.notifications)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.feedback)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.androidx.activity.compose)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.coil)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.timber)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.coil)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}