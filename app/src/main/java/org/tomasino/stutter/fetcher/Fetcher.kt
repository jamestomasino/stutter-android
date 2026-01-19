package org.tomasino.stutter.fetcher

sealed class FetchResult {
    data class Success(val html: String) : FetchResult()
    data class Error(val message: String, val cause: Throwable? = null) : FetchResult()
}

interface Fetcher {
    suspend fun fetch(url: String): FetchResult
}
