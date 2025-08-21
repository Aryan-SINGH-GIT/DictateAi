package com.example.dictateai.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {
	private var mediaRecorder: MediaRecorder? = null
	private var outputFile: File? = null

	fun start(): File {
		stopSafely()
		val file = createOutputFile()
		outputFile = file

		val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
		mediaRecorder = recorder
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
		recorder.setAudioChannels(1)
		recorder.setAudioSamplingRate(16000)
		recorder.setAudioEncodingBitRate(64000)
		recorder.setOutputFile(file.absolutePath)
		recorder.prepare()
		recorder.start()
		return file
	}

	fun stop(): File? {
		val file = outputFile
		stopSafely()
		return file
	}

	private fun stopSafely() {
		try {
			mediaRecorder?.apply {
				try { stop() } catch (_: Exception) {}
				reset()
				release()
			}
		} finally {
			mediaRecorder = null
		}
	}

	private fun createOutputFile(): File {
		val dir = File(context.cacheDir, "recordings")
		if (!dir.exists()) dir.mkdirs()
		val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
		return File(dir, "rec_${ts}.m4a")
	}
} 