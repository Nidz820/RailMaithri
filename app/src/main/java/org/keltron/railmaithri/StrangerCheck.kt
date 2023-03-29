package org.keltron.railmaithri

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

class StrangerCheck: AppCompatActivity()  {

    private lateinit var progressPB:   ProgressBar
    private lateinit var saveBT:       Button
    private lateinit var locationUtil: LocationUtil
    private lateinit var fileUtil:     FileUtil

    private lateinit var nativeStateSP:               Spinner
    private lateinit var nameET:                      EditText
    private lateinit var identificationMarkDetailsET: EditText
    private lateinit var purposeOfVisitET:            EditText
    private lateinit var ageET:                       EditText
    private lateinit var languagesKnownET:            EditText
    private lateinit var mobileNumberET:              EditText
    private lateinit var placeOfCheckET:              EditText
    private lateinit var nativeAddressET:             EditText
    private lateinit var nativePoliceStationET:       EditText
    private lateinit var remarksET:                   EditText
    private lateinit var emailET:                     EditText
    private lateinit var statesAP:                    ArrayAdapter<String>

    private lateinit var mode:    String
    private lateinit var states:  JSONArray
    private lateinit var utcTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stranger_check)
        supportActionBar!!.hide()

        utcTime                      = Helper.getUTC()
        mode                         = intent.getStringExtra("mode")!!
        locationUtil                 = LocationUtil(this, findViewById(R.id.ly_location))
        fileUtil                     = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB                   = findViewById(R.id.progress_bar)
        saveBT                       = findViewById(R.id.save)
        nativeStateSP                = findViewById(R.id.native_state)
        nameET                       = findViewById(R.id.name)
        identificationMarkDetailsET  = findViewById(R.id.identification_mark_details)
        purposeOfVisitET             = findViewById(R.id.purpose_of_visit)
        ageET                        = findViewById(R.id.age)
        languagesKnownET             = findViewById(R.id.languages_known)
        mobileNumberET               = findViewById(R.id.contact_number)
        placeOfCheckET               = findViewById(R.id.place_of_check)
        nativeAddressET              = findViewById(R.id.native_address)
        nativePoliceStationET        = findViewById(R.id.native_police_station)
        remarksET                    = findViewById(R.id.remarks)
        emailET                      = findViewById(R.id.email)

        states   = JSONArray(Helper.getData(this, Scope.STATES_LIST)!!)
        statesAP = Helper.makeArrayAdapter(states, this)
        nativeStateSP.adapter = statesAP

        progressPB.visibility = View.GONE
        saveBT.setOnClickListener {
            val inputData = validateInput()
            inputData?.let {
                progressPB.visibility = View.VISIBLE
                saveBT.isClickable = false
                CoroutineScope(Dispatchers.IO).launch {
                    val file     = fileUtil.getFile()
                    val fileName = fileUtil.getFileName()
                    sendForm(inputData, file, fileName)
                    Handler(Looper.getMainLooper()).post {
                        saveBT.isClickable = true
                        progressPB.visibility = View.GONE
                    }
                }
            }
        }

        if (mode == Scope.MODE_VIEW_FORM){
            saveBT.visibility = View.GONE
            fileUtil.disableUpdate()
            locationUtil.disableUpdate()
        } else if (mode == Scope.MODE_UPDATE_FORM){
            val savedData = intent.getStringExtra("saved_data")
            val formData  = JSONObject(savedData!!)
            utcTime       = formData.getString("utc_timestamp")
            populateForm(formData)
        }
    }

    private fun populateForm(data: JSONObject){
        nameET.setText(data.getString("name"))
        identificationMarkDetailsET.setText(data.getString("identification_marks_details"))
        purposeOfVisitET.setText(data.getString("purpose_of_visit"))
        ageET.setText(data.getString("age"))
        languagesKnownET.setText(data.getString("languages_known"))
        emailET.setText(data.getString("email"))
        mobileNumberET.setText(data.getString("mobile_number"))
        placeOfCheckET.setText(data.getString("place_of_check"))
        nativeAddressET.setText(data.getString("native_address"))
        nativePoliceStationET.setText(data.getString("native_police_station"))
        remarksET.setText(data.getString("remarks"))

        val latitude  = data.getDouble("latitude")
        val longitude = data.getDouble("longitude")
        val accuracy  = data.getDouble("accuracy").toFloat()
        locationUtil.importLocation(latitude, longitude, accuracy)

        if(data.has("file_name")){
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }
        val stateNumber     = data.getInt("native_state")
        val stateName       = Helper.getName(states, stateNumber)
        val stateNumberPos  = statesAP.getPosition(stateName)
        nativeStateSP.setSelection(stateNumberPos)
    }

    private fun validateInput(): JSONObject? {
        val name                       = nameET.text.toString()
        val identificationMarkDetails  = identificationMarkDetailsET.text.toString()
        val purposeOfVisit             = purposeOfVisitET.text.toString()
        val age                        = ageET.text.toString()
        val languagesKnown             = languagesKnownET.text.toString()
        val mobileNumber               = mobileNumberET.text.toString()
        val placeOfCheck               = placeOfCheckET.text.toString()
        val nativeAddress              = nativeAddressET.text.toString()
        val nativePoliceStation        = nativePoliceStationET.text.toString()
        val remarks                    = remarksET.text.toString()
        val email                      =  emailET.text.toString()

        val stateNumberPos    = nativeStateSP.selectedItemPosition
        val stateNumber       = states.getJSONObject(stateNumberPos).getString("id").toString()

        if (!locationUtil.haveLocation()){
            Helper.showToast(this, "Location is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(name.isEmpty()){
            Helper.showToast(this, "Name is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(identificationMarkDetails.isEmpty()){
            Helper.showToast(this, "identification mark details is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(purposeOfVisit.isEmpty()){
            Helper.showToast(this, "Purpose of visit is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(age.isEmpty()){
            Helper.showToast(this, "Age is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(languagesKnown.isEmpty()){
            Helper.showToast(this, "Languages known is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(mobileNumber.isEmpty()){
            Helper.showToast(this, "Mobile number is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(placeOfCheck.isEmpty()){
            Helper.showToast(this, "Place of check is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(nativeAddress.isEmpty()){
            Helper.showToast(this, "Native address is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(nativePoliceStation.isEmpty()){
            Helper.showToast(this, "Native police station is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(remarks.isEmpty()){
            Helper.showToast(this, "Remarks is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("name", name)
        formData.put("identification_marks_details", identificationMarkDetails)
        formData.put("purpose_of_visit", purposeOfVisit)
        formData.put("utc_timestamp", utcTime)
        formData.put("age", age)
        formData.put("email", email)
        formData.put("languages_known", languagesKnown)
        formData.put("mobile_number", mobileNumber)
        formData.put("checking_date_time", utcTime)
        formData.put("place_of_check", placeOfCheck)
        formData.put("native_address", nativeAddress)
        formData.put("native_police_station", nativePoliceStation)
        formData.put("native_state", stateNumber)
        formData.put("remarks", remarks)
        locationUtil.exportLocation(formData)
        return formData
    }

    private fun sendForm(formData: JSONObject, file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.STRANGER_CHECK, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM){
                    removeStrangerCheck()
                }
                Helper.showToast(this, "Stranger check reported", Toast.LENGTH_SHORT)
                finish()
            } else {
                val apiResponse  = response.body!!.string()
                val errorMessage = Helper.getError(apiResponse)
                Helper.showToast(this, errorMessage, Toast.LENGTH_LONG)
            }
        } catch (e: Exception) {
            Log.d("RailMaithri", e.stackTraceToString())
            saveForm(formData)
        }
    }

    private fun saveForm(formData: JSONObject) {
        if (mode == Scope.MODE_UPDATE_FORM) {
            removeStrangerCheck()
        }
        val savedStr  = Helper.getObject(this, Scope.STRANGER_CHECK)!!
        val savedData = JSONObject(savedStr)
        if (fileUtil.haveFile()) {
            fileUtil.saveFile(this)
            formData.put("file_name", fileUtil.getFileName())
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.STRANGER_CHECK, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeStrangerCheck() {
        val savedStr  = Helper.getObject(this, Scope.STRANGER_CHECK)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.STRANGER_CHECK, savedData.toString())
        fileUtil.removeFile(this)
    }
}
