package com.ro.aminvatatro.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class OffSearchResponse(
    val count: Int = 0,
    val products: List<OffProduct> = emptyList(),
)

@Serializable
internal data class OffProduct(
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("product_name_ro")
    val productNameRo: String? = null,
    @SerialName("product_name_en")
    val productNameEn: String? = null,
    val nutriments: Map<String, JsonElement> = emptyMap(),
)

/**
 * Result of querying an ingredient's nutritional data from OpenFoodFacts.
 */
data class NutritionSearchResult(
    val productName: String,
    val kcalPer100g: Double,
)

/**
 * Cross-platform client for OpenFoodFacts API (v2 search).
 *
 * Implements non-blocking HTTP requests with lenient JSON parsing and safe error handling.
 * Never throws exceptions on network or HTTP errors; returns `null` or empty lists gracefully.
 */
class OpenFoodFactsClient(
    private val httpClient: HttpClient = defaultHttpClient(),
) {
    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/cgi/search.pl"
        private const val USER_AGENT = "RetetarApp - KotlinMultiplatform - Version 1.0"

        fun defaultHttpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    },
                )
            }
        }
    }

    /**
     * Searches OpenFoodFacts for an ingredient by [query] and extracts the energy in kcal per 100g.
     */
    suspend fun searchKcalPer100g(query: String): NutritionSearchResult? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null

        return runCatching {
            val response: OffSearchResponse = httpClient.get(BASE_URL) {
                header("User-Agent", USER_AGENT)
                parameter("search_terms", trimmed)
                parameter("search_simple", "1")
                parameter("action", "process")
                parameter("json", "1")
                parameter("page_size", "5")
                parameter("fields", "product_name,product_name_ro,product_name_en,nutriments")
            }.body()

            for (product in response.products) {
                val energyKcal = extractEnergyKcal(product.nutriments)
                if (energyKcal != null && energyKcal > 0.0) {
                    val name = product.productNameRo ?: product.productName ?: product.productNameEn ?: trimmed
                    return@runCatching NutritionSearchResult(
                        productName = name,
                        kcalPer100g = energyKcal,
                    )
                }
            }
            null
        }.getOrNull()
    }

    private fun extractEnergyKcal(nutriments: Map<String, JsonElement>): Double? {
        val directKcal = nutriments["energy-kcal_100g"]?.jsonPrimitive?.doubleOrNull
            ?: nutriments["energy-kcal_value"]?.jsonPrimitive?.doubleOrNull
            ?: nutriments["energy-kcal"]?.jsonPrimitive?.doubleOrNull

        if (directKcal != null) return directKcal

        // If only energy-kj is available, convert kJ to kcal (1 kcal ≈ 4.184 kJ)
        val kj = nutriments["energy-kj_100g"]?.jsonPrimitive?.doubleOrNull
            ?: nutriments["energy_100g"]?.jsonPrimitive?.doubleOrNull
            ?: nutriments["energy"]?.jsonPrimitive?.doubleOrNull

        return kj?.let { it / 4.184 }
    }
}
