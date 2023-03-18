package org.keltron.railmaithri

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject


class IncidentReport : AppCompatActivity() {
    private lateinit var clientNT:        OkHttpClient
    private lateinit var progressPB:      ProgressBar
    private lateinit var saveBT:          Button
    private lateinit var selectFileBT:    Button
    private lateinit var openFileBT:      Button
    private lateinit var getLocationBT:   Button
    private lateinit var openLocationBT:  Button
    private lateinit var railwayStations: JSONArray
    private lateinit var trains:          JSONArray

    private lateinit var incidentTypeSP:     Spinner
    private lateinit var railwayStationSP:   Spinner
    private lateinit var platformNumberET:   EditText
    private lateinit var trackLocationET:    EditText
    private lateinit var trainSP:            Spinner
    private lateinit var coachNumberET:      EditText
    private lateinit var contactNumberET:    EditText
    private lateinit var detailsET:          EditText
    private lateinit var location:           Location
    private lateinit var locationDataTV:     TextView
    private lateinit var locationAccuracyTV: TextView
    private lateinit var file:               ByteArray
    private lateinit var fileName:           String
    private lateinit var fileNameTV:         TextView

    private val FILE_REQUEST = 111
    private val PLATFORM     = 0
    private val TRACK        = 1
    private val TRAIN        = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.incident_report)
        supportActionBar!!.hide()

        clientNT           = OkHttpClient().newBuilder().build()
        progressPB         = findViewById(R.id.progress_bar)
        saveBT             = findViewById(R.id.save)
        selectFileBT       = findViewById(R.id.select_file)
        openFileBT         = findViewById(R.id.open_file)
        getLocationBT      = findViewById(R.id.get_location)
        openLocationBT     = findViewById(R.id.open_location)
        
        incidentTypeSP     = findViewById(R.id.incident_type)
        railwayStationSP   = findViewById(R.id.railway_station)
        platformNumberET   = findViewById(R.id.platform_number)
        trackLocationET    = findViewById(R.id.track_location)
        trainSP            = findViewById(R.id.train)
        coachNumberET      = findViewById(R.id.coach_number)
        contactNumberET    = findViewById(R.id.contact_number)
        detailsET          = findViewById(R.id.details)
        locationDataTV     = findViewById(R.id.location_data)
        locationAccuracyTV = findViewById(R.id.location_accuracy)
        fileNameTV         = findViewById(R.id.file_name)

        railwayStations = JSONArray(Helper.getData(this, Scope.RAILWAY_STATIONS_LIST)!!)
        railwayStationSP.adapter = Helper.makeArrayAdapter(railwayStations, this)
        trains = JSONArray(Helper.getData(this, Scope.TRAINS_LIST)!!)
        trainSP.adapter = Helper.makeArrayAdapter(trains, this)

        progressPB.visibility = View.GONE
        file = ByteArray(0)
        fileName = ""
        ArrayAdapter.createFromResource(
            this,
            R.array.incident_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            incidentTypeSP.adapter = adapter
        }
        incidentTypeSP.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val railwayStationLY = findViewById<LinearLayout>(R.id.ly_railway_station)
                val platformNumberLY = findViewById<LinearLayout>(R.id.ly_platform_number)
                val trackLocationLY  = findViewById<LinearLayout>(R.id.ly_track_location)
                val trainLY          = findViewById<LinearLayout>(R.id.ly_train)
                val coachNumberLY    = findViewById<LinearLayout>(R.id.ly_coach_number)
                val contactNumberLY  = findViewById<LinearLayout>(R.id.ly_contact_number)

                railwayStationLY.visibility = View.GONE
                platformNumberLY.visibility = View.GONE
                trackLocationLY.visibility = View.GONE
                trainLY.visibility = View.GONE
                coachNumberLY.visibility = View.GONE
                contactNumberET.visibility = View.GONE

                when (position) {
                    PLATFORM -> {
                        railwayStationLY.visibility = View.VISIBLE
                        platformNumberLY.visibility = View.VISIBLE
                    }
                    TRACK -> {
                        railwayStationLY.visibility = View.VISIBLE
                        trackLocationLY.visibility = View.VISIBLE
                    }
                    TRAIN -> {
                        trainLY.visibility = View.VISIBLE
                        coachNumberLY.visibility = View.VISIBLE
                        contactNumberLY.visibility = View.VISIBLE
                    }
                }
            }
        }
        getLocationBT.setOnClickListener { fetchLocation() }
        openLocationBT.setOnClickListener {
            if (::location.isInitialized) {
                Helper.openMap(this, location)
            } else{
                Helper.showToast(this, "Unknown location", Toast.LENGTH_SHORT)
            }
        }
        selectFileBT.setOnClickListener { selectFile() }
        saveBT.setOnClickListener {
            val inputData = validateInput()
            if (inputData != null){
                progressPB.visibility = View.VISIBLE
                saveBT.isClickable = false
                CoroutineScope(Dispatchers.IO).launch { saveForm(inputData) }
            }
        }
    }

    private fun validateInput(): JSONObject? {
        val coachNumber    = coachNumberET.text.toString()
        val contactNumber  = contactNumberET.text.toString()
        val platformNumber = platformNumberET.text.toString()
        val trackLocation  = trackLocationET.text.toString()
        val details        = detailsET.text.toString()
        val utcTime        = Helper.getUTC()

        val trainNumberPos    = trainSP.selectedItemPosition
        val trainNumber       = trains.getJSONObject(trainNumberPos).getString("id").toString()
        val railwayStationPos = railwayStationSP.selectedItemPosition
        val stationNumber     = railwayStations.getJSONObject(railwayStationPos).getString("id").toString()

        if (!::location.isInitialized){
            Helper.showToast(this, "Location is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(details.isEmpty()){
            Helper.showToast(this, "Incident detail is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val requestData = JSONObject()
        requestData.put("status", "1")
        requestData.put("data_from", "Beat Officer")
        requestData.put("incident_date_time", utcTime)
        requestData.put("utc_timestamp", utcTime)
        requestData.put("incident_details", details)
        requestData.put("latitude", location.latitude)
        requestData.put("longitude", location.longitude)

        when (incidentTypeSP.selectedItemPosition) {
            PLATFORM -> {
                if(platformNumber.isEmpty()){
                    Helper.showToast(this, "Platform number is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                requestData.put("incident_type","Platform")
                requestData.put("railway_station", stationNumber)
                requestData.put("platform_number", platformNumber)
            }
            TRACK -> {
                if(trackLocation.isEmpty()){
                    Helper.showToast(this, "Track location is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                requestData.put("incident_type","Track")
                requestData.put("railway_station", stationNumber)
                requestData.put("track_location", trackLocation)
            }
            TRAIN -> {
                if(coachNumber.isEmpty()){
                    Helper.showToast(this, "Coach number is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                if(contactNumber.isEmpty()){
                    Helper.showToast(this, "Contact number is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                requestData.put("incident_type", "Train")
                requestData.put("train_number", trainNumber)
                requestData.put("coach", coachNumber)
                requestData.put("mobile_number", contactNumber)
            }
        }
        return requestData
    }

    private fun saveForm(input: JSONObject) {
        try {
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.INCIDENT_REPORT, input, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                Helper.showToast(this, "Incident reported", Toast.LENGTH_SHORT)
                Log.e("RailMaithri", "Incident reported")
                finish()
            } else {
                val apiResponse = response.body!!.string()
                Log.d("RailMaithri", apiResponse)
                val errorMessage = Helper.getError(apiResponse)
                Helper.showToast(this, errorMessage, Toast.LENGTH_LONG)
            }
        } catch (e: Exception) {
            Helper.showToast(this, "Something went wrong !!", Toast.LENGTH_LONG)
            Log.d("RailMaithri", e.stackTraceToString())
        } finally {
            Handler(Looper.getMainLooper()).post {
                saveBT.isClickable = true
                progressPB.visibility = View.GONE
            }
        }
    }

    private fun selectFile() {
        val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_REQUEST)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                file = inputStream.use { it?.readBytes() }!!
                inputStream?.close()
                fileName = Helper.getFileName(this, uri)
                fileNameTV.text = fileName
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        if (!Helper.haveLocationPermission(this)) {
            Helper.showToast(this, "No GPS !!", Toast.LENGTH_SHORT)
        } else {
            locationDataTV.text = "Locating ...."
            Helper.getLocation(this, fun(gpsLocation: Location?) {
                if (gpsLocation != null) {
                    location = gpsLocation
                    val latitude = location.latitude.toString().substring(0, 8)
                    val longitude = location.longitude.toString().substring(0, 8)
                    locationDataTV.text = "Location : ${latitude}, ${longitude}"
                    locationAccuracyTV.text = "Accuracy : ${location.accuracy}m"
                } else {
                    locationDataTV.text = "Accuracy : unknown !!"
                }
            })
        }
    }
}