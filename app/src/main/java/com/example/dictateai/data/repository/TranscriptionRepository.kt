package com.example.dictateai.data.repository

import com.example.dictateai.data.network.GroqApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class TranscriptionRepository(private val api: GroqApi) {
	suspend fun transcribeFile(file: File): Result<String> = withContext(Dispatchers.IO) {
		return@withContext try {
			val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
			val body = MultipartBody.Part.createFormData(
				name = "file",
				filename = file.name,
				body = requestFile
			)
			val model: RequestBody = RequestBody.create("text/plain".toMediaType(), "whisper-large-v3")
			val responseFormat: RequestBody = RequestBody.create("text/plain".toMediaType(), "json")

			val response = api.transcribe(body, model, responseFormat)
			Result.success(response.text ?: "")
		} catch (t: Throwable) {
			Result.failure(t)
		}
	}
} 