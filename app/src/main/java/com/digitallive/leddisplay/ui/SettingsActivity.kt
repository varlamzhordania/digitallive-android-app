package com.digitallive.leddisplay.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.core.content.ContextCompat
import com.digitallive.leddisplay.R
import com.digitallive.leddisplay.databinding.ActivitySettingsBinding
import com.digitallive.leddisplay.utils.DataManager
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private var loadingDialog: LoadingDialog? = null
    private var isWsServerUrlVisible = false
    private var isDisplaySecretKeyVisible = false
    private var isLogServerUrlVisible = false
    private var isDisplayTokenVisible = false
    private var tickerBgColor = DataManager.getTickerBackgroundColor()
    private var tickerTextColor = DataManager.getTickerTextColor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingDialog = LoadingDialog(this)

        setListeners()
        setSavedData()

        binding.save.setOnClickListener {
            if (!validateInput()) return@setOnClickListener

            loadingDialog?.show(false)

            val wsServerUrl = binding.wsServerUrl.text.toString().trim()
            val displaySecretKey = binding.displaySecretKey.text.toString().trim()
            val logServerUrl = binding.logServerUrl.text.toString().trim()
            val displayToken = binding.displayToken.text.toString().trim()
            val retryInterval = binding.retryInterval.text.toString().trim().toInt()
            val showError = binding.showErrorAsToast.isChecked

            DataManager.saveWebSocketServerUrl(wsServerUrl)
            DataManager.saveDisplaySecretKey(displaySecretKey)
            DataManager.saveLogServerUrl(logServerUrl)
            DataManager.saveDisplayToken(displayToken)
            DataManager.saveRetryInterval(retryInterval)
            DataManager.saveShowError(showError)
            DataManager.saveTickerBackgroundColor(tickerBgColor)
            DataManager.saveTickerTextColor(tickerTextColor)

            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                loadingDialog?.hide()

                startActivity(
                    Intent(this@SettingsActivity, VideoActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                )
            }
        }
    }

    private fun setSavedData() {
        val wsServerUrl = DataManager.getWebSocketServerUrl()
        val displaySecretKey = DataManager.getDisplaySecretKey()
        val logServerUrl = DataManager.getLogServerUrl()
        val displayToken = DataManager.getDisplayToken()
        val retryInterval = DataManager.getRetryInterval()
        val showError = DataManager.getShowError()

        binding.wsServerUrl.setText(wsServerUrl)
        binding.displaySecretKey.setText(displaySecretKey)
        binding.logServerUrl.setText(logServerUrl)
        binding.displayToken.setText(displayToken)
        binding.retryInterval.setText(retryInterval.toString())
        binding.showErrorAsToast.isChecked = showError
        binding.tickerBackgroundAlphaTileView.setPaintColor(tickerBgColor)
        binding.tickerTextAlphaTileView.setPaintColor(tickerTextColor)
    }

    private fun setListeners() {
        binding.generalTab.setOnClickListener {
            binding.generalSettingsContainer.visibility = View.VISIBLE
            binding.advancedSettingsContainer.visibility = View.GONE

            binding.generalTab.background = ContextCompat.getDrawable(this, R.drawable.tab_bg_sel)
            binding.advancedTab.background = ContextCompat.getDrawable(this, R.drawable.tab_bg)
        }

        binding.advancedTab.setOnClickListener {
            binding.generalSettingsContainer.visibility = View.GONE
            binding.advancedSettingsContainer.visibility = View.VISIBLE

            binding.generalTab.background = ContextCompat.getDrawable(this, R.drawable.tab_bg)
            binding.advancedTab.background = ContextCompat.getDrawable(this, R.drawable.tab_bg_sel)
        }

        binding.toggleWsServerUrlVisibility.setOnClickListener { v ->
            isWsServerUrlVisible = !isWsServerUrlVisible
            binding.wsServerUrl.transformationMethod =
                if (isWsServerUrlVisible) null else PasswordTransformationMethod()
            binding.toggleWsServerUrlVisibility.setImageResource(
                if (isWsServerUrlVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
            )
        }

        binding.toggleDisplaySecretKeyVisibility.setOnClickListener { v ->
            isDisplaySecretKeyVisible = !isDisplaySecretKeyVisible
            binding.displaySecretKey.transformationMethod =
                if (isDisplaySecretKeyVisible) null else PasswordTransformationMethod()
            binding.toggleDisplaySecretKeyVisibility.setImageResource(
                if (isDisplaySecretKeyVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
            )
        }

        binding.toggleLogServerUrlVisibility.setOnClickListener { v ->
            isLogServerUrlVisible = !isLogServerUrlVisible
            binding.logServerUrl.transformationMethod =
                if (isLogServerUrlVisible) null else PasswordTransformationMethod()
            binding.toggleLogServerUrlVisibility.setImageResource(
                if (isLogServerUrlVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
            )
        }

        binding.toggleDisplayTokenVisibility.setOnClickListener { v ->
            isDisplayTokenVisible = !isDisplayTokenVisible
            binding.displayToken.transformationMethod =
                if (isDisplayTokenVisible) null else PasswordTransformationMethod()
            binding.toggleDisplayTokenVisibility.setImageResource(
                if (isDisplayTokenVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
            )
        }

        binding.showErrorAsToastContainer.setOnClickListener {
            binding.showErrorAsToast.isChecked = !binding.showErrorAsToast.isChecked
        }

        binding.tickerBackgroundColorContainer.setOnClickListener {
            ColorPickerDialog.Builder(this@SettingsActivity).apply {
                colorPickerView.setInitialColor(tickerBgColor)
                colorPickerView.flagView = BubbleFlag(this@SettingsActivity).apply {
                    flagMode = FlagMode.FADE
                }
            }.setTitle(getString(R.string.ticker_background_color))
                .setPositiveButton(
                    getString(R.string.select),
                    object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                            setTickerBgColor(envelope)
                        }
                    })
                .setNegativeButton(
                    getString(R.string.cancel),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialogInterface: DialogInterface, i: Int) {
                            dialogInterface.dismiss()
                        }
                    })
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }

        binding.tickerTextColorContainer.setOnClickListener {
            ColorPickerDialog.Builder(this@SettingsActivity).apply {
                colorPickerView.setInitialColor(tickerTextColor)
                colorPickerView.flagView = BubbleFlag(this@SettingsActivity).apply {
                    flagMode = FlagMode.FADE
                }
            }.setTitle(R.string.ticker_text_color)
                .setPositiveButton(
                    getString(R.string.select),
                    object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                            setTickerTextColor(envelope)
                        }
                    })
                .setNegativeButton(
                    getString(R.string.cancel),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialogInterface: DialogInterface, i: Int) {
                            dialogInterface.dismiss()
                        }
                    })
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        val wsServerUrl = binding.wsServerUrl.text.toString().trim()
        val displaySecretKey = binding.displaySecretKey.text.toString().trim()
        val retryInterval = binding.retryInterval.text.toString().trim()

        if (wsServerUrl.isEmpty()) {
            binding.wsServerUrl.error = getString(R.string.server_url_is_required)
            isValid = false
        }

        if (displaySecretKey.isEmpty()) {
            binding.displaySecretKey.error = getString(R.string.secret_key_is_required)
            isValid = false
        }

        if (retryInterval.isEmpty()) {
            binding.retryInterval.error = getString(R.string.retry_interval_is_required)
            isValid = false
        } else if (retryInterval.toInt() <= 0) {
            binding.retryInterval.error = getString(R.string.retry_interval_lower_limit)
            isValid = false
        }

        return isValid
    }

    private fun setTickerBgColor(envelope: ColorEnvelope) {
        tickerBgColor = envelope.color
        binding.tickerBackgroundAlphaTileView.setPaintColor(envelope.color)
    }

    private fun setTickerTextColor(envelope: ColorEnvelope) {
        tickerTextColor = envelope.color
        binding.tickerTextAlphaTileView.setPaintColor(envelope.color)
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog?.dismiss()
    }
}