package com.example.dictateai.data.network

import com.example.dictateai.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
	private const val BASE_URL = "https://api.groq.com/"

	private val authInterceptor = Interceptor { chain ->
		val request = chain.request().newBuilder()
			.addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
			.build()
		chain.proceed(request)
	}

	private val logging = HttpLoggingInterceptor().apply {
		level = HttpLoggingInterceptor.Level.BASIC
	}

	private val client: OkHttpClient = OkHttpClient.Builder()
		.addInterceptor(authInterceptor)
		.addInterceptor(logging)
		.build()

	val api: GroqApi by lazy {
		Retrofit.Builder()
			.baseUrl(BASE_URL)
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(GroqApi::class.java)
	}
} 