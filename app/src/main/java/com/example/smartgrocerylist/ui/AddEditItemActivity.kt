package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgrocerylist.R

class AddEditItemActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the XML layout for this screen
        setContentView(R.layout.activity_add_edit_item)

        //Cancel button: close this screen and return to previous
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            finish()
        }
    }
}

