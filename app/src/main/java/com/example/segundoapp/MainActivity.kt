package com.example.segundoapp

import android.os.Bundle
import android.os.Handler
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.segundoapp.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference


    val db = FirebaseFirestore.getInstance()
    var handler = Handler()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

     binding = ActivityMainBinding.inflate(layoutInflater)
     setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        }

    fun consulta(){
        val collectionRef = db.collection("IntegrasData")
        collectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                for (documentSnapshot in querySnapshot) {
                    // Acesso aos dados de cada documento retornado
                    val data = documentSnapshot.data
                    // Lógica para manipular os dados
                }
            }
            .addOnFailureListener { exception ->
                // Lógica para lidar com erros
            }

    }







}

