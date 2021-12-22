package bd.ac.ru.cse

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var fusedlocation: FusedLocationProviderClient
    private var myResquestCode = 1001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedlocation = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermission()) {
            if (locationEnable()) {
                fusedlocation.lastLocation.addOnCompleteListener { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        newLocation()
                    } else {
                        val lat = location.latitude.toString()
                        var long = location.longitude.toString()

                        if (lat != null && long != null) {
                            getJsonData(lat, long)
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please Turn on your GPS location", Toast.LENGTH_LONG).show()
            }
        } else {
            requestPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun newLocation() {
        var locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1
        fusedlocation = LocationServices.getFusedLocationProviderClient(this)
        fusedlocation.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            var lastLocation: Location = p0.lastLocation
        }
    }

    private fun locationEnable(): Boolean {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermission() {
        try {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), myResquestCode
            )
        } catch (e: Exception) {

        }
    }

    private fun checkPermission(): Boolean {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }


    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == myResquestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                requestPermission()
            }
        }
    }

    private fun getJsonData(lat: String, long: String) {
        val API_KEY = "ff7ac71987b94e62956cb97c003363b9" // use your own api_key
        val queue = Volley.newRequestQueue(this)
        val cityName = getCityName(lat.toDouble(), long.toDouble())

//      call API with city name
        val url =
            "https://api.openweathermap.org/data/2.5/weather?q=${cityName}&appid=${API_KEY}"

        try {
            val jsonRequest = JsonObjectRequest(
                Request.Method.GET, url, null,
                Response.Listener { response ->
                    setValues(response)
                },
                Response.ErrorListener {
                    Toast.makeText(
                        this,
                        "Please turn on internet connection",
                        Toast.LENGTH_LONG
                    ).show()
                })


            queue.add(jsonRequest)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "ERROR" + e.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setValues(response: JSONObject) {
        var country = response.getJSONObject("sys").getString("country")
        address.text = response.getString("name") + ", " + country

        updated_at.text = dateFormat(response.getString("dt").toLong()).toString()

        weather.text = response.getJSONArray("weather").getJSONObject(0).getString("description")

        var tempr = response.getJSONObject("main").getString("temp")
        temp.text = "${farToCel(tempr.toFloat()).toString()}°C"

        var mintemp = response.getJSONObject("main").getString("temp_min")
        temp_min.text = "Min Temp: ${farToCel(mintemp.toFloat()).toString()}°C"

        var maxtemp = response.getJSONObject("main").getString("temp_max")
        temp_max.text = "Max Temp:  ${farToCel(maxtemp.toFloat()).toString()}°C"

        var sunsetTime = response.getJSONObject("sys").getString("sunset").toLong()
        sunset.text = timeFormat(sunsetTime)

        var sunriseTime = response.getJSONObject("sys").getString("sunrise").toLong()
        sunrise.text = timeFormat(sunriseTime)

        pressure.text = response.getJSONObject("main").getString("pressure") + " hPa"
        humidity.text = response.getJSONObject("main").getString("humidity") + "%"
        wind.text = response.getJSONObject("wind").getString("speed")
        sea_level.text = response.getJSONObject("main").getString("sea_level") + " hPa"
    }

    private fun getCityName(lat: Double, long: Double): String {
        var geoCoder = Geocoder(this, Locale.getDefault())
        var adress = geoCoder.getFromLocation(lat, long, 3)

        return adress.get(0).locality
    }

    private fun farToCel(far: Float): Int {
        return (far - 273.15).toInt()
    }

    private fun dateFormat(milisecond: Long): String {
        return SimpleDateFormat(
            "EEE, d MMM yyyy hh:mm:ss a",
            Locale.ENGLISH
        ).format(Date(milisecond * 1000))
    }

    private fun timeFormat(time: Long): String {
        val formatter: DateFormat = SimpleDateFormat("hh:mm a", Locale.US)
        return formatter.format(Date(time * 1000)).toString()
    }
}