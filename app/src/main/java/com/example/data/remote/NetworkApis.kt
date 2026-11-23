package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchPlaces(
        @Query("name") query: String,
        @Query("count") count: Int = 8,
        @Query("language") language: String = "en",
    ): OpenMeteoGeocodingResponse
}

interface PhotonGeocodingApi {
    @GET("api")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("limit") limit: Int = 6,
        @Query("lang") lang: String = "en",
    ): PhotonResponse

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lang") lang: String = "en",
    ): PhotonResponse
}

interface OsrmRoutingApi {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getDrivingRoute(
        @Path(value = "coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
    ): OsrmRouteResponse
}

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getBatchForecastRaw(
        @Query(value = "latitude", encoded = true) latitudes: String,
        @Query(value = "longitude", encoded = true) longitudes: String,
        @Query(value = "hourly", encoded = true) hourly: String =
            "temperature_2m,apparent_temperature,weather_code,precipitation,precipitation_probability,wind_speed_10m,wind_direction_10m,visibility,cloud_cover",
        @Query(value = "daily", encoded = true) daily: String = "sunrise,sunset",
        @Query("timeformat") timeformat: String = "unixtime",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
    ): ResponseBody
}

interface RainViewerRadarApi {
    @GET("public/weather-maps.json")
    suspend fun getWeatherMaps(): RainViewerResponse
}

object NetworkModule {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "SkyWay-Android/1.1 (Route Weather Instrument)")
                .build()
            var response = chain.proceed(request)
            // Retry once on server 5xx
            if (!response.isSuccessful && response.code >= 500) {
                response.close()
                response = chain.proceed(request)
            }
            response
        }
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val openMeteoGeocodingApi: OpenMeteoGeocodingApi by lazy {
        retrofit("https://geocoding-api.open-meteo.com/").create(OpenMeteoGeocodingApi::class.java)
    }

    val photonGeocodingApi: PhotonGeocodingApi by lazy {
        retrofit("https://photon.komoot.io/").create(PhotonGeocodingApi::class.java)
    }

    val fossgisRoutingApi: OsrmRoutingApi by lazy {
        retrofit("https://routing.openstreetmap.de/routed-car/").create(OsrmRoutingApi::class.java)
    }

    val osrmRoutingApi: OsrmRoutingApi by lazy {
        retrofit("https://router.project-osrm.org/").create(OsrmRoutingApi::class.java)
    }

    val openMeteoForecastApi: OpenMeteoForecastApi by lazy {
        retrofit("https://api.open-meteo.com/").create(OpenMeteoForecastApi::class.java)
    }

    val rainViewerRadarApi: RainViewerRadarApi by lazy {
        retrofit("https://api.rainviewer.com/").create(RainViewerRadarApi::class.java)
    }
}
