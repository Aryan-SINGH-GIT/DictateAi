package com.example.dictateai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.dictateai.ui.theme.DictateAiTheme

class MainActivity : ComponentActivity() {
	private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		ensureMicPermission()

		setContent {
			DictateAiTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					KeyboardTestScreen(modifier = Modifier.padding(innerPadding))
				}
			}
		}
	}

	private fun ensureMicPermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			requestPermission.launch(Manifest.permission.RECORD_AUDIO)
		}
	}
}

@Composable
fun KeyboardTestScreen(modifier: Modifier = Modifier) {
	val context = LocalContext.current
	val keyboardController = LocalSoftwareKeyboardController.current
	val textState = remember { mutableStateOf("") }

	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = "Dictate AI Keyboard Tester",
			style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
		)

		TextField(
			value = textState.value,
			onValueChange = { textState.value = it },
			modifier = Modifier.fillMaxWidth(),
			placeholder = { Text("Tap here, then use the blue button on our keyboard") }
		)

		Button(
			onClick = {
				keyboardController?.show()
			},
			modifier = Modifier.fillMaxWidth(),
			contentPadding = PaddingValues(vertical = 12.dp)
		) {
			Text("Show Keyboard")
		}

		Button(
			onClick = {
				val imm = context.getSystemService(InputMethodManager::class.java)
				imm?.showInputMethodPicker()
			},
			modifier = Modifier.fillMaxWidth(),
			contentPadding = PaddingValues(vertical = 12.dp)
		) {
			Text("Choose Keyboard…")
		}

		Button(
			onClick = {
				context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
					addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				})
			},
			modifier = Modifier.fillMaxWidth(),
			contentPadding = PaddingValues(vertical = 12.dp)
		) {
			Text("Open Keyboard Settings")
		}
	}
}