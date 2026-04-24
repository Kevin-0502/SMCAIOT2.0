package com.example.smcaiot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    // Fragments
    private val entitiesFragment = EntitiesFragment()
    private val mapsFragment = MapsFragment()
    private val alertsFragment = AlertsFragment()
    private val profileFragment = ProfileFragment()

    private var activeFragment: Fragment = entitiesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            // Agregar todos los fragments y ocultar los que no son el inicial
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
                add(R.id.fragmentContainer, alertsFragment, "alerts").hide(alertsFragment)
                add(R.id.fragmentContainer, mapsFragment, "maps").hide(mapsFragment)
                add(R.id.fragmentContainer, entitiesFragment, "entities")
            }.commit()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_entities -> entitiesFragment
                R.id.nav_maps -> mapsFragment
                R.id.nav_alerts -> alertsFragment
                R.id.nav_profile -> profileFragment
                else -> entitiesFragment
            }

            if (selectedFragment != activeFragment) {
                supportFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(selectedFragment)
                    .commit()
                activeFragment = selectedFragment
            }
            true
        }
    }
}
