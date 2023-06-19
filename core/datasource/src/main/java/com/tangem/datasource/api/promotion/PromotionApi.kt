package com.tangem.datasource.api.promotion

import com.tangem.datasource.api.promotion.models.*
import retrofit2.http.*

/**
 * @author Anton Zhilenkov on 05.06.2023.
 * Api details
 * https://www.notion.so/tangem/Promotion-Program-API-0907159c3fdb4975aac761be632f44da
 */
interface PromotionApi {

    /**
     * Получить статус и информацию по программе
     */
    @Headers("Cache-Control: max-age=$CACHE_CONTROL_SECONDS")
    @GET("promotion")
    suspend fun getPromotionInfo(@Query("programName") name: String): PromotionInfoResponse

    /**
     * Валидация при нажатии Learn&Earn, для нового пользователя
     * - проверяет есть ли такой код в БД и была ли сделана по нему покупка
     */
    @POST("promotion/code/validate")
    suspend fun codeValidate(@Body request: CodeValidateRequestBody): CodeValidateResponse

    /**
     * Создание задания для начисления выплаты по коду, для нового пользователя
     * - проверяет есть ли такой код в системе и была ли сделана по нему покупка
     * - проверяет были ли уже выплаты для этого кошелька
     * - проверяет были ли уже выплаты для этой карты
     * - если всё ок - создаёт задание на выплату
     */
    @POST("promotion/code/award")
    suspend fun codeAward(@Body request: CodeAwardRequestBody): CodeAwardResponse

    /**
     * Валидация при нажатии Learn&Earn для старого пользователя
     * - проверяет есть ли такой код в системе
     * - проверяет была ли сделана по нему покупка
     * - проверяет не был ли он уже использован
     */
    @POST("promotion/validate")
    suspend fun validate(@Body request: ValidateRequestBody): ValidateResponse

    /**
     * Создание задания для начисления выплаты для зарегистрированного (старого) пользователя
     * - проверяет были ли уже выплаты для этого кошелька
     * - проверяет были ли уже выплаты для этой карты
     * - если всё ок - создаёт задание на выплату
     */
    @GET("promotion/award")
    suspend fun award(@Body request: AwardRequestBody): AwardResponse

    companion object {
        const val ONE_INCH_TIMEOUT_MS = 5000L
        const val CACHE_CONTROL_SECONDS = 3600
    }
}
