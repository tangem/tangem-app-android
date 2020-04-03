package com.tangem.tangemtest.ucase.resources

/**
[REDACTED_AUTHOR]
 */
open class Resources(
        val resName: Int,
        val resDescription: Int? = null
)

class ActionRes(
        resName: Int,
        resDescription: Int? = null,
        val resNavigation: Int? = null
) : Resources(resName, resDescription)