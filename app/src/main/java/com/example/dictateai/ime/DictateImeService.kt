package com.example.dictateai.ime

import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
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
	private lateinit var recordButton: Button
	private lateinit var backspaceButton: Button
	private lateinit var progress: ProgressBar
	private lateinit var statusText: TextView

	private lateinit var audioRecorder: AudioRecorder
	private var currentFile: File? = null

	private val viewModelStore = ViewModelStore()
	private lateinit var viewModel: TranscriptionViewModel

	private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

	override fun onCreate() {
		super.onCreate()
		audioRecorder = AudioRecorder(this)

		val repo = TranscriptionRepository(RetrofitClient.api)
		viewModel = ViewModelProvider(viewModelStore, object : ViewModelProvider.Factory {
			@Suppress("UNCHECKED_CAST")
			override fun <T : ViewModel> create(modelClass: Class<T>): T {
				return TranscriptionViewModel(repo) as T
			}
		}).get(TranscriptionViewModel::class.java)
	}

	override fun onCreateInputView(): View {
		val view = LayoutInflater.from(this).inflate(R.layout.ime_keyboard, null)
		recordButton = view.findViewById(R.id.recordButton)
		backspaceButton = view.findViewById(R.id.backspaceButton)
		progress = view.findViewById(R.id.progress)
		statusText = view.findViewById(R.id.statusText)

		setupButtons()
		observeState()
		return view
	}

	private fun setupButtons() {
		setupRecordButton()
		setupBackspaceButton()
	}

	private fun setupRecordButton() {
		recordButton.text = getString(R.string.hold_to_dictate)
		recordButton.setOnTouchListener { _, event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					if (!hasMicPermission()) {
						statusText.text = getString(R.string.mic_permission_rationale)
						launchPermissionActivity()
						return@setOnTouchListener true
					}
					viewModel.setRecording()
					currentFile = audioRecorder.start()
					statusText.text = getString(R.string.recording)
					recordButton.isPressed = true
					true
				}
				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					recordButton.isPressed = false
					val file = audioRecorder.stop()
					if (file != null) {
						transcribe(file)
					}
					true
				}
				else -> false
			}
		}
	}

	private fun setupBackspaceButton() {
		backspaceButton.text = getString(R.string.backspace)
		backspaceButton.setOnClickListener {
			deleteTextBeforeCursor()
		}
	}

	private fun deleteTextBeforeCursor() {
		val ic: InputConnection? = currentInputConnection
		ic?.deleteSurroundingText(1, 0)
	}

	private fun transcribe(file: File) {
		progress.visibility = View.VISIBLE
		viewModel.transcribe(file)
	}

	private fun observeState() {
		serviceScope.launch {
			viewModel.state.collectLatest { state ->
				when (state) {
					UIState.Idle -> {
						progress.visibility = View.GONE
						recordButton.text = getString(R.string.hold_to_dictate)
						statusText.text = ""
					}
					UIState.Recording -> {
						progress.visibility = View.GONE
						recordButton.text = getString(R.string.recording)
					}
					UIState.Processing -> {
						progress.visibility = View.VISIBLE
						recordButton.text = getString(R.string.processing)
						statusText.text = getString(R.string.processing)
					}
					is UIState.Completed -> {
						progress.visibility = View.GONE
						recordButton.text = getString(R.string.hold_to_dictate)
						insertText(state.text)
						statusText.text = getString(R.string.inserted)
						Handler(Looper.getMainLooper()).postDelayed({
							statusText.text = ""
						}, 1500)
						viewModel.setIdle()
					}
					is UIState.Error -> {
						progress.visibility = View.GONE
						recordButton.text = getString(R.string.hold_to_dictate)
						statusText.text = state.message
						Handler(Looper.getMainLooper()).postDelayed({
							statusText.text = ""
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

	override fun onDestroy() {
		super.onDestroy()
		serviceScope.cancel()
		viewModelStore.clear()
	}
} 