package com.example.dictateai.ui.ime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dictateai.data.repository.TranscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class UIState {
	data object Idle : UIState()
	data object Recording : UIState()
	data object Processing : UIState()
	data class Completed(val text: String) : UIState()
	data class Error(val message: String) : UIState()
}

class TranscriptionViewModel(private val repository: TranscriptionRepository) : ViewModel() {
	private val _state = MutableStateFlow<UIState>(UIState.Idle)
	val state: StateFlow<UIState> = _state

	fun setRecording() { _state.value = UIState.Recording }
	fun setIdle() { _state.value = UIState.Idle }

	fun transcribe(file: File) {
		_state.value = UIState.Processing
		viewModelScope.launch {
			val result = repository.transcribeFile(file)
			_state.value = result.fold(
				onSuccess = { text -> UIState.Completed(text) },
				onFailure = { err -> UIState.Error(err.message ?: "Unknown error") }
			)
		}
	}
} 