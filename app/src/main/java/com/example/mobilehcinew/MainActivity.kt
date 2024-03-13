package com.example.mobilehcinew

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException


class MainActivity : ComponentActivity(), RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var muteButton: Button
    private lateinit var forwardButton: Button
    private lateinit var backwardButton: Button
    private lateinit var statusIndicator: TextView
    private lateinit var youTubePlayerView: YouTubePlayerView
    private lateinit var youTubeTracker: YouTubePlayerTracker
    private var isMuted: Boolean = false
    private var currentState = AppState.IDLE
    private lateinit var youtubePlayerSetup: YouTubePlayerSetup
    private var originalVolumeLevel: Int = 0
    private val showToast = UIUtil::showToast
    private val updateButtonState = UIUtil::updateButtonState
    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        youtubePlayerSetup = YouTubePlayerSetup(youTubePlayerView)
        youtubePlayerSetup.initializeYouTubePlayer()
        lifecycle.addObserver(youTubePlayerView)
        youTubeTracker = YouTubePlayerTracker()
        setListeners()

        if (!PermissionsUtil.hasRecordAudioPermission(this)) {
            requestRecordAudioPermission()
        } else {
            initModel()
        }
    }

    private fun initializeViews() {
        playButton = findViewById(R.id.playButton)
        pauseButton = findViewById(R.id.pauseButton)
        muteButton = findViewById(R.id.muteButton)
        forwardButton = findViewById(R.id.forwardButton)
        backwardButton = findViewById(R.id.backwardButton)
        statusIndicator = findViewById(R.id.statusIndicator)
        youTubePlayerView = findViewById(R.id.youtube_player_view)
    }

    private fun setListeners() {
        playButton.setOnClickListener {
            performButtonAction(
                playButton,
                { youtubePlayerSetup.youTubePlayer.play() },
                "Play",
                "Play Button Clicked"
            )
        }
        pauseButton.setOnClickListener {
            performButtonAction(
                pauseButton,
                { youtubePlayerSetup.youTubePlayer.pause() },
                "Pause",
                "Pause Button Clicked"
            )
        }
        muteButton.setOnClickListener {
            if (isMuted) {
                performButtonAction(
                    muteButton,
                    { youtubePlayerSetup.youTubePlayer.unMute() },
                    "Unmute",
                    "Mute Button Clicked"
                )
                isMuted = false
            } else {
                performButtonAction(
                    muteButton,
                    { youtubePlayerSetup.youTubePlayer.unMute() },
                    "Mute",
                    "Mute Button Clicked"
                )
                isMuted = true
            }
        }
        forwardButton.setOnClickListener {
            performButtonAction(
                forwardButton,
                { youtubePlayerSetup.youTubePlayer.seekTo(youTubeTracker.currentSecond + 10) },
                "Forward",
                "Forward Button Clicked"
            )
        }
        backwardButton.setOnClickListener {
            performButtonAction(
                backwardButton,
                { youtubePlayerSetup.youTubePlayer.seekTo(youTubeTracker.currentSecond - 10) },
                "Backward",
                "Backward Button Clicked"
            )
        }
    }

    private fun requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requiredPermission = Manifest.permission.RECORD_AUDIO

            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(arrayOf(requiredPermission), 101)
            }
        }
    }

    private fun initModel() {
        StorageService.unpack(this, "model-en-us", "model", { model ->
            this.model = model
            startListening()
        }, { exception ->
            showToast(this, "Failed to unpack the model: ${exception.message}")
        })
    }

    private fun startListening() {
        if (model != null) {
            try {
                Log.d("MainScreen", "Starting listening")
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService?.startListening(this)
                updateStatusIndicator(R.color.idle_state, "Idle...!")

            } catch (e: IOException) {
                showToast(this, "Failed to start listening: ${e.message}")
            }
        }
    }

    override fun onResult(hypothesis: String) {
        Log.d("MainScreen", "Hypothesis: $hypothesis")
        when (currentState) {
            AppState.IDLE -> {
                if (hypothesis.contains("\"text\" : \"hello\"", true)) {
                    currentState = AppState.AWAITING_COMMAND
                    updateStatusIndicator(R.color.listening_state, "Listening...!")
                    saveCurrentVolume()
                    setVolumeToZero()
                }
            }

            AppState.AWAITING_COMMAND -> {
                if (hypothesis.contains("\"text\" : \"play\"", true)) {
                    performAction { playButton.performClick() }
                } else if (hypothesis.contains("\"text\" : \"mute\"", true)) {
                    performAction { muteButton.performClick() }
                } else if (hypothesis.contains("\"text\" : \"unmute\"", true)) {
                    performAction { muteButton.performClick() }
                } else if (hypothesis.contains("\"text\" : \"pause\"", true)) {
                    performAction { pauseButton.performClick() }
                } else if (hypothesis.contains("\"text\" : \"forward\"", true)) {
                    performAction { forwardButton.performClick() }
                } else if (hypothesis.contains("\"text\" : \"backward\"", true)) {
                    performAction { backwardButton.performClick() }
                } else {
                    updateStatusIndicator(R.color.error_state, "Invalid Command!")
                    statusIndicator.postDelayed({
                        revertToIdleState()
                    }, 2000)
                }
            }

            AppState.COMMAND_RECEIVED -> {
                revertToIdleState()
            }
        }
    }


    override fun onFinalResult(hypothesis: String) {}

    override fun onPartialResult(hypothesis: String) {}

    override fun onError(e: Exception) {
        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onTimeout() {}

    override fun onDestroy() {
        super.onDestroy()
        speechService?.let {
            it.stop()
            it.shutdown()
        }
    }

    private fun updateStatusIndicator(color: Int, text: String) {
        val background = statusIndicator.background as GradientDrawable
        background.setColor(ContextCompat.getColor(this, color))
        statusIndicator.text = text
        statusIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        statusIndicator.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
    }

    private fun performButtonAction(
        button: Button, youTubePlayerAction: () -> Unit, actionText: String, toastMessage: String
    ) {
        showToast(this, toastMessage)
        updateButtonState(button, this, R.color.correct_state)
        youTubePlayerAction()
        updateStatusIndicator(R.color.correct_state, actionText)
        statusIndicator.postDelayed({
            revertToIdleState()
            updateButtonState(button, this, R.color.button_colour)
        }, 2000)
    }

    private fun performAction(action: () -> Unit) {
        runOnUiThread(action)
        currentState = AppState.COMMAND_RECEIVED
        statusIndicator.postDelayed({
            revertToIdleState()
        }, 2000)
    }

    private fun setVolumeToZero() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
    }

    private fun restoreOriginalVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolumeLevel, 0)
    }

    private fun revertToIdleState() {
        restoreOriginalVolume()
        currentState = AppState.IDLE
        updateStatusIndicator(R.color.idle_state, "Idle...")
    }

    private fun saveCurrentVolume() {
        originalVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
}