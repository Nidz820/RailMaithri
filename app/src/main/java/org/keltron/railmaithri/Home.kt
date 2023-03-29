package org.keltron.railmaithri

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
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

    }
}