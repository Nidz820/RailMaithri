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
import java.io.File

class IncidentReport : AppCompatActivity() {
    private lateinit var clientNT:              OkHttpClient
    private lateinit var progressPB:            ProgressBar
    private lateinit var saveBT:                Button
    private lateinit var selectFileBT:          Button
    private lateinit var deleteFileBT:          Button
    private lateinit var getLocationBT:         Button
    private lateinit var openLocationBT:        Button
    private lateinit var incidentTypeSP:        Spinner
    private lateinit var railwayStationSP:      Spinner
    private lateinit var platformNumberET:      EditText
    private lateinit var trackLocationET:       EditText
    private lateinit var trainSP:               Spinner
    private lateinit var coachNumberET:         EditText
    private lateinit var contactNumberET:       EditText
    private lateinit var detailsET:             EditText
    private lateinit var locationDataTV:        TextView
    private lateinit var locationAccuracyTV:    TextView
    private lateinit var fileNameTV:            TextView
    private lateinit var railwayStationsAP:     ArrayAdapter<String>
    private lateinit var trainsAP:              ArrayAdapter<String>

    private lateinit var mode:                  String
    private lateinit var railwayStations:       JSONArray
    private lateinit var trains:                JSONArray
    private var          haveLocation:          Boolean    = false
    private var          latitude:              Double?    = null
    private var          longitude:             Double?    = null
    private var          accuracy:              Float?     = null
    private var          haveFile:              Boolean    = false
    private var          file:                  ByteArray? = null
    private var          fileName:              String?    = null
    private var          utcTime:               String     = Helper.getUTC()

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
        saveBT             = findViewById(R.id.sync)
        selectFileBT       = findViewById(R.id.select_file)
        deleteFileBT       = findViewById(R.id.delete_file)
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

        railwayStations          = JSONArray(Helper.getData(this, Scope.RAILWAY_STATIONS_LIST)!!)
        railwayStationsAP        = Helper.makeArrayAdapter(railwayStations, this)
        railwayStationSP.adapter = railwayStationsAP

        trains          = JSONArray(Helper.getData(this, Scope.TRAINS_LIST)!!)
        trainsAP        = Helper.makeArrayAdapter(trains, this)
        trainSP.adapter = trainsAP

