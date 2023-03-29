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

class PassengerStatistics: AppCompatActivity() {

    private lateinit var progressPB:         ProgressBar
    private lateinit var saveBT:             Button
    private lateinit var trainSP:            Spinner
    private lateinit var densitySP:          Spinner
    private lateinit var compartmentTypeSP:  Spinner
    private lateinit var coachNumberET:      EditText

    private lateinit var trainsAP:           ArrayAdapter<String>
    private lateinit var densityAP:          ArrayAdapter<String>
    private lateinit var compartmentTypeAP:  ArrayAdapter<String>

    private lateinit var mode:               String
    private lateinit var densities:          JSONArray
    private lateinit var trains:             JSONArray
    private lateinit var compartmentTypes:   JSONArray
    private lateinit var utcTime:            String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.passenger_statistics)
        supportActionBar!!.hide()

        utcTime           = Helper.getUTC()
        mode              = intent.getStringExtra("mode")!!
        progressPB        = findViewById(R.id.progress_bar)
        saveBT            = findViewById(R.id.save)
        trainSP           = findViewById(R.id.train)
        densitySP         = findViewById(R.id.density)
        compartmentTypeSP = findViewById(R.id.compartment_type)
        coachNumberET     = findViewById(R.id.coach_number)

        densitys          = JSONArray(Helper.getData(this, Scope.DENSITY_TYPES)!!)
        densityAP         = Helper.makeArrayAdapter(densitys, this)
        densitySP.adapter = densityAP

        trains          = JSONArray(Helper.getData(this, Scope.TRAINS_LIST)!!)
        trainsAP        = Helper.makeArrayAdapter(trains, this)
        trainSP.adapter = trainsAP

        compartmentTypes          = JSONArray(Helper.getData(this, Scope.COMPARTMENT_TYPES)!!)
        compartmentTypeAP         = Helper.makeArrayAdapter(compartmentTypes, this)
        compartmentTypeSP.adapter = compartmentTypeAP

        progressPB.visibility = View.GONE
        saveBT.setOnClickListener {
            val inputData = validateInput()
            inputData?.let {
                progressPB.visibility = View.VISIBLE
                saveBT.isClickable = false
                CoroutineScope(Dispatchers.IO).launch {
                    sendForm(inputData)
                    Handler(Looper.getMainLooper()).post {
                        saveBT.isClickable = true
                        progressPB.visibility = View.GONE
                    }
                }
            }
        }

        if (mode == Scope.MODE_VIEW_FORM) {
            saveBT.visibility = View.GONE
        } else if (mode == Scope.MODE_UPDATE_FORM) {
            val savedData = intent.getStringExtra("saved_data")
            val formData = JSONObject(savedData!!)
            utcTime = formData.getString("utc_timestamp")
            populateForm(formData)
        }
    }

    private fun populateForm(data: JSONObject) {
        coachNumberET.setText(data.getString("coach"))
    }

    private fun validateInput(): JSONObject? {
        val coachNumber      = coachNumberET.text.toString()
        val trainNumberPos   = trainSP.selectedItemPosition
        val trainNumber      = trains.getJSONObject(trainNumberPos).getString("id").toString()
        val densityPos       = densitySP.selectedItemPosition
        val densityNumber    = densities.getJSONObject(densityPos).getString("id").toString()
        val compartmentTypePos    = compartmentTypeSP.selectedItemPosition
        val compartmentTypeNumber = compartmentTypes.getJSONObject(compartmentTypePos).getString("id").toString()

        if (coachNumber.isEmpty()) {
            Helper.showToast(this, "Coach NUmber is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("train", trainNumber)
        formData.put("coach", coachNumber)
        formData.put("density", densityNumber)
        formData.put("last_updated", utcTime)
        formData.put("utc_timestamp", utcTime)
        formData.put("compartment_type", compartmentTypeNumber)
        return formData
    }

    private fun sendForm(formData: JSONObject) {
        try {
            val clientNT  = OkHttpClient().newBuilder().build()
            val token     = Helper.getData(this, Scope.TOKEN)
            val request   = API.postRequest(token!!, API.PASSENGER_STATISTICS, formData,file = null,fileName = null)
            val response  = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removePassengerStatistics()
                }
                Helper.showToast(this, "Passenger statistics reported", Toast.LENGTH_SHORT)
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
            removePassengerStatistics()
        }
        val savedStr  = Helper.getObject(this, Scope.PASSENGER_STATISTICS)!!
        val savedData = JSONObject(savedStr)
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.PASSENGER_STATISTICS, savedData.toString())
        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removePassengerStatistics() {
        val savedStr  = Helper.getObject(this, Scope.PASSENGER_STATISTICS)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.PASSENGER_STATISTICS, savedData.toString())
    }
}
