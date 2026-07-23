package com.studio71.germanlinia2_b2.data.image

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.studio71.germanlinia2_b2.BuildConfig
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale


/**
 * Looks up the first Google Image result via the official Google Custom Search API.
 */
class GoogleImageSearchClient(
    private val apiKey: String = BuildConfig.GOOGLE_IMAGE_API_KEY,
    private val searchEngineCx: String = BuildConfig.GOOGLE_IMAGE_SEARCH_CX
) {

    private val api: GoogleCustomSearchApi

    init {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "GermanLiniA2_B2/1.0 (google-image-lookup)")
                    .build()
                chain.proceed(request)
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleCustomSearchApi::class.java)
    }

    suspend fun findMeaningImage(word: VocabularyEntity): MeaningImageLookupResult {
        val queries = composeQueries(word)
        if (apiKey.isBlank() || searchEngineCx.isBlank()) {
            return MeaningImageLookupResult(imageUrl = null, queryUsed = queries.firstOrNull().orEmpty())
        }

        var lastError: Throwable? = null
        for (query in queries) {
            val response = try {
                api.searchImage(
                    key = apiKey,
                    cx = searchEngineCx,
                    query = query
                )
            } catch (t: Throwable) {
                lastError = t
                continue
            }

            val firstUrl = response.items
                .orEmpty()
                .firstOrNull { isProbablyRenderableImage(it.link.orEmpty()) }
                ?.link

            if (!firstUrl.isNullOrBlank()) {
                return MeaningImageLookupResult(imageUrl = firstUrl, queryUsed = query)
            }
        }

        if (lastError != null) throw lastError
        return MeaningImageLookupResult(imageUrl = null, queryUsed = queries.firstOrNull().orEmpty())
    }

    private fun composeQueries(word: VocabularyEntity): List<String> {
        val baseWord = word.word.trim()
        val germanMeaning = word.germanMeaning.trim()
        val englishMeaning = word.english.trim()

        val queries = linkedSetOf<String>()

        // German-first, as requested.
        if (baseWord.isNotBlank() && germanMeaning.isNotBlank()) {
            queries += "$baseWord $germanMeaning"
        }
        if (baseWord.isNotBlank()) {
            queries += baseWord
        }
        if (baseWord.isNotBlank() && englishMeaning.isNotBlank()) {
            queries += "$baseWord $englishMeaning"
        }
        if (word.displayWord.isNotBlank()) {
            queries += word.displayWord
        }

        return queries.filter { it.isNotBlank() }
    }

    private fun isProbablyRenderableImage(url: String): Boolean {
        if (url.isBlank()) return false
        val clean = url.substringBefore('?').lowercase(Locale.ROOT)
        return clean.endsWith(".jpg") ||
            clean.endsWith(".jpeg") ||
            clean.endsWith(".png") ||
            clean.endsWith(".webp") ||
            clean.endsWith(".gif") ||
            clean.endsWith(".svg")
    }

    private interface GoogleCustomSearchApi {
        @GET("customsearch/v1")
        suspend fun searchImage(
            @Query("key") key: String,
            @Query("cx") cx: String,
            @Query("q") query: String,
            @Query("searchType") searchType: String = "image",
            @Query("safe") safe: String = "active",
            @Query("lr") languageRestriction: String = "lang_de",
            @Query("num") num: Int = 1
        ): GoogleCustomSearchResponse
    }

    private data class GoogleCustomSearchResponse(
        @param:Json(name = "items") val items: List<GoogleImageItem>?
    )

    private data class GoogleImageItem(
        @param:Json(name = "link") val link: String?
    )

    private companion object {
        const val BASE_URL = "https://customsearch.googleapis.com/"
    }
}

