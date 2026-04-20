package com.tangem.utils.annotations

/**
 * Marks code that should be removed when the specified feature toggle is cleaned up
 * by the `/cleanup-feature-toggles` skill.
 *
 * @property toggleName the name of the feature toggle (e.g., "GASLESS_APPROVAL_ENABLED")
 * @property description optional description of what should be done during cleanup
 *
[REDACTED_AUTHOR]
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.FILE,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.SOURCE)
annotation class RemoveWithToggle(
    val toggleName: String,
    val description: String = "",
)