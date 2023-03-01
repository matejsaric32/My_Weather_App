package matejsaric32.android.myweatherapp.activity

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import matejsaric32.android.myweatherapp.R
import matejsaric32.android.myweatherapp.databinding.ActivityMainBinding
import matejsaric32.android.myweatherapp.models.WeatherResponse
import matejsaric32.android.myweatherapp.network.WeatherServices
import matejsaric32.android.myweatherapp.utils.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var mProgressDialog: Dialog? = null


    private var currentLocation: Location? = null
    lateinit var locationManager: LocationManager
    private var num = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (isLocationEnabled()) {
            Toast.makeText(this, "Location is enabled", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "Your location provider is turned off. Please turn it on.", Toast.LENGTH_SHORT).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)


        }

        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))

//        getLocalWeatherDetails()


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                getLocalWeatherDetails()
                Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setUpDataResponse(weatherResponse: WeatherResponse){
        for (element in weatherResponse.weather!!.indices){
            binding?.tvWeather?.text = weatherResponse.weather[element].main
            binding?.tvWeatherDescription?.text = weatherResponse.weather[element].description

            binding?.tvHumidityDescription?.text = weatherResponse.main?.humidity.toString() + "%"

            binding?.tvTemperature?.text = weatherResponse.main?.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            binding?.tvTemperatureDescription?.text =
                "${weatherResponse.main?.temp_min .toString() + getUnit(application.resources.configuration.locales.toString())} - " +
                        "${weatherResponse.main?.temp_max.toString() + getUnit(application.resources.configuration.locales.toString())}"

            binding?.tvWind?.text = weatherResponse.wind?.speed.toString()
            binding?.tvWindDescription?.text = getUnitForWind(application.resources.configuration.locales.toString())

            binding?.tvCountry?.text = weatherResponse.sys?.country
            binding?.tvLocality?.text = weatherResponse.name

            binding?.tvSunriseTime?.text = convertTime(weatherResponse.sys?.sunrise!!)
            binding?.tvSunsetTime?.text = convertTime(weatherResponse.sys?.sunset!!)

        }
    }

    private fun convertTime(timex: Int): String? {
        val date = Date(timex * 1000L)
        if(application.resources.configuration.locales.toString() == "US"){
            val sdf = SimpleDateFormat("hh:mm a")
            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(date)
        }else{
            val sdf = SimpleDateFormat("HH:mm")
            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(date)
        }

    }

    private fun getUnitForWind(value: String): String? {
        var value = "km/h"
        if ("US" == value) {
            value = "mph"
        }
        return value
    }

    private fun getUnit(value: String): String? {
        var value = "°C"
        if ("US" == value) {
            value = "°F"
        }
        return value
    }

    private fun showDialog(){
        mProgressDialog = Dialog(this)
        mProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog?.show()
    }

    private fun hideDialog(){
        if(mProgressDialog != null){
            mProgressDialog?.dismiss()
        }
    }

    private fun getLocalWeatherDetails(){
        if(Constants.isNetworkAvailable(this)){
            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Constants.URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service : WeatherServices =
                retrofit.create(WeatherServices::class.java)

            val location = getCurrentLocation()
            Log.e("Location", "${location.toString()}")
            val listCall: Call<WeatherResponse> =
            service.getWeather(location?.latitude!!, location.longitude, Constants.METRIC_UNIT, Constants.API_KEY)

            showDialog()

            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response.isSuccessful){
                        val weatherList : WeatherResponse? = response.body()
                        num++
                        Log.e("Response Result", "$num")
                        Log.e("Response Result", "$weatherList")
                        setUpDataResponse(weatherList!!)
                    }else{
                        val rc = response.code()
                        when(rc){
                            400 -> Log.e("Error 400", "Bad connection")
                            404 -> Log.e("Error 404", "Not found")
                            else -> Log.e("Error", "Generic error")
                        }
                    }

                    hideDialog()
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Error", t.message.toString())
                    hideDialog()
                }

            })

        }
    }

    fun getCurrentLocation() : Location? {

        var location : Location? = null

        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        } else{
            val locationManager2 = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            location = locationManager2.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (location != null) {
                Log.e("Current Location", "Latitude: ${currentLocation?.latitude} Longitude: ${currentLocation?.longitude}")
                currentLocation = location
            }else{
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }

        return location
    }

    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Toast.makeText(this, "Fine location permission granted", Toast.LENGTH_SHORT).show()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Toast.makeText(this, "Coarse location permission granted", Toast.LENGTH_SHORT).show()
            } else -> {
                showRationalDialogForPermissions()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )

    }

    fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = android.net.Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }


}