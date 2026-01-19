package org.tomasino.stutter.fetcher

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class FetcherTest {
    @Test
    fun redirectsStopAtCap() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/next"))
        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/final"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html></html>"))
        server.start()

        try {
            runBlocking {
                val fetcher = OkHttpFetcher(redirectCap = 1)
                val result = fetcher.fetch(server.url("/start").toString())
                assertTrue(result is FetchResult.Error)
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun responseSizeCapRejectsLargePayloads() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("a".repeat(2048)))
        server.start()

        try {
            runBlocking {
                val fetcher = OkHttpFetcher(maxBytes = 512)
                val result = fetcher.fetch(server.url("/big").toString())
                assertTrue(result is FetchResult.Error)
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun timeoutsSurfaceFailure() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setBodyDelay(2, TimeUnit.SECONDS)
                .setBody("<html></html>")
        )
        server.start()

        try {
            runBlocking {
                val fetcher = OkHttpFetcher(timeoutMs = 100)
                val result = fetcher.fetch(server.url("/slow").toString())
                assertTrue(result is FetchResult.Error)
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun noCookiesSentOrStored() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Set-Cookie", "session=abc123")
                .addHeader("Location", "/next")
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html></html>"))
        server.start()

        try {
            runBlocking {
                val fetcher = OkHttpFetcher()
                val result = fetcher.fetch(server.url("/start").toString())
                assertTrue(result is FetchResult.Success)
            }

            val first = server.takeRequest(1, TimeUnit.SECONDS)
            val second = server.takeRequest(1, TimeUnit.SECONDS)
            assertTrue(first?.getHeader("Cookie").isNullOrEmpty())
            assertTrue(second?.getHeader("Cookie").isNullOrEmpty())
        } finally {
            server.shutdown()
        }
    }
}
