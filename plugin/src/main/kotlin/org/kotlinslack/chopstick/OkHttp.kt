package org.kotlinslack.chopstick

import com.squareup.okhttp.*
import java.io.*

fun OkHttpClient.execute(request: Request): Response = newCall(request).execute()
fun OkHttpClient.executeAsync(url: String, body: ResponseHandler.() -> Unit) = executeAsync(Request.Builder().apply {
    url(url)
}.build(), body)
fun OkHttpClient.execute(url: String, body: ResponseHandler.() -> Unit) = execute(Request.Builder().apply {
    url(url)
}.build(), body)

fun OkHttpClient.executeAsync(request: Request, body: ResponseHandler.() -> Unit) {
    ResponseHandler().run {
        body()
        newCall(request).enqueue(this)
    }
}
fun OkHttpClient.execute(request: Request, body: ResponseHandler.() -> Unit) {
    ResponseHandler().run {
        body()
        try {
            val response = newCall(request).execute()
            onResponse(response)
        } catch(e: IOException) {
            onFailure(request, e)
        }
    }
}

class ResponseHandler : Callback {
    private val successFunctions = arrayListOf<(Response) -> Unit>()
    private val failFunctions = arrayListOf<(Request, IOException) -> Unit>()

    override fun onFailure(request: Request, e: IOException) {
        failFunctions.forEach { it(request, e) }
    }
    override fun onResponse(response: Response) {
        successFunctions.forEach { it(response) }
        response.body().close()
    }

    fun success(body: (Response) -> Unit) {
        successFunctions.add(body)
    }

    fun fail(body: (Request, IOException) -> Unit) {
        failFunctions.add(body)
    }
}