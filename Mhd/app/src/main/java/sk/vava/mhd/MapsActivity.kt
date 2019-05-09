package sk.vava.mhd

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.customListAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.item_departure.view.*
import kotlinx.android.synthetic.main.item_departure.view.icon
import kotlinx.android.synthetic.main.item_stop.view.*
import mumayank.com.airlocationlibrary.AirLocation
import net.sharewire.googlemapsclustering.Cluster
import net.sharewire.googlemapsclustering.ClusterManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import sk.vava.mhd.ui.map.MapViewModel
import android.app.Activity
import android.view.inputmethod.InputMethodManager


class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnCameraIdleListener {
    private lateinit var map: GoogleMap

    private var airLocation: AirLocation? = null

    private lateinit var clusterManager: ClusterManager<MyItem>

    private var stops = mutableListOf<Stop>()
    private var requestedStop = ""

    val mapViewModel: MapViewModel by viewModel()

    override fun onMapReady(googleMap: GoogleMap) {

        //Gets the google map reference
        map = googleMap

        //Gets the Cluster manager reference for clustering markers
        clusterManager = ClusterManager(this, map)

        //Set custom icon generator, so we can have custom icons
        clusterManager.setIconGenerator(CustomIconGenerator(this))

        //We will need at least 4 items before the clustering will happen
        clusterManager.setMinClusterSize(4)

        //The callbacks to map, invoked after certain action
        clusterManager.setCallbacks(object : ClusterManager.Callbacks<MyItem> {
            override fun onClusterClick(cluster: Cluster<MyItem>): Boolean {
                Log.d("TAG", "Clicking on cluster")
                return false
            }

            override fun onClusterItemClick(it: MyItem): Boolean {
                Log.d("TAG", "Clicking on marker")
                if (it.banister != -1) {
                    mapViewModel.getDepartureBoard(it.passport, it.banister)
                    requestedStop = it.name
                }
                return false
            }

        })

        //The camera idle listener
        map.setOnCameraIdleListener(this@MapsActivity)

        //This will observe on Stops live data, will notify whenever they change
        mapViewModel.getStopsLiveData().observe(this, androidx.lifecycle.Observer {
            stops.addAll(it)
        })

        //This will observe on Transports live data, will notify whenever they change
        mapViewModel.getTransportsLiveData().observe(this, androidx.lifecycle.Observer {
            clusterManager.setItems(mutableListOf())
            placeMarkBusOnMap(it)
        })

        //This will observe on Departure board live data, will notify whenever they change
        mapViewModel.getDepartureBoardLiveData().observe(this, androidx.lifecycle.Observer {
            showDepartures(it)
        })

        //Set the on click listener on floating action button
        floatingActionButton.setOnClickListener {
            navigateToLocation()
        }

        search.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                println(s.toString())

                //Filter the stops by query
                val filtered = stops.filter { it.name != null && it.name.toLowerCase().contains(s.toString().toLowerCase()) }

                //If more than 3 letters
                if(s.toString().length > 3){
                    //Show if hidden
                    if(recycler.visibility == View.GONE){
                        recycler.visibility = View.VISIBLE
                    }
                    recycler.adapter =
                        StopsAdapter(filtered, this@MapsActivity, object : RecyclerViewListener {
                            override fun onItemClick(stop: Stop) {
                                if (stop.location != null) {
                                    //Animate the camera to stop
                                    map.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(stop.location.latitude!!, stop.location.longitude!!), 16.0f
                                        ), 2000, null
                                    )
                                    //Hide the list
                                    recycler.visibility = View.GONE
                                    //Hide the keyboard
                                    hideKeyboard(this@MapsActivity)
                                    //Clear the search
                                    search.setText("")
                                }
                            }

                        })
                    //Set the linear layout manager, so it will be shown vertically
                    recycler.layoutManager = LinearLayoutManager(this@MapsActivity)

                } else {
                    //Hide the list
                    if(recycler.visibility == View.VISIBLE){
                        recycler.visibility = View.GONE
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        //Let's start by loading the stops
        loadStops()

        //Start by navigating to my location
        navigateToLocation()
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onCameraIdle() {
        val projection = map.projection.visibleRegion.latLngBounds

        //Start clustering when camera is not moving
        clusterManager.onCameraIdle()

        //Get the transport in current box
        mapViewModel.getTransportsInBox(
            projection.southwest.latitude,
            projection.southwest.longitude,
            projection.northeast.latitude,
            projection.northeast.longitude
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        airLocation?.onActivityResult(requestCode, resultCode, data) // ADD THIS LINE INSIDE onActivityResult
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        airLocation?.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        ) // ADD THIS LINE INSIDE onRequestPermissionResult
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun loadStops() {
        mapViewModel.getStops()
    }

    private fun placeMarkBusOnMap(transports: List<Transport>) {

        val myItems = mutableListOf<MyItem>()

        //First add all transports
        transports.forEach { transport ->
            transport.location?.let {
                val item = MyItem(it.latitude ?: 0.toDouble(), it.longitude ?: 0.toDouble(), "")
                item.name = transport.linename.toString()
                myItems.add(item)
            }
        }

        //Then add all stops
        stops.forEach { stop ->
            stop.location?.let {
                val item = MyItem(it.latitude ?: 0.toDouble(), it.longitude ?: 0.toDouble(), stop.name.toString())
                item.banister = stop.banister ?: 0
                item.passport = stop.passport ?: 0
                item.name = stop.name.toString()
                myItems.add(item)
            }
        }

        //Add them to one cluster manager
        clusterManager.setItems(myItems)

    }

    private fun showDepartures(departureBoard: DepartureBoard) {
        //Show the bottom sheet dialog with actual departures
        MaterialDialog(this, BottomSheet()).show {
            title(text = getString(R.string.zastavky) + " - " + requestedStop)
            customListAdapter(
                DepartureAdapter(
                    departureBoard.departures ?: mutableListOf<Departure>(),
                    this@MapsActivity
                ), LinearLayoutManager(this@MapsActivity)
            )
        }
    }

    private fun navigateToLocation() {
        //Get the latest location
        airLocation = AirLocation(this, true, true, object : AirLocation.Callbacks {
            @SuppressLint("MissingPermission")
            override fun onSuccess(location: Location) {
                map.isMyLocationEnabled = true
                map.uiSettings.apply {
                    isCompassEnabled = false
                    isMapToolbarEnabled = false
                    isMyLocationButtonEnabled = false
                }

                //Animate camera to current location
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 16.0f
                    ), 2000, null
                )
            }

            override fun onFailed(locationFailedEnum: AirLocation.LocationFailedEnum) {
            }

        })
    }

    class DepartureAdapter(val items: List<Departure?>, val context: Context) : RecyclerView.Adapter<ViewHolder>() {

        // Gets the number of animals in the list
        override fun getItemCount(): Int {
            return items.size
        }

        // Inflates the item views
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_departure, parent, false))
        }

        // Binds each animal in the ArrayList to a view
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemView.lineName.text = items[position]?.lineName
            holder.itemView.departureTime.text = items[position]?.departureTime
            holder.itemView.delay.text = items[position]?.delay
            if (items[position]?.transportTypeName == "A") {
                holder.itemView.icon.setImageResource(R.drawable.ic_tram)
            } else {
                holder.itemView.icon.setImageResource(R.drawable.ic_bus)
            }
        }
    }

    class StopsAdapter(val items: List<Stop>, val context: Context, var listener: RecyclerViewListener) :
        RecyclerView.Adapter<ViewHolder>() {

        // Gets the number of animals in the list
        override fun getItemCount(): Int {
            return items.size
        }

        // Inflates the item views
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_stop, parent, false))
        }

        // Binds each animal in the ArrayList to a view
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemView.stopName.text = items[position].name
            holder.itemView.setOnClickListener {
                listener.onItemClick(items[position])
            }

        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each animal to
    }

    interface RecyclerViewListener {
        fun onItemClick(stop: Stop)
    }
}


