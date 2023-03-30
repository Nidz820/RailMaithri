package org.keltron.railmaithri

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File


class SavedData : AppCompatActivity() {
    private lateinit var clientNT:    OkHttpClient
    private lateinit var progressPB:  ProgressBar
    private lateinit var savedDataLY: LinearLayout
    private lateinit var formNameTV:  TextView
    private lateinit var syncBT:      Button
    private lateinit var scope:       String

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_data)
        supportActionBar!!.hide()

        clientNT    = OkHttpClient().newBuilder().build()
        savedDataLY = findViewById(R.id.ly_saved_data)
        formNameTV  = findViewById(R.id.form_name)
        syncBT      = findViewById(R.id.sync)
        progressPB  = findViewById(R.id.progress_bar)
        scope       = intent.getStringExtra("scope")!!

        progressPB.visibility = View.GONE
        formNameTV.text       = Helper.prettify(scope)
        syncBT.setOnClickListener {
            syncBT.isClickable = false
            progressPB.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch { syncData() }
        }
    }

    private fun loadData() {
        savedDataLY.removeAllViews()
        val savedData = JSONObject(Helper.getObject(this, scope)!!)
        val uuids     = savedData.keys()
        while (uuids.hasNext()) {
            val uuid       = uuids.next()
            val savedDatum = savedData.getJSONObject(uuid)
            injectDatum(uuid, savedDatum)
        }
    }

    private fun removeDatum(uuid: String){
        try{
            val savedStr  = Helper.getObject(this, scope)!!
            val savedData = JSONObject(savedStr)
            savedData.remove(uuid)            
            Helper.saveData(this, scope, savedData.toString())
        }catch (_: Exception){ }

        val storedFile = File(uuid)
        if (storedFile.exists()) storedFile.delete()
    }

    private fun injectDatum(uuid: String, savedDatum: JSONObject){
        val button  = Button(this)
        button.text = uuid
        savedDataLY.addView(button)

        button.setOnClickListener {
            var intent: Intent? = null
            if(scope == Scope.INCIDENT_REPORT){
                intent = Intent(this, IncidentReport::class.java)
            }else if(scope == Scope.PASSENGER_STATISTICS){
                intent = Intent(this, PassengerStatistics::class.java)
            }else if(scope == Scope.STRANGER_CHECK){
                intent = Intent(this, StrangerCheck::class.java)
            }else if(scope == Scope.BEAT_DIARY){
                intent = Intent(this, BeatDiary::class.java)
            }else if(scope == Scope.POI){
                intent = Intent(this, Poi::class.java)
            }else if(scope == Scope.EMERGENCY_CONTACTS){
                intent = Intent(this, EmergencyContacts::class.java)
            }else if(scope == Scope.LOST_PROPERTY) {
                intent = Intent(this, LostProperty::class.java)
            }else if(scope == Scope.ABANDONED_PROPERTY) {
                intent = Intent(this, AbandonedProperty::class.java)
            }else if(scope == Scope.RELIABLE_PERSON) {
                intent = Intent(this, ReliablePerson::class.java)
            }else if(scope == Scope.INTELLIGENCE_INFORMATION) {
                intent = Intent(this, IntelligenceInformation::class.java)
            }

            intent!!.putExtra("mode", Scope.MODE_UPDATE_FORM)
            intent.putExtra("saved_data", savedDatum.toString())
            startActivity(intent)
        }
    }

    private fun syncData() {
        val clientNT  = OkHttpClient().newBuilder().build()
        val savedData = JSONObject(Helper.getObject(this, scope)!!)
        val uuids     = savedData.keys()
        val token     = Helper.getData(this, Scope.TOKEN)

        while (uuids.hasNext()) {
            val uuid       = uuids.next()
            val savedDatum = savedData.getJSONObject(uuid)
            var fileName: String?   = null
            var file: ByteArray?    = null
            try{
                fileName = savedDatum.getString("file_name")
                file     = Helper.loadFile(this, uuid)
            }catch (_: Exception){}

            var apiURL = ""
            if(scope == Scope.INCIDENT_REPORT){
                apiURL = API.INCIDENT_REPORT
            }else if(scope == Scope.PASSENGER_STATISTICS){
                apiURL = API.PASSENGER_STATISTICS
            }else if(scope == Scope.STRANGER_CHECK){
                apiURL = API.STRANGER_CHECK
            }else if(scope == Scope.BEAT_DIARY){
                apiURL = API.BEAT_DIARY
            }else if(scope == Scope.POI){
                apiURL = API.POI
            }else if(scope == Scope.EMERGENCY_CONTACTS){
                apiURL = API.EMERGENCY_CONTACTS
            }else if(scope ==Scope.LOST_PROPERTY){
                apiURL =API.LOST_PROPERTY
            }else if(scope ==Scope.ABANDONED_PROPERTY){
                apiURL =API.ABANDONED_PROPERTY
            }else if(scope ==Scope.RELIABLE_PERSON){
                apiURL =API.RELIABLE_PERSON
            }else if(scope ==Scope.INTELLIGENCE_INFORMATION){
                apiURL =API.INTELLIGENCE_INFORMATION
            }

            try {
                val request  = API.postRequest(token!!, apiURL, savedDatum, file, fileName)
                val response = clientNT.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("RailMaithri", "Incident synced")
                    removeDatum(uuid)
                } else {
                    Helper.showToast(this, "Sync failed !!", Toast.LENGTH_SHORT)
                    break
                }
            } catch (e: Exception) {
                Log.d("RailMaithri", e.stackTraceToString())
                Helper.showToast(this, "Sync failed !!", Toast.LENGTH_SHORT)
            } finally {
                Handler(Looper.getMainLooper()).post {
                    syncBT.isClickable = true
                    progressPB.visibility = View.GONE
                    loadData()
                }
            }
        }
    }
}
