package org.keltron.railmaithri

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject


class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        supportActionBar!!.hide()

        val profile  = JSONObject(Helper.getData(this, Scope.PROFILE)!!)
        val username = profile.getString("username")
        findViewById<TextView>(R.id.profile_name).text = username

        val logoutButton = findViewById<ImageButton>(R.id.logout)
        logoutButton.setOnClickListener {
            Helper.saveData(this, Scope.TOKEN, "")
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        val addIncidentBT    = findViewById<ImageView>(R.id.add_incident_report)
        val searchIncidentBT = findViewById<Button>(R.id.search_incident_report)
        val savedIncidentBT  = findViewById<Button>(R.id.saved_incident_report)
        addIncidentBT.setOnClickListener {
            val intent = Intent(this, IncidentReport::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        savedIncidentBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.INCIDENT_REPORT)
            startActivity(intent)
        }
        searchIncidentBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.INCIDENT_REPORT)
            startActivity(intent)
        }

        val addPassengerStatisticsBT    = findViewById<ImageView>(R.id.add_passenger_statistics)
        val searchPassengerStatisticsBT = findViewById<Button>(R.id.search_passenger_statistics)
        val savedPassengerStatisticsBT  = findViewById<Button>(R.id.saved_passenger_statistics)
        addPassengerStatisticsBT.setOnClickListener {
            val intent = Intent(this, PassengerStatistics::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        savedPassengerStatisticsBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.PASSENGER_STATISTICS)
            startActivity(intent)
        }
        searchPassengerStatisticsBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.PASSENGER_STATISTICS)
            startActivity(intent)
        }
    }
}