        progressPB.visibility = View.GONE
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
                updateForm(position)
            }
        }
        getLocationBT.setOnClickListener { fetchLocation() }
        openLocationBT.setOnClickListener {
            if (haveLocation) {
                Helper.openMap(this, latitude!!, longitude!!)
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
                CoroutineScope(Dispatchers.IO).launch { sendForm(inputData) }
            }
        }
        deleteFileBT.setOnClickListener { updateFileName(null) }

        mode = intent.getStringExtra("mode")!!
        if (mode == Scope.MODE_VIEW_FORM){
            saveBT.visibility = View.GONE
            deleteFileBT.isClickable = false
            getLocationBT.isClickable = false
        } else if (mode == Scope.MODE_UPDATE_FORM){
            val savedData = intent.getStringExtra("saved_data")
            val formData  = JSONObject(savedData!!)
            utcTime       = formData.getString("utc_timestamp")
            populateForm(utcTime, formData)
            Log.e("Railmaithri", formData.toString())
        }
    }

    private fun populateForm(uuid: String, data: JSONObject){
        detailsET.setText(data.getString("incident_details"))
        updateLocation(data.getDouble("latitude"),
            data.getDouble("longitude"),
            data.getDouble("accuracy").toFloat())
        try{
            fileName = data.getString("file_name")
            file     = Helper.getFile(this, uuid)
            updateFileName(fileName!!)
        }catch (_: Exception){}

        when (data.getString("incident_type")) {
            "Platform" -> {
                incidentTypeSP.setSelection(PLATFORM)
                platformNumberET.setText(data.getString("platform_number"))

                val railwayStationID   = data.getInt("railway_station")
                val railwayStationName = Helper.getName(railwayStations, railwayStationID)
                val railwayStationPos  = railwayStationsAP.getPosition(railwayStationName)
                railwayStationSP.setSelection(railwayStationPos)
            }
            "Track" -> {
                incidentTypeSP.setSelection(TRACK)
                trackLocationET.setText(data.getString("track_location"))

                val railwayStationID   = data.getInt("railway_station")
                val railwayStationName = Helper.getName(railwayStations, railwayStationID)
                val railwayStationPos  = railwayStationsAP.getPosition(railwayStationName)
                railwayStationSP.setSelection(railwayStationPos)
            }
            "Train" -> {
                incidentTypeSP.setSelection(TRAIN)
                contactNumberET.setText(data.getString("mobile_number"))
                coachNumberET.setText(data.getString("coach"))

                val trainNumber     = data.getInt("train_number")
                val trainName       = Helper.getName(trains, trainNumber)
                val trainNumberPos  = trainsAP.getPosition(trainName)
                trainSP.setSelection(trainNumberPos)
            }
        }
    }

    private fun updateForm(position: Int){
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
        contactNumberLY.visibility = View.GONE

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

    private fun validateInput(): JSONObject? {
        val coachNumber    = coachNumberET.text.toString()
        val contactNumber  = contactNumberET.text.toString()
        val platformNumber = platformNumberET.text.toString()
        val trackLocation  = trackLocationET.text.toString()
        val details        = detailsET.text.toString()

        val trainNumberPos    = trainSP.selectedItemPosition
        val trainNumber       = trains.getJSONObject(trainNumberPos).getString("id").toString()
        val railwayStationPos = railwayStationSP.selectedItemPosition
        val stationNumber     = railwayStations.getJSONObject(railwayStationPos).getString("id").toString()

        if (!haveLocation){
            Helper.showToast(this, "Location is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(details.isEmpty()){
            Helper.showToast(this, "Incident detail is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("status", "1")
        formData.put("data_from", "Beat Officer")
        formData.put("incident_date_time", utcTime)
        formData.put("utc_timestamp", utcTime)
        formData.put("incident_details", details)
        formData.put("latitude", latitude)
        formData.put("longitude", longitude)
        formData.put("accuracy", accuracy)

        when (incidentTypeSP.selectedItemPosition) {
            PLATFORM -> {
                if(platformNumber.isEmpty()){
                    Helper.showToast(this, "Platform number is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                formData.put("incident_type","Platform")
                formData.put("railway_station", stationNumber)
                formData.put("platform_number", platformNumber)
            }
            TRACK -> {
                if(trackLocation.isEmpty()){
                    Helper.showToast(this, "Track location is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                formData.put("incident_type","Track")
                formData.put("railway_station", stationNumber)
                formData.put("track_location", trackLocation)
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
                formData.put("incident_type", "Train")
                formData.put("train_number", trainNumber)
                formData.put("coach", coachNumber)
                formData.put("mobile_number", contactNumber)
            }
        }
        return formData
    }

    private fun sendForm(formData: JSONObject) {
        try {
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.INCIDENT_REPORT, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM){
                    removeIncident(utcTime)
                }
                Helper.showToast(this, "Incident reported", Toast.LENGTH_SHORT)
                finish()
            } else {
                val apiResponse  = response.body!!.string()
                val errorMessage = Helper.getError(apiResponse)
                Helper.showToast(this, errorMessage, Toast.LENGTH_LONG)
            }
        } catch (e: Exception) {
            Log.d("RailMaithri", e.stackTraceToString())
            saveForm(formData)
        } finally {
            Handler(Looper.getMainLooper()).post {
                saveBT.isClickable = true
                progressPB.visibility = View.GONE
            }
        }
    }

    private fun saveForm(formData: JSONObject) {
        if (mode == Scope.MODE_UPDATE_FORM){
           removeIncident(utcTime)
        }
        val savedStr  = Helper.getObject(this, Scope.INCIDENT_REPORT)!!
        val savedData = JSONObject(savedStr)
        if (haveFile) {
            Helper.saveFile(this, file!!, utcTime)
            formData.put("file_name", fileName)
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.INCIDENT_REPORT, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
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
                updateFileName(Helper.getFileName(this, uri))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        if (!Helper.haveLocationPermission(this)) {
            Helper.showToast(this, "No GPS !!", Toast.LENGTH_SHORT)
        } else {
            locationDataTV.text = "Locating ...."
            Helper.getLocation(this, fun(location: Location?) {
                if (location != null) {
                    updateLocation(location.latitude, location.longitude, location.accuracy)
                } else {
                    locationAccuracyTV.text = "Accuracy : unknown !!"
                    locationDataTV.text     = "Location : unknown !!"
                }
            })
        }
    }

    private  fun updateFileName(_fileName: String?) {
        if (_fileName != null){
            deleteFileBT.isClickable = true
            haveFile        = true
            fileName        = _fileName
            fileNameTV.text = _fileName
        } else{
            deleteFileBT.isClickable = false
            haveFile        = false
            fileNameTV.text = "No file selected"
        }
    }

    private fun updateLocation(_latitude: Double, _longitude: Double, _accuracy: Float){
        haveLocation = true
        latitude     = _latitude
        longitude    = _longitude
        accuracy     = _accuracy

        val latitudeString      = latitude.toString().substring(0, 8)
        val longitudeString     = longitude.toString().substring(0, 8)
        locationDataTV.text     = "Location : ${latitudeString}, ${longitudeString}"
        locationAccuracyTV.text = "Accuracy : ${accuracy}m"
    }

    private fun removeIncident(uuid: String){
        try{
            val savedStr  = Helper.getObject(this, Scope.INCIDENT_REPORT)!!
            val savedData = JSONObject(savedStr)
            savedData.remove(utcTime)
            Helper.saveData(this, Scope.INCIDENT_REPORT, savedData.toString())
        }catch (e: Exception){ }

        val storedFile = File(uuid)
        if (storedFile.exists()) storedFile.delete()
    }
}