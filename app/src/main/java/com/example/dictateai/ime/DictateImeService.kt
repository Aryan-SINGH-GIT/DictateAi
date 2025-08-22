package com.example.dictateai.ime

import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.provider.Settings
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputConnection
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.dictateai.R
import com.example.dictateai.audio.AudioRecorder
import com.example.dictateai.data.network.RetrofitClient
import com.example.dictateai.data.repository.TranscriptionRepository
import com.example.dictateai.ui.ime.TranscriptionViewModel
import com.example.dictateai.ui.ime.UIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class DictateImeService : InputMethodService() {
	private lateinit var recordButtonContainer: FrameLayout
	private lateinit var micIcon: ImageView
	private lateinit var pulseRing: View
	private lateinit var closeButton: FrameLayout
	private lateinit var settingsButton: FrameLayout
	private lateinit var progressBar: ProgressBar
	private lateinit var statusText: TextView

	private lateinit var audioRecorder: AudioRecorder
	private var currentFile: File? = null

	private val viewModelStore = ViewModelStore()
	private lateinit var viewModel: TranscriptionViewModel

	private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

	// Animations
	private lateinit var pulseAnimation: Animation
	private lateinit var fadeInAnimation: Animation
	private lateinit var fadeOutAnimation: Animation

	// Backspace auto-repeat
	private val backspaceHandler = Handler(Looper.getMainLooper())
	private var backspaceRepeater: Runnable? = null
	private var backspaceIntervalMs: Long = 120L
	private var isBackspaceHeld: Boolean = false
	private var backspacePressStartAtMs: Long = 0L
	private var didBackspaceRepeat: Boolean = false
	private val INITIAL_BACKSPACE_DELAY_MS: Long = 300L
	private val MIN_BACKSPACE_INTERVAL_MS: Long = 40L
	private val BACKSPACE_ACCELERATION_MS: Long = 10L

	override fun onCreate() {
		super.onCreate()
		audioRecorder = AudioRecorder(this)
		loadAnimations()

		val repo = TranscriptionRepository(RetrofitClient.api)
		viewModel = ViewModelProvider(viewModelStore, object : ViewModelProvider.Factory {
			@Suppress("UNCHECKED_CAST")
			override fun <T : ViewModel> create(modelClass: Class<T>): T {
				return TranscriptionViewModel(repo) as T
			}
		}).get(TranscriptionViewModel::class.java)
	}

	private fun loadAnimations() {
		pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
		fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
		fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
	}

	override fun onCreateInputView(): View {
		val view = LayoutInflater.from(this).inflate(R.layout.ime_keyboard, null)
		
		// Initialize new UI elements
		recordButtonContainer = view.findViewById(R.id.recordButtonContainer)
		micIcon = view.findViewById(R.id.micIcon)
		pulseRing = view.findViewById(R.id.pulseRing)
		closeButton = view.findViewById(R.id.closeButton)
		settingsButton = view.findViewById(R.id.settingsButton)
		progressBar = view.findViewById(R.id.progressBar)
		statusText = view.findViewById(R.id.statusText)
		
		setupButtons()
		observeState()
		return view
	}

	private fun setupButtons() {
		setupRecordButton()
		setupCloseButton()
		setupSettingsButton()
	}

	private fun setupRecordButton() {
		recordButtonContainer.setOnTouchListener { _, event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					if (!hasMicPermission()) {
						showStatus(getString(R.string.mic_permission_rationale))
						launchPermissionActivity()
						return@setOnTouchListener true
					}
					startRecording()
					true
				}
				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					stopRecording()
					true
				}
				else -> false
			}
		}
	}

	private fun setupCloseButton() {
		// Handle tap and hold in one listener
		closeButton.setOnTouchListener { _, event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					didBackspaceRepeat = false
					startBackspaceAutoRepeat()
					true
				}
				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					stopBackspaceAutoRepeat()
					if (!didBackspaceRepeat) {
						deleteTextBeforeCursor()
					}
					true
				}
				else -> false
			}
		}
	}

	private fun setupSettingsButton() {
		settingsButton.setOnClickListener {
			openKeyboardSettings()
		}
	}

	private fun startRecording() {
		viewModel.setRecording()
		currentFile = audioRecorder.start()
		
		// Don't change button color or size; just run continuous pulse outside the button
		pulseRing.visibility = View.VISIBLE
		pulseRing.bringToFront()
		pulseRing.startAnimation(pulseAnimation)
		recordButtonContainer.invalidate()
		
		// Subtle mic feedback (no size change)
		micIcon.clearAnimation()
		
		// Do not show any recording text
		hideStatus()
	}

	private fun stopRecording() {
		// Stop pulse only; keep sizes/colors unchanged
		pulseRing.clearAnimation()
		pulseRing.visibility = View.GONE
		micIcon.clearAnimation()
		
		val file = audioRecorder.stop()
		if (file != null) {
			transcribe(file)
		}
	}

	private fun deleteTextBeforeCursor() {
		val ic: InputConnection? = currentInputConnection
		ic?.deleteSurroundingText(1, 0)
	}

	private fun clearAllText() {
		val ic: InputConnection? = currentInputConnection
		if (ic != null) {
			val before = ic.getTextBeforeCursor(100000, 0) ?: ""
			val after = ic.getTextAfterCursor(100000, 0) ?: ""
			ic.deleteSurroundingText(before.length, after.length)
		}
	}

	private fun transcribe(file: File) {
		// Keep keyboard size unchanged; show lightweight progress
		progressBar.visibility = View.VISIBLE
		progressBar.startAnimation(fadeInAnimation)
		viewModel.transcribe(file)
	}

	private fun showStatus(message: String) {
		statusText.text = message
		statusText.visibility = View.VISIBLE
		statusText.startAnimation(fadeInAnimation)
	}

	private fun hideStatus() {
		statusText.startAnimation(fadeOutAnimation)
		statusText.visibility = View.GONE
	}

	private fun observeState() {
		serviceScope.launch {
			viewModel.state.collectLatest { state ->
				when (state) {
					UIState.Idle -> {
						progressBar.visibility = View.GONE
						progressBar.clearAnimation()
						hideStatus()
					}
					UIState.Recording -> {
						progressBar.visibility = View.GONE
						showStatus("")
					}
					UIState.Processing -> {
						progressBar.visibility = View.VISIBLE
						showStatus(getString(R.string.processing))
					}
					is UIState.Completed -> {
						progressBar.visibility = View.GONE
						progressBar.clearAnimation()
						insertText(state.text)
						showStatus(getString(R.string.inserted))
						Handler(Looper.getMainLooper()).postDelayed({
							hideStatus()
						}, 1500)
						viewModel.setIdle()
					}
					is UIState.Error -> {
						progressBar.visibility = View.GONE
						progressBar.clearAnimation()
						showStatus(state.message)
						Handler(Looper.getMainLooper()).postDelayed({
							hideStatus()
						}, 2500)
						viewModel.setIdle()
					}


				}
			}
		}
	}

	private fun insertText(text: String) {
		val ic: InputConnection? = currentInputConnection
		ic?.commitText(text, 1)
	}

	private fun hasMicPermission(): Boolean {
		return ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
	}

	private fun launchPermissionActivity() {
		try {
			val intent = Intent(this, Class.forName("com.example.dictateai.MainActivity"))
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			startActivity(intent)
		} catch (_: Exception) { }
	}

	private fun openKeyboardSettings() {
		val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		startActivity(intent)
	}

	private fun startBackspaceAutoRepeat() {
		isBackspaceHeld = true
		backspaceIntervalMs = 120L
		// Initial delay for long-press feel
		backspaceRepeater = object : Runnable {
			override fun run() {
				if (!isBackspaceHeld) return
				deleteTextBeforeCursor()
				// mark that repetition happened so click doesn't fire
				didBackspaceRepeat = true

				backspaceIntervalMs = (backspaceIntervalMs - BACKSPACE_ACCELERATION_MS).coerceAtLeast(MIN_BACKSPACE_INTERVAL_MS)
				backspaceHandler.postDelayed(this, backspaceIntervalMs)
			}
		}
		backspaceHandler.postDelayed(backspaceRepeater!!, INITIAL_BACKSPACE_DELAY_MS)
	}

	private fun stopBackspaceAutoRepeat() {
		isBackspaceHeld = false
		backspaceRepeater?.let { backspaceHandler.removeCallbacks(it) }
		backspaceRepeater = null
	}

	override fun onDestroy() {
		super.onDestroy()
		serviceScope.cancel()
		viewModelStore.clear()
	}
}