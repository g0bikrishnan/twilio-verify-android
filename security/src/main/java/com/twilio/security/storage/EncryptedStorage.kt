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

package com.twilio.security.storage

import android.content.SharedPreferences
import android.util.Base64
import com.twilio.security.crypto.key.template.AESGCMNoPaddingCipherTemplate
import com.twilio.security.crypto.keyManager
import com.twilio.security.logger.Level
import com.twilio.security.logger.Logger
import com.twilio.security.storage.key.SecretKeyCipher
import com.twilio.security.storage.key.SecretKeyProvider
import java.security.MessageDigest
import kotlin.reflect.KClass

interface EncryptedStorage {
  val encryptionSecretKey: SecretKeyProvider
  val serializer: Serializer

  @Throws(StorageException::class)
  fun <T : Any> put(
    key: String,
    value: T
  )

  @Throws(StorageException::class)
  fun <T : Any> get(
    key: String,
    kClass: KClass<T>
  ): T

  @Throws(StorageException::class)
  fun <T : Any> getAll(
    kClass: KClass<T>
  ): List<T>

  fun contains(key: String): Boolean
  fun remove(key: String)
  fun clear()
}

fun encryptedPreferences(
  storageAlias: String,
  sharedPreferences: SharedPreferences
): EncryptedStorage {
  val keyManager = keyManager()
  val secretKeyProvider = SecretKeyCipher(
    AESGCMNoPaddingCipherTemplate(storageAlias), keyManager
  )
  if (!keyManager.contains(storageAlias) && sharedPreferences.all.isEmpty()) {
    secretKeyProvider.create()
  }
  return EncryptedPreferences(secretKeyProvider, sharedPreferences, DefaultSerializer())
}

internal fun generateKeyDigest(key: String): String {
  Logger.log(Level.Debug, "Generating key digest for $key")
  val messageDigest = MessageDigest.getInstance("SHA-256")
  return Base64.encodeToString(messageDigest.digest(key.toByteArray()), Base64.DEFAULT)
    .also { Logger.log(Level.Debug, "Generated key digest for $key: $it") }
}
