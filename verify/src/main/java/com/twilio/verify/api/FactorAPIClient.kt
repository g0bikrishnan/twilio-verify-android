/*
 * Copyright (c) 2020 Twilio Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twilio.verify.api

import android.content.Context
import com.twilio.security.logger.Level
import com.twilio.security.logger.Logger
import com.twilio.verify.TwilioVerifyException
import com.twilio.verify.TwilioVerifyException.ErrorCode.NetworkError
import com.twilio.verify.data.DateAdapter
import com.twilio.verify.data.DateProvider
import com.twilio.verify.domain.factor.models.CreateFactorPayload
import com.twilio.verify.domain.factor.models.UpdateFactorPayload
import com.twilio.verify.models.Factor
import com.twilio.verify.networking.Authentication
import com.twilio.verify.networking.BasicAuthorization
import com.twilio.verify.networking.HttpMethod.Delete
import com.twilio.verify.networking.HttpMethod.Post
import com.twilio.verify.networking.NetworkAdapter
import com.twilio.verify.networking.NetworkException
import com.twilio.verify.networking.NetworkProvider
import com.twilio.verify.networking.Request
import com.twilio.verify.networking.RequestHelper
import com.twilio.verify.storagePreferences
import org.json.JSONObject

internal const val SERVICE_SID_PATH = "{ServiceSid}"
internal const val FACTOR_SID_PATH = "{FactorSid}"
internal const val IDENTITY_PATH = "{Identity}"

internal const val AUTH_PAYLOAD_PARAM = "AuthPayload"

internal const val CREATE_FACTOR_URL = "Services/$SERVICE_SID_PATH/Entities/$IDENTITY_PATH/Factors"
internal const val VERIFY_FACTOR_URL =
  "Services/$SERVICE_SID_PATH/Entities/$IDENTITY_PATH/Factors/$FACTOR_SID_PATH"
internal const val UPDATE_FACTOR_URL =
  "Services/$SERVICE_SID_PATH/Entities/$IDENTITY_PATH/Factors/$FACTOR_SID_PATH"
internal const val DELETE_FACTOR_URL =
  "Services/$SERVICE_SID_PATH/Entities/$IDENTITY_PATH/Factors/$FACTOR_SID_PATH"

internal const val FRIENDLY_NAME_KEY = "FriendlyName"
internal const val FACTOR_TYPE_KEY = "FactorType"
internal const val BINDING_KEY = "Binding"
internal const val CONFIG_KEY = "Config"
internal const val METADATA_KEY = "Metadata"
internal const val AUTHENTICATION_USER = "token"

internal class FactorAPIClient(
  private val networkProvider: NetworkProvider = NetworkAdapter(),
  private val context: Context,
  private val authentication: Authentication,
  private val baseUrl: String,
  dateProvider: DateProvider = DateAdapter(
    storagePreferences(context)
  )
) : BaseAPIClient(dateProvider) {

  fun create(
    createFactorPayload: CreateFactorPayload,
    success: (response: JSONObject) -> Unit,
    error: (TwilioVerifyException) -> Unit
  ) {
    try {
      val requestHelper =
        RequestHelper(
          context,
          BasicAuthorization(AUTHENTICATION_USER, createFactorPayload.accessToken)
        )
      val request = Request.Builder(
        requestHelper,
        createFactorURL(createFactorPayload)
      )
        .httpMethod(Post)
        .body(createFactorBody(createFactorPayload))
        .build()
      networkProvider.execute(
        request,
        success = {
          success(JSONObject(it.body))
        },
        error = { exception ->
          error(TwilioVerifyException(exception, NetworkError))
        }
      )
    } catch (e: TwilioVerifyException) {
      error(e)
    } catch (e: Exception) {
      Logger.log(Level.Error, e.toString(), e)
      error(TwilioVerifyException(NetworkException(e), NetworkError))
    }
  }

  fun verify(
    factor: Factor,
    authPayload: String,
    success: (response: JSONObject) -> Unit,
    error: (TwilioVerifyException) -> Unit
  ) {
    fun verifyFactor(retries: Int = retryTimes) {
      try {
        val authToken = authentication.generateJWT(factor)
        val requestHelper =
          RequestHelper(
            context,
            BasicAuthorization(AUTHENTICATION_USER, authToken)
          )
        val request = Request.Builder(requestHelper, verifyFactorURL(factor))
          .httpMethod(Post)
          .body(verifyFactorBody(authPayload))
          .build()
        networkProvider.execute(
          request,
          {
            success(JSONObject(it.body))
          },
          { exception ->
            validateException(exception, ::verifyFactor, retries, error)
          }
        )
      } catch (e: TwilioVerifyException) {
        error(e)
      } catch (e: Exception) {
        Logger.log(Level.Error, e.toString(), e)
        error(TwilioVerifyException(NetworkException(e), NetworkError))
      }
    }
    verifyFactor()
  }

  fun update(
    factor: Factor,
    updateFactorPayload: UpdateFactorPayload,
    success: (response: JSONObject) -> Unit,
    error: (TwilioVerifyException) -> Unit
  ) {
    fun updateFactor(retries: Int = retryTimes) {
      try {
        val authToken = authentication.generateJWT(factor)
        val requestHelper =
          RequestHelper(
            context,
            BasicAuthorization(AUTHENTICATION_USER, authToken)
          )
        val request =
          Request.Builder(requestHelper, updateFactorURL(factor))
            .httpMethod(Post)
            .body(updateFactorBody(updateFactorPayload))
            .build()
        networkProvider.execute(
          request,
          {
            success(JSONObject(it.body))
          },
          { exception ->
            validateException(exception, ::updateFactor, retries, error)
          }
        )
      } catch (e: TwilioVerifyException) {
        error(e)
      } catch (e: Exception) {
        Logger.log(Level.Error, e.toString(), e)
        error(TwilioVerifyException(NetworkException(e), NetworkError))
      }
    }
    updateFactor()
  }

  fun delete(
    factor: Factor,
    success: () -> Unit,
    error: (TwilioVerifyException) -> Unit
  ) {
    fun deleteFactor(retries: Int = retryTimes) {
      try {
        val authToken = authentication.generateJWT(factor)
        val requestHelper =
          RequestHelper(
            context,
            BasicAuthorization(AUTHENTICATION_USER, authToken)
          )
        val request = Request.Builder(requestHelper, deleteFactorURL(factor))
          .httpMethod(Delete)
          .build()
        networkProvider.execute(
          request,
          {
            success()
          },
          { exception ->
            when (exception.failureResponse?.statusCode) {
              notFound -> success()
              unauthorized ->
                if (retries == 0) {
                  success()
                } else {
                  validateException(exception, ::deleteFactor, retries, error)
                }
              else -> validateException(exception, ::deleteFactor, retries, error)
            }
          }
        )
      } catch (e: TwilioVerifyException) {
        error(e)
      } catch (e: Exception) {
        Logger.log(Level.Error, e.toString(), e)
        error(TwilioVerifyException(NetworkException(e), NetworkError))
      }
    }
    deleteFactor()
  }

  private fun createFactorURL(createFactorPayload: CreateFactorPayload): String =
    "$baseUrl$CREATE_FACTOR_URL".replace(SERVICE_SID_PATH, createFactorPayload.serviceSid, true)
      .replace(IDENTITY_PATH, createFactorPayload.identity)

  private fun verifyFactorURL(factor: Factor): String =
    "$baseUrl$VERIFY_FACTOR_URL".replace(SERVICE_SID_PATH, factor.serviceSid, true)
      .replace(IDENTITY_PATH, factor.identity)
      .replace(FACTOR_SID_PATH, factor.sid)

  private fun deleteFactorURL(factor: Factor): String =
    "$baseUrl$DELETE_FACTOR_URL".replace(SERVICE_SID_PATH, factor.serviceSid, true)
      .replace(IDENTITY_PATH, factor.identity)
      .replace(FACTOR_SID_PATH, factor.sid)

  private fun updateFactorURL(
    factor: Factor
  ): String =
    "$baseUrl$UPDATE_FACTOR_URL".replace(SERVICE_SID_PATH, factor.serviceSid, true)
      .replace(IDENTITY_PATH, factor.identity)
      .replace(
        FACTOR_SID_PATH, factor.sid
      )

  private fun createFactorBody(
    createFactorPayload: CreateFactorPayload
  ): Map<String, String?> =
    mutableMapOf(
      FRIENDLY_NAME_KEY to createFactorPayload.friendlyName,
      FACTOR_TYPE_KEY to createFactorPayload.type.factorTypeName
    ).apply {
      putAll(createFactorPayload.binding.map { "$BINDING_KEY.${it.key}" to it.value })
      putAll(createFactorPayload.config.map { "$CONFIG_KEY.${it.key}" to it.value })
      createFactorPayload.metadata?.let { JSONObject(it) }?.let {
        put(METADATA_KEY, it.toString())
      }
    }

  private fun verifyFactorBody(
    authPayload: String
  ): Map<String, String?> =
    mapOf(AUTH_PAYLOAD_PARAM to authPayload)

  private fun updateFactorBody(
    updateFactorPayload: UpdateFactorPayload
  ): Map<String, String?> =
    mutableMapOf(
      FRIENDLY_NAME_KEY to updateFactorPayload.friendlyName
    ).apply {
      putAll(updateFactorPayload.config.map { "$CONFIG_KEY.${it.key}" to it.value })
    }
}
