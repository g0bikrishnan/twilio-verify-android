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

package com.twilio.verify.domain.service

import com.twilio.verify.TwilioVerifyException
import com.twilio.verify.api.ServiceAPIClient
import com.twilio.verify.models.Factor
import com.twilio.verify.models.Service
import org.json.JSONObject

internal class ServiceRepository(
  private val apiClient: ServiceAPIClient,
  private val serviceMapper: ServiceMapper = ServiceMapper()
) : ServiceProvider {

  override fun get(
    serviceSid: String,
    factor: Factor,
    success: (Service) -> Unit,
    error: (TwilioVerifyException) -> Unit
  ) {
    fun toService(response: JSONObject) {
      try {
        success(serviceMapper.fromApi(response))
      } catch (e: TwilioVerifyException) {
        error(e)
      }
    }
    apiClient.get(serviceSid, factor, ::toService, error)
  }
}
