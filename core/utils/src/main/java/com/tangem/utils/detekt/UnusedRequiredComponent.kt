package com.tangem.utils.detekt

/**
 * Annotation for an unused component that should be ignored by Detekt
 *
 * @property description description of why I shouldn't remove it
 *
 * @author Andrew Khokhlov on 16/01/2023
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.TYPE_PARAMETER,
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
    AnnotationTarget.TYPEALIAS
)
@Retention(AnnotationRetention.SOURCE)
annotation class UnusedRequiredComponent(val description: String)
