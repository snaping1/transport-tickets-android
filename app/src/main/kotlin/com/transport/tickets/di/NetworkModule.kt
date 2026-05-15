package com.transport.tickets.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.transport.tickets.BuildConfig
import com.transport.tickets.data.remote.api.TransportApi
import com.transport.tickets.data.remote.datasource.RemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseAuth

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideOkHttpClient(firebaseAuth: FirebaseAuth): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Blocking call is fine here: OkHttp interceptors run on background threads
                val token = runCatching {
                    firebaseAuth.currentUser
                        ?.getIdToken(false)
                        ?.let { com.google.android.gms.tasks.Tasks.await(it) }
                        ?.token
                }.getOrNull()

                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else chain.request()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideTransportApi(retrofit: Retrofit): TransportApi =
        retrofit.create(TransportApi::class.java)

    @Provides
    @Singleton
    fun provideRemoteDataSource(api: TransportApi): RemoteDataSource =
        RemoteDataSource(api)
}
