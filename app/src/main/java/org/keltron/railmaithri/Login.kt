package org.keltron.railmaithri

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject

class Login : AppCompatActivity() {
    lateinit var clientNT:      OkHttpClient
    lateinit var progressPB:    ProgressBar
    lateinit var loginBT:       Button
    lateinit var usernameET:    EditText
    lateinit var passwordET:    EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        supportActionBar!!.hide()
        requestPermissions()

        clientNT   = OkHttpClient().newBuilder().build()
        progressPB = findViewById(R.id.progress_bar)
        loginBT    = findViewById(R.id.login)
        usernameET = findViewById(R.id.username)
        passwordET = findViewById(R.id.password)

        progressPB.visibility = View.GONE
        val token = Helper.getData(this, Scope.TOKEN)!!
        if (token != ""){
            startActivity(Intent(this, Home::class.java))
            finish()
        }

        loginBT.setOnClickListener {
            val username = usernameET.text.toString()
            val password = passwordET.text.toString()
            if(username.isBlank() or password.isBlank()) {
                val message = "Both username and password are required to login"
                Helper.showToast(this, message, Toast.LENGTH_SHORT)
            } else {
                loginBT.isClickable   = false
                progressPB.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.IO).launch {  login(username, password)  }
            }
        }
    }

    private fun login(username: String, password: String) {
        try {
            val request  = API.loginRequest(username, password)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                val authData = JSONObject(response.body!!.string())
                val profile  = authData.getJSONObject("user")
                val token    = authData.getString("token")

                startCaching(token)
                Helper.saveData(this, Scope.PROFILE, profile.toString())
                Helper.saveData(this, Scope.TOKEN, token)
                startActivity(Intent(this, Home::class.java))
                finish()
            } else {
                val apiResponse = response.body!!.string()
                Log.d("RailMaithri", apiResponse)
                val errorMessage = Helper.getError(apiResponse)
                Helper.showToast(this, errorMessage, Toast.LENGTH_LONG)
            }
        } catch (e: Exception) {
            Helper.showToast(this, "Server unreachable !!", Toast.LENGTH_LONG)
            Log.d("RailMaithri", e.stackTraceToString())
        } finally {
            Handler(Looper.getMainLooper()).post {
                loginBT.isClickable   = true
                progressPB.visibility = View.GONE
            }
        }
    }

    private fun requestPermissions() {
        val appPermissions = arrayOf(
            permission.ACCESS_FINE_LOCATION,
            permission.READ_EXTERNAL_STORAGE,
            permission.INTERNET
        )
        val neededPermissions = ArrayList<String>()
        for (permission in appPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission)
            }
        }
        if(neededPermissions.isNotEmpty()) {
            val requestCode = 108
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), requestCode)
        }
    }

    private fun startCaching(token: String) {
        cacheData(API.RAILWAY_STATIONS_LIST, Scope.RAILWAY_STATIONS_LIST,token)         
        cacheData(API.TRAINS_LIST, Scope.TRAINS_LIST,token)                   
        cacheData(API.INTELLIGENCE_SEVERITY_TYPES, Scope.INTELLIGENCE_SEVERITY_TYPES,token)   
        cacheData(API.INTELLIGENCE_TYPES, Scope.INTELLIGENCE_TYPES,token)            
        cacheData(API.COMPARTMENT_TYPES, Scope.COMPARTMENT_TYPES,token)             
        cacheData(API.DENSITY_TYPES, Scope.DENSITY_TYPES,token)                 
        cacheData(API.MEETING_TYPES, Scope.MEETING_TYPES,token)                 
        cacheData(API.POI_TYPES, Scope.POI_TYPES,token)                     
        cacheData(API.POLICE_STATIONS_LIST, Scope.POLICE_STATIONS_LIST,token)          
        cacheData(API.DISTRICTS_LIST, Scope.DISTRICTS_LIST,token)                
        cacheData(API.STATES_LIST, Scope.STATES_LIST,token)                   
        cacheData(API.ABANDONED_PROPERTY_TYPES, Scope.ABANDONED_PROPERTY_TYPES,token)      
        cacheData(API.RAIL_VOLUNTEER_TYPES, Scope.RAIL_VOLUNTEER_TYPES,token)          
        cacheData(API.GENDER_TYPES, Scope.GENDER_TYPES,token)                  
        cacheData(API.CONTACT_TYPES, Scope.CONTACT_TYPES,token)                 
        cacheData(API.WATCH_ZONE_TYPES, Scope.WATCH_ZONE_TYPES,token)              
        cacheData(API.VENDOR_TYPES, Scope.VENDOR_TYPES,token)                  
        cacheData(API.LOST_PROPERTY_TYPES, Scope.LOST_PROPERTY_TYPES,token)           
        cacheData(API.FOUND_IN_TYPES, Scope.FOUND_IN_TYPES,token)                
        cacheData(API.SURAKSHA_SAMITHI_LIST, Scope.SURAKSHA_SAMITHI_LIST,token)         
        cacheData(API.SHOP_TYPES, Scope.SHOP_TYPES,token)                    
        cacheData(API.CRIME_MEMO_TYPES, Scope.CRIME_MEMO_TYPES,token)              
    }

    private fun cacheData(apiURL: String, scope: String, token: String): Boolean {
        val request  = API.getRequest(apiURL, token)
        val response = clientNT.newCall(request).execute()
        return if (response.isSuccessful) {
            val cachedData = response.body!!.string()
            Helper.saveData(this, scope, cachedData)
            Log.d("RailMaithri", Helper.getData(this, scope)!!)
            true
        } else {
            Log.d("RailMaithri", "Failed to cache $apiURL")
            false
        }
    }
}