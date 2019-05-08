package sk.vava.mhd

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.listItems
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ui.IconGenerator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.concurrent.timerTask

private const val PERMISSION_REQUEST = 10

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnCameraMoveListener {

    lateinit var locationManager: LocationManager
    private var hasGps = false
    private var hasNetwork = false
    private var locationGPS: Location? = null
    private var locationNetwork: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private var busList: MutableList<Marker> = mutableListOf()
    private var stopList: MutableList<Pair<Marker, Stop>> = mutableListOf()
    private var permissions =
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)

    var t: Timer = Timer()
    var ne = LatLng(0.toDouble(), 0.toDouble())
    var ne1 = 0.toDouble()
    var ne2 = 0.toDouble()
    var sw = LatLng(0.toDouble(), 0.toDouble())
    var sw1 = 0.toDouble()
    var sw2 = 0.toDouble()

    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap

        if (checkPermission(permissions)) {
            getLocation()
            map.uiSettings.isZoomControlsEnabled = true
            map.setOnMarkerClickListener(this)
            map.isMyLocationEnabled = true
            map.setOnCameraMoveListener(this)
        }
    }

    override fun onMarkerClick(p0: Marker?) = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCameraMove() {
        val bounds = map.projection.visibleRegion.latLngBounds
        ne = bounds.northeast
        sw = bounds.southwest
        t.purge()
        t.cancel()
        t = Timer()
        t.schedule(timerTask {
            if (ne1 != ne.latitude && ne2 != ne.longitude && sw1 != sw.latitude && sw2 != sw.longitude) {
                ne1 = ne.latitude
                ne2 = ne.longitude
                sw1 = sw.latitude
                sw2 = sw.longitude
                Log.d("Tag", "Refreshing data")
                repositoryInterface.getBox(sw1, sw2, ne1, ne2).enqueue(object : Callback<List<Transport>> {
                    override fun onFailure(call: Call<List<Transport>>, t: Throwable) {
                        Log.d("TAG", "Fail")
                        t.printStackTrace()
                    }

                    override fun onResponse(call: Call<List<Transport>>, response: Response<List<Transport>>) {
                        Log.d("TAG", "Response")
                        if (response.isSuccessful) {
                            deleteBus()
                            loadBus(response)
                        }
                    }

                })
                t.cancel()
            } else {
                ne1 = ne.latitude
                ne2 = ne.longitude
                sw1 = sw.latitude
                sw2 = sw.longitude
            }
            t.cancel()
        }, 1000)
    }

    private val repositoryInterface by lazy {
        RepositoryInterface.create()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        var frst: Boolean
        frst = true
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (hasGps || hasNetwork) {
            if (hasGps) {
                Log.d("CodeAndroidLocation", "hasGps")
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0F,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                if (frst) {
                                    frst = false
                                    loadGps(location)

                                } else {
                                    showGpsLocation(location)

                                }
                            }
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

                        }

                        override fun onProviderEnabled(provider: String?) {

                        }

                        override fun onProviderDisabled(provider: String?) {

                        }

                    })

                val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGpsLocation != null) {
                    if (frst) {
                        frst = false
                        loadGps(localGpsLocation)
                    } else {
                        showGpsLocation(localGpsLocation)

                    }
                }
                if (hasNetwork) {
                    Log.d("CodeAndroidLocation", "hasGps")
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        0F,
                        object : LocationListener {
                            override fun onLocationChanged(location: Location?) {
                                if (location != null) {
                                    if (frst) {
                                        frst = false
                                        loadNetwork(location)
                                    } else {
                                        showNetworkLocation(location)

                                    }
                                }
                            }

                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                            }

                            override fun onProviderEnabled(provider: String?) {

                            }

                            override fun onProviderDisabled(provider: String?) {

                            }

                        })

                    val localNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (localNetworkLocation != null) {
                        if (frst) {
                            frst = false
                            loadNetwork(localNetworkLocation)
                        } else {
                            showNetworkLocation(localNetworkLocation)

                        }
                    }

                    if (locationGPS != null && locationNetwork != null) {
                        if (locationGPS!!.accuracy > locationNetwork!!.accuracy) {
                            Log.d("CodeAndroidLocation", "Network Latitude: " + locationNetwork!!.latitude)
                            Log.d("CodeAndroidLocation", "Network Latitude: " + locationNetwork!!.longitude)
                        } else {
                            Log.d("CodeAndroidLocation", "GPS Latitude: " + locationGPS!!.latitude)
                            Log.d("CodeAndroidLocation", "GPS Latitude: " + locationGPS!!.longitude)
                        }
                    }
                }
            }

        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }


    private fun checkPermission(permissionArray: Array<String>): Boolean {
        var allSuccess = true
        for (i in permissionArray.indices) {
            if (checkCallingOrSelfPermission(permissionArray[i]) == PackageManager.PERMISSION_DENIED)
                allSuccess = false
        }
        return allSuccess
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            var allSuccess = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    allSuccess = false
                    val requestAgain =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(
                            permissions[i]
                        )
                    if (requestAgain) {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Go to settings and enable the permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (allSuccess) {
                getLocation()
            }

        }
    }

    private fun loadStops() {
        repositoryInterface.getStops().enqueue(object : Callback<List<Stop>> {

            override fun onFailure(call: Call<List<Stop>>, t: Throwable) {
                Log.d("loadStops", "Fail")
            }

            override fun onResponse(call: Call<List<Stop>>, response: Response<List<Stop>>) {
                Log.d("TAG", "Response")
                if (response.isSuccessful) {
                    val transports = response.body()!!
                    transports.forEach {
                        if (it.location != null) {
                            placeMarkStopOnMap(it)
                        }
                    }
                } else {
                    Log.d("loadStops", "unsuccesful")
                }
            }

        })
    }

    private fun loadGps(location: Location?) {
        locationGPS = location
        val current = LatLng(locationGPS!!.latitude, locationGPS!!.longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16.0f))
        loadStops()
    }

    private fun loadNetwork(location: Location?) {
        locationNetwork = location
        val current = LatLng(locationNetwork!!.latitude, locationNetwork!!.longitude)
        /*showGpsLocation(location)
        onCameraMove()*/
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16.0f))
        loadStops()
    }

    private fun showGpsLocation(location: Location?) {
        locationGPS = location
        val current = LatLng(locationGPS!!.latitude, locationGPS!!.longitude)
        Log.d("CodeAndroidLocation", "GPS Latitude: " + locationGPS!!.latitude)
        Log.d("CodeAndroidLocation", "Network Latitude: " + locationGPS!!.longitude)

        map.projection.visibleRegion.latLngBounds.southwest

        Log.d("CodeAndroidLocation", "GPS Latitude: " + locationGPS!!.latitude)
        Log.d("CodeAndroidLocation", "GPS LongLatitude: " + locationGPS!!.longitude)
    }

    private fun showNetworkLocation(location: Location?) {
        locationNetwork = location
        Log.d("CodeAndroidLocation", "Network Latitude: " + locationNetwork!!.latitude)
        Log.d("CodeAndroidLocation", "Network Latitude: " + locationNetwork!!.longitude)

        val current = LatLng(locationNetwork!!.latitude, locationNetwork!!.longitude)
        //map.addMarker(MarkerOptions().position(current).title("Current Possition"))

        map.projection.visibleRegion.latLngBounds.southwest

        Log.d("CodeAndroidLocation", "GPS Latitude: " + locationNetwork!!.latitude)
        Log.d("CodeAndroidLocation", "GPS LongLatitude: " + locationNetwork!!.longitude)
    }

    private fun deleteBus() {
        Log.d("Marker", "Bus delete")
        busList.forEach {
            it.remove()
        }
        busList.clear()
    }

    private fun loadBus(response: Response<List<Transport>>) {
        val transports = response.body()!!

        transports.forEach {
            if (it.location != null) {
                placeMarkBusOnMap(it)
            }
        }
    }

    private fun placeMarkBusOnMap(transport: Transport) {
        val mark = MarkerOptions().position(LatLng(transport.location?.latitude!!, transport.location?.longitude!!))
        val marker2 = IconGenerator(this)
        marker2.setTextAppearance(R.style.iconGenText)

        Log.d("Marker", "BusAdd")
        marker2.setColor(Color.parseColor("#f2d5d6"))
        busList.add(map.addMarker(mark.icon(BitmapDescriptorFactory.fromBitmap(marker2.makeIcon(transport.linename)))))

        //map.addMarker(mark.icon(BitmapDescriptorFactory.fromBitmap(marker2.makeIcon(transport.linename))))

    }

    private fun placeMarkStopOnMap(stop: Stop) {
        val mark = MarkerOptions().position(LatLng(stop.location?.latitude!!, stop.location?.longitude!!))
        val marker2 = IconGenerator(this)

        marker2.setTextAppearance(R.style.iconGenText)

        Log.d("Marker", "StopAdd")
        marker2.setColor(Color.parseColor("#e0e0ff"))
        val newMarker = map.addMarker(mark.icon(BitmapDescriptorFactory.fromBitmap(marker2.makeIcon(stop.name))))
        stopList.add(Pair(newMarker, stop))

        map.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(p0: Marker?): Boolean {
                stopList.forEach {
                    if (p0 == it.first) {
                        val stop = it.second
                        Log.d("TAG", stop.toString())
                        repositoryInterface.getDeparatures(stop.passport ?: 0, stop.banister ?: 0).enqueue(object : Callback<DepartureBoard>{
                            override fun onFailure(call: Call<DepartureBoard>, t: Throwable) {

                            }

                            override fun onResponse(call: Call<DepartureBoard>, response: Response<DepartureBoard>) {
                                if(response.isSuccessful){
                                    response.body()?.let {
                                        showDepartures(it)
                                    }
                                }
                            }

                        })
                    }
                }
                return true
            }
        })

    }

    private fun showDepartures(departureBoard: DepartureBoard) {
        val items = mutableListOf<String>()
        val icons = mutableListOf<Int>()
        departureBoard.departures?.forEach {
            items.add("${it?.lineName} - ${it?.departureTime}")
            icons.add(R.drawable.ic_bus)
        }
//        if(items.isEmpty())
//            return
        MaterialDialog(this, BottomSheet()).show {
            title(R.string.zastavky)
            listItems(items = items)
        }
    }


}


