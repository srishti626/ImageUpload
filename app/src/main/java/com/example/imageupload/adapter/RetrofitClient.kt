package eu.tutorials.myfirstapp.adapter

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val retrofit by lazy{


        val httpLoggingInterceptor = HttpLoggingInterceptor()
        val logger: HttpLoggingInterceptor =
            httpLoggingInterceptor.apply {
                httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            }
        val okkHttpclient = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
        Retrofit.Builder().baseUrl("https://himalayanewsagency.000webhostapp.com/apis/")
            .client(okkHttpclient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiInterface by lazy{
        retrofit.create(ApiInterface::class.java)
    }
}