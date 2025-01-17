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

package com.twilio.verify.sample.view.factors.create

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.iid.FirebaseInstanceId
import com.twilio.verify.TwilioVerifyException
import com.twilio.verify.models.Factor
import com.twilio.verify.networking.NetworkException
import com.twilio.verify.sample.R
import com.twilio.verify.sample.model.CreateFactorData
import com.twilio.verify.sample.view.showError
import com.twilio.verify.sample.viewmodel.FactorError
import com.twilio.verify.sample.viewmodel.FactorViewModel
import kotlinx.android.synthetic.main.fragment_create_factor.accessTokenUrlInput
import kotlinx.android.synthetic.main.fragment_create_factor.content
import kotlinx.android.synthetic.main.fragment_create_factor.createFactorButton
import kotlinx.android.synthetic.main.fragment_create_factor.identityInput
import kotlinx.android.synthetic.main.fragment_create_factor.includePushTokenCheck
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateFactorFragment : Fragment() {

  private lateinit var token: String
  private val factorViewModel: FactorViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_create_factor, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    createFactorButton.setOnClickListener {
      startCreateFactor()
    }
    getPushToken()
    factorViewModel.getFactor()
      .observe(
        viewLifecycleOwner,
        Observer {
          createFactorButton.isEnabled = true
          when (it) {
            is com.twilio.verify.sample.viewmodel.Factor -> onSuccess(it.factor)
            is FactorError -> showError(it.exception)
          }
        }
      )
  }

  private fun getPushToken() {
    FirebaseInstanceId.getInstance()
      .instanceId.addOnCompleteListener(
        OnCompleteListener { task ->
          if (!task.isSuccessful) {
            task.exception?.let { it.showError(content) }
            return@OnCompleteListener
          }
          task.result?.token?.let {
            token = it
          }
        }
      )
  }

  private fun startCreateFactor() {
    hideKeyboardFrom()
    when {
      includePushTokenCheck.isChecked && !this::token.isInitialized ->
        IllegalArgumentException("Invalid push token").showError(content)
      identityInput.text.toString()
        .isEmpty() -> IllegalArgumentException("Invalid identity").showError(
        content
      )
      accessTokenUrlInput.text.toString()
        .isEmpty() || accessTokenUrlInput.text.toString()
        .toHttpUrlOrNull() == null ->
        IllegalArgumentException(
          "Invalid access token url"
        ).showError(
          content
        )
      else -> {
        createFactor(identityInput.text.toString(), accessTokenUrlInput.text.toString())
      }
    }
  }

  private fun createFactor(
    identity: String,
    accessTokenUrl: String
  ) {
    createFactorButton.isEnabled = false
    val pushToken = if (includePushTokenCheck.isChecked) token else null
    val metadata = if (!includePushTokenCheck.isChecked) mapOf("os" to "Android") else null
    val createFactorData =
      CreateFactorData(identity, "$identity's factor", accessTokenUrl, pushToken, metadata)
    factorViewModel.createFactor(createFactorData)
  }

  private fun onSuccess(factor: Factor) {
    Snackbar.make(content, "Factor ${factor.sid} created", LENGTH_SHORT)
      .show()
    findNavController().navigate(R.id.action_show_new_factor)
  }

  private fun hideKeyboardFrom() {
    val imm = activity?.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(content.windowToken, 0)
  }

  private fun showError(exception: Throwable) {
    when (exception) {
      is TwilioVerifyException -> handleNetworkException(exception as TwilioVerifyException)
      else -> exception.showError(content)
    }
  }

  private fun handleNetworkException(exception: TwilioVerifyException) {
    (exception.cause as? NetworkException)?.failureResponse?.apiError?.let {
      Snackbar.make(
        content,
        "Code: ${it.code} - ${it.message}",
        BaseTransientBottomBar.LENGTH_LONG
      )
        .show()
    } ?: exception.showError(content)
  }
}
