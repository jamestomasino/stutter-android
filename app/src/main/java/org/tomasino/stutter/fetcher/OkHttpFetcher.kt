package org.tomasino.stutter.fetcher

import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpFetcher(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val redirectCap: Int = DEFAULT_REDIRECT_CAP,
) : Fetcher {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .cookieJar(CookieJar.NO_COOKIES)
        .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun fetch(url: String): FetchResult {
        val initialUrl = url.toHttpUrlOrNull() ?: return FetchResult.Error("Invalid URL")
        var currentUrl = initialUrl
        var redirects = 0

        while (true) {
            val request = Request.Builder().url(currentUrl).build()
            val response = try {
                client.newCall(request).execute()
            } catch (ex: IOException) {
                return FetchResult.Error("Network error", ex)
            }

            var redirected = false
            response.use { handled ->
                val code = handled.code
                if (code in 300..399) {
                    val location = handled.header("Location")
                        ?.let { currentUrl.resolve(it) }
                        ?: return FetchResult.Error("Redirect without location")
                    redirects++
                    if (redirects > redirectCap) {
                        return FetchResult.Error("Redirect limit exceeded")
                    }
                    currentUrl = location
                    redirected = true
                    return@use
                }

                if (!handled.isSuccessful) {
                    return FetchResult.Error("HTTP ${handled.code}")
                }

                val body = handled.body ?: return FetchResult.Error("Empty response")
                val buffer = okio.Buffer()
                val text = try {
                    val source = body.source()
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer, READ_CHUNK_SIZE)
                        if (read == -1L) break
                        total += read
                        if (total > maxBytes) {
                            return FetchResult.Error("Response too large")
                        }
                    }
                    buffer.readString(body.contentType()?.charset() ?: Charsets.UTF_8)
                } catch (ex: IOException) {
                    return FetchResult.Error("Network error", ex)
                }
                if (text.isEmpty()) return FetchResult.Error("Empty response")
                return FetchResult.Success(text)
            }

            if (redirected) {
                continue
            }
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
        const val DEFAULT_MAX_BYTES = 15_000_000L
        const val DEFAULT_REDIRECT_CAP = 5
        private const val READ_CHUNK_SIZE = 8_192L
    }
}
