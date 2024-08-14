package com.tangem.datasource.api.utils

import java.util.concurrent.TimeUnit

/**
 * Set connect timeout of request
 *
 * @property duration duration
 * @property unit     unit
 *
 * @see "HttpClientExt.applyTimeoutAnnotations"
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
internal annotation class ConnectTimeout(val duration: Int, val unit: TimeUnit)

/**
 * Set read timeout of request
 *
 * @property duration duration
 * @property unit     unit
 *
 * @see "HttpClientExt.applyTimeoutAnnotations"
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
internal annotation class ReadTimeout(val duration: Int, val unit: TimeUnit)

/**
 * Set write timeout of request
 *
 * @property duration duration
 * @property unit     unit
 *
 * @see "HttpClientExt.applyTimeoutAnnotations"
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
internal annotation class WriteTimeout(val duration: Int, val unit: TimeUnit)
