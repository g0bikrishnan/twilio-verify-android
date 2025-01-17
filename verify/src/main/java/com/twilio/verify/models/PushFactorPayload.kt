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

package com.twilio.verify.models

/**
 * Describes the information required to create a Factor which type is Push
 *
 * @property friendlyName A human readable description of this resource, up to 64 characters. For a push factor, this can be the device's name.
 * @property serviceSid Service sid
 * @property identity Identifies the user, should be an UUID you should not use PII (Personal Identifiable Information)
 * because the systems that will process this attribute assume it is not directly identifying information
 * @property pushToken (Optional) Registration token generated by the FCM SDK on initial startup of your app.
 *                      Sending null or empty will disable sending push notifications for challenges associated to this factor
 * @property accessToken Previously generated Access Token using the /accessTokens endpoint
 * @property metadata Custom metadata associated with the factor. This is added by the Device/SDK directly to allow for the inclusion of device information.
 * @property factorType Type of the factor
 * @see FactorType
 */
class PushFactorPayload(
  override val friendlyName: String,
  override val serviceSid: String,
  override val identity: String,
  val pushToken: String?,
  val accessToken: String,
  val metadata: Map<String, String>? = null
) : FactorPayload {
  override val factorType: FactorType
    get() = FactorType.PUSH
}
