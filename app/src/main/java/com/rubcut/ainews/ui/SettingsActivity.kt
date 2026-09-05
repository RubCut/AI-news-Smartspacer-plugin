package com.rubcut.ainews.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.rubcut.ainews.AiProvider
import com.rubcut.ainews.Constants
import com.rubcut.ainews.NewsUpdater
import com.rubcut.ainews.R
import com.rubcut.ainews.SettingsRepository
import com.rubcut.ainews.TargetSettings
import com.rubcut.ainews.targets.NewsTarget
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/**
 * The plugin has no launcher entry: Smartspacer opens this screen when the
 * target is added or its settings are opened, and it configures that one
 * target instance — topic, AI provider, key, model and refresh interval.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: TargetSettings
    private lateinit var smartspacerId: String

    private lateinit var topicField: TextInputEditText
    private lateinit var apiKeyField: TextInputEditText
    private lateinit var languageField: TextInputEditText
    private lateinit var providerField: MaterialAutoCompleteTextView
    private lateinit var modelField: MaterialAutoCompleteTextView
    private lateinit var intervalSlider: Slider
    private lateinit var intervalValue: TextView
    private lateinit var storiesSlider: Slider
    private lateinit var storiesValue: TextView
    private lateinit var status: TextView
    private lateinit var preview: TextView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        smartspacerId = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
            ?: FALLBACK_ID
        settings = SettingsRepository(this).forTarget(smartspacerId)

        topicField = findViewById(R.id.topicField)
        apiKeyField = findViewById(R.id.apiKeyField)
        languageField = findViewById(R.id.languageField)
        providerField = findViewById(R.id.providerField)
        modelField = findViewById(R.id.modelField)
        intervalSlider = findViewById(R.id.intervalSlider)
        intervalValue = findViewById(R.id.intervalValue)
        storiesSlider = findViewById(R.id.storiesSlider)
        storiesValue = findViewById(R.id.storiesValue)
        status = findViewById(R.id.statusText)
        preview = findViewById(R.id.previewText)
        progress = findViewById(R.id.progress)
        saveButton = findViewById(R.id.buttonSave)

        topicField.setText(settings.topic)
        apiKeyField.setText(settings.apiKey)
        languageField.setText(settings.language)

        // Only Gemini is available for now, but the picker is already in place.
        providerField.setSimpleItems(AiProvider.entries.map { it.label }.toTypedArray())
        providerField.setText(settings.aiProvider.label, false)

        modelField.setSimpleItems(Constants.GEMINI_MODELS.toTypedArray())
        modelField.setText(settings.model, false)

        intervalSlider.valueFrom = Constants.MIN_REFRESH_MINUTES.toFloat()
        intervalSlider.valueTo = Constants.MAX_REFRESH_MINUTES.toFloat()
        intervalSlider.stepSize = 15f
        intervalSlider.value = normalisedInterval()
        intervalSlider.addOnChangeListener { _, value, _ -> renderInterval(value.roundToInt()) }
        renderInterval(intervalSlider.value.roundToInt())

        storiesSlider.valueFrom = 1f
        storiesSlider.valueTo = 5f
        storiesSlider.stepSize = 1f
        storiesSlider.value = settings.maxStories.toFloat()
        storiesSlider.addOnChangeListener { _, value, _ -> renderStories(value.roundToInt()) }
        renderStories(settings.maxStories)

        findViewById<MaterialButton>(R.id.buttonGetKey).setOnClickListener {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(API_KEY_URL)))
            }
        }
        findViewById<MaterialButton>(R.id.buttonRestore).setOnClickListener {
            settings.clearDismissed()
            notifyTarget()
            toast(getString(R.string.toast_restored))
            render()
        }
        saveButton.setOnClickListener { save() }

        render()
    }

    private fun normalisedInterval(): Float {
        val minutes = settings.refreshIntervalMinutes
            .coerceIn(Constants.MIN_REFRESH_MINUTES, Constants.MAX_REFRESH_MINUTES)
        return ((minutes / 15) * 15).coerceAtLeast(Constants.MIN_REFRESH_MINUTES).toFloat()
    }

    private fun renderInterval(minutes: Int) {
        intervalValue.text = if (minutes >= 60 && minutes % 60 == 0) {
            resources.getQuantityString(R.plurals.hours, minutes / 60, minutes / 60)
        } else {
            resources.getQuantityString(R.plurals.minutes, minutes, minutes)
        }
    }

    private fun renderStories(count: Int) {
        storiesValue.text = resources.getQuantityString(R.plurals.stories, count, count)
    }

    private fun render() {
        val error = settings.lastError
        val updated = settings.lastUpdated
        status.text = when {
            error != null -> error
            updated > 0L -> getString(
                R.string.status_updated,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(updated)),
                settings.getVisibleStories().size
            )
            else -> getString(R.string.status_never_generated)
        }

        val stories = settings.getVisibleStories()
        if (stories.isEmpty()) {
            preview.visibility = View.GONE
        } else {
            preview.visibility = View.VISIBLE
            preview.text = stories.joinToString("\n") { "• ${it.shortTitle}" }
        }
    }

    private fun persistFields() {
        settings.topic = topicField.text?.toString().orEmpty()
        settings.apiKey = apiKeyField.text?.toString().orEmpty()
        settings.language = languageField.text?.toString()
            ?.takeIf { it.isNotBlank() } ?: settings.language
        settings.aiProvider = AiProvider.entries
            .firstOrNull { it.label == providerField.text?.toString() } ?: AiProvider.GEMINI
        settings.model = modelField.text?.toString()
            ?.takeIf { it.isNotBlank() } ?: Constants.DEFAULT_GEMINI_MODEL
        settings.refreshIntervalMinutes = intervalSlider.value.roundToInt()
        settings.maxStories = storiesSlider.value.roundToInt()
    }

    private fun save() {
        persistFields()
        if (!settings.isConfigured) {
            toast(getString(R.string.error_no_key))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            NewsUpdater.refresh(this@SettingsActivity, settings)
            setLoading(false)
            render()
            notifyTarget()
            val error = settings.lastError
            if (error != null) {
                toast(error)
            } else {
                toast(getString(R.string.toast_generated))
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        saveButton.isEnabled = !loading
        saveButton.setText(if (loading) R.string.settings_generating else R.string.settings_save)
    }

    private fun notifyTarget() {
        SmartspacerTargetProvider.notifyChange(this, NewsTarget::class.java, smartspacerId)
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    override fun onPause() {
        super.onPause()
        // Keep edits when the user leaves with the back gesture.
        if (isFinishing) {
            persistFields()
            notifyTarget()
        }
    }

    companion object {
        private const val FALLBACK_ID = "default"
        private const val API_KEY_URL = "https://aistudio.google.com/app/apikey"
    }
}
