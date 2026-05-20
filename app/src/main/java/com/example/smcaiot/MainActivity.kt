package com.example.smcaiot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var entitiesFragment: Fragment
    private lateinit var mapsFragment: Fragment
    private lateinit var alertsFragment: Fragment
    private lateinit var profileFragment: Fragment

    private lateinit var activeFragment: Fragment

    companion object {
        private const val KEY_ACTIVE_TAG = "active_fragment_tag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            // Primera vez: crear todos los fragments
            entitiesFragment = EntitiesFragment()
            mapsFragment = MapsFragment()
            alertsFragment = AlertsFragment()
            profileFragment = ProfileFragment()

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
                add(R.id.fragmentContainer, alertsFragment, "alerts").hide(alertsFragment)
                add(R.id.fragmentContainer, mapsFragment, "maps").hide(mapsFragment)
                add(R.id.fragmentContainer, entitiesFragment, "entities")
            }.commit()

            activeFragment = entitiesFragment
        } else {
            // Restaurar fragments existentes del FragmentManager
            entitiesFragment = supportFragmentManager.findFragmentByTag("entities") ?: EntitiesFragment()
            mapsFragment = supportFragmentManager.findFragmentByTag("maps") ?: MapsFragment()
            alertsFragment = supportFragmentManager.findFragmentByTag("alerts") ?: AlertsFragment()
            profileFragment = supportFragmentManager.findFragmentByTag("profile") ?: ProfileFragment()

            // Restaurar cuál fragment estaba activo
            val activeTag = savedInstanceState.getString(KEY_ACTIVE_TAG, "entities")
            activeFragment = supportFragmentManager.findFragmentByTag(activeTag) ?: entitiesFragment
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Guardar el tag del fragment activo para restaurarlo
        val activeTag = when (activeFragment) {
            entitiesFragment -> "entities"
            mapsFragment -> "maps"
            alertsFragment -> "alerts"
            profileFragment -> "profile"
            else -> "entities"
        }
        outState.putString(KEY_ACTIVE_TAG, activeTag)
    }
}
