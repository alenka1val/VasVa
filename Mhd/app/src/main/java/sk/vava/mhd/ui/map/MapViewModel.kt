package sk.vava.mhd.ui.map

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sk.vava.mhd.DepartureBoard
import sk.vava.mhd.RepositoryInterface
import sk.vava.mhd.Stop
import sk.vava.mhd.Transport

class MapViewModel(val repositoryInterface: RepositoryInterface) : ViewModel() {

    private var trasports = MutableLiveData<List<Transport>>()
    private var stops = MutableLiveData<List<Stop>>()
    private var departureBoard = MutableLiveData<DepartureBoard>()

    fun getTransportsLiveData(): LiveData<List<Transport>>{
        return trasports
    }
    fun getStopsLiveData(): LiveData<List<Stop>>{
        return stops
    }
    fun getDepartureBoardLiveData(): LiveData<DepartureBoard>{
        return departureBoard
    }

    fun getTransportsInBox(
        latBottomLeft: Double,
        lonBottomLeft: Double,
        latTopRight: Double,
        lonTopRight: Double
    ) {
        repositoryInterface.getBox(latBottomLeft, lonBottomLeft, latTopRight, lonTopRight).enqueue(object : Callback<List<Transport>> {
            override fun onFailure(call: Call<List<Transport>>, t: Throwable) {
                Log.e(MapViewModel::class.java.simpleName, "Error getting transport", t)
            }

            override fun onResponse(call: Call<List<Transport>>, response: Response<List<Transport>>) {
                if (response.isSuccessful) {
                    trasports.postValue(response.body())
                }
            }

        })
    }

    fun getStops(){
        repositoryInterface.getStops().enqueue(object : Callback<List<Stop>>{
            override fun onFailure(call: Call<List<Stop>>, t: Throwable) {
                Log.e(MapViewModel::class.java.simpleName, "Error getting stops", t)
            }

            override fun onResponse(call: Call<List<Stop>>, response: Response<List<Stop>>) {
                if(response.isSuccessful){
                    stops.postValue(response.body())
                }
            }

        })
    }

    fun getDepartureBoard(passport: Int, banister: Int){
        repositoryInterface.getDeparatures(passport, banister).enqueue(object : Callback<DepartureBoard>{
            override fun onFailure(call: Call<DepartureBoard>, t: Throwable) {
                Log.e(MapViewModel::class.java.simpleName, "Error getting Departure Board", t)
            }

            override fun onResponse(call: Call<DepartureBoard>, response: Response<DepartureBoard>) {
                if(response.isSuccessful){
                    departureBoard.postValue(response.body())
                }
            }

        })
    }
}