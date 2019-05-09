package sk.vava.mhd

import androidx.annotation.NonNull
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import sk.vava.mhd.ApiInterface.Companion.API_BASE_URL
import java.io.IOException
import java.util.concurrent.TimeUnit


interface ApiInterface {
    companion object {
        val API_BASE_URL = "http://mhdrunner-env.pnac4huusd.us-east-2.elasticbeanstalk.com/"
    }
}

interface RepositoryInterface {

    @GET("all")
    fun getAllTrans(): Call<List<Transport>>

    @GET("box")
    fun getBox(
        @Query("latBottomLeft") latBottomLeft: Double,
        @Query("lonBottomLeft") lonBottomLeft: Double,
        @Query("latTopRight") latTopRight: Double,
        @Query("lonTopRight") lonTopRight: Double
    ): Call<List<Transport>>

    @GET("stops")
    fun getStops(): Call<List<Stop>>

    @GET("departures")
    fun getDeparatures(@Query("passport") passport: Int, @Query("banister") banister: Int): Call<DepartureBoard>

    companion object {
        fun create():RepositoryInterface{
            val retrofit = Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(
                    OkHttpClient()
                        .newBuilder()
                        .callTimeout(2L, TimeUnit.MINUTES)
                        .readTimeout(2L, TimeUnit.MINUTES)
                        .writeTimeout(2L, TimeUnit.MINUTES)
                        .addNetworkInterceptor(
                            HttpLoggingInterceptor()
                                .setLevel(HttpLoggingInterceptor.Level.HEADERS)
                        )
                        .build()
                )
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .build()
            return retrofit.create(RepositoryInterface::class.java)

        }
    }

}