package sk.vava.mhd

import androidx.annotation.Nullable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Departure(
    val delay: String? = "", // 1min
    val departureTime: String? = "", // 18:04
    val destinationName: String? = "", // Karlova Ves
    val lineName: String? = "", // 9
    val notes: String? = "", // k
    val transportTypeName: String? = "", // A T O-trolejbus
    val vehicleNumber: Int? = 0 // 7423
)

data class DepartureBoard(
    val departures: List<Departure?>? = listOf(),
    val stationBanister: Int? = 0, // 1
    val stationName: String? = "", // Trnavské mýto
    val stationPassport: Int? = 0 // 437
)

data class Location(
    @Expose
    @Nullable
    var altitude: Int? = 0,
    @Expose
    @Nullable
    var longitude: Double? = 0.toDouble(),
    @Expose
    @Nullable
    var latitude: Double? = 0.toDouble()
)

data class Stop(
    val banister: Int? = 0, // 2
    @Nullable
    val location: Location? = Location(),
    val name: String? = "", // Agátová
    val passport: Int? = 0, // 81
    val id: String = ""
)

data class Transport(
    @Expose
    var isPublic: Boolean = false,
    @Expose
    var delay: String? = null,
    @Expose
    var coursename: String? = null,
    @Expose
    var linename: String? = null,
    @Expose
    var ismoving: Boolean = false,
    @Expose
    var locationtimestamp: String? = null,
    @Expose
    @Nullable
    var location: Location? = null,
    @Expose
    var vehicletransporttypecaption: String? = null,
    @Expose
    var vehicletransporttypename: String? = null,
    @Expose
    var vehiclenumber: Int = 0
)