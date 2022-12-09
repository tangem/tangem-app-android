include(":app")
include(":domain")
include(":common")

// region Core modules
include(":core:datasource")
include(":core:res")
include(":core:utils")
include(":core:ui")
// endregion Core modules

// region Libs modules
include(":libs:crypto")
include(":libs:auth")
// endregion Libs modules

// region Feature modules
include(":features:referral:data")
include(":features:referral:domain")
include(":features:referral:presentation")
include(":features:swap:data")
include(":features:swap:domain")
include(":features:swap:presentation")
// endregion Feature modules
