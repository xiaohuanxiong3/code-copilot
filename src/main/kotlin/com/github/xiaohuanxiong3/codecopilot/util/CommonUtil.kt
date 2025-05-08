package com.github.xiaohuanxiong3.codecopilot.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

val httpClient = OkHttpClient().newBuilder()
    .readTimeout(300, TimeUnit.SECONDS)
    .build()

val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

val objectMapper : ObjectMapper = jacksonObjectMapper()

val globalIOScope = CoroutineScope(Dispatchers.IO)
