package com.example.tomatize

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import ui.AddHabitDialog
import ui.Habit
import ui.HabitDatabaseHelper
import ui.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var databaseHelper: HabitDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = HabitDatabaseHelper(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.addHabitFragment -> {
                    showAddHabitDialog()
                    false
                }
                else -> {
                    navController.navigate(item.itemId)
                    true
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
    }

    private fun showAddHabitDialog() {
        val dialog = AddHabitDialog()

        dialog.setOnHabitAddedListener(object : AddHabitDialog.OnHabitAddedListener {
            override fun onHabitAdded(habit: Habit) {
                // Сохраняем привычку в базу данных
                val id = databaseHelper.addHabit(habit)

                if (id != -1L) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Привычка добавлена!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()


                    val navHostFragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()

                    if (currentFragment is HomeFragment) {
                        currentFragment.refreshHabits()
                    } else {
                        val navController = navHostFragment.navController
                        navController.navigate(R.id.homeFragment)
                    }
                }
            }
        })

        dialog.show(supportFragmentManager, "AddHabitDialog")
    }
}