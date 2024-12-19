plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.ui"
}

dependencies {

    /** Compose */
    implementation(deps.compose.material3)
    implementation(deps.compose.material)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)

    /** Deps */
    implementation(deps.kotlin.immutable.collections)

    /** Project - Common */
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Project - Domain */
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.onramp.models)

    implementation(deps.tangem.card.core)
    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }
}