package org.keltron.railmaithri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        supportActionBar!!.hide()
    }
}