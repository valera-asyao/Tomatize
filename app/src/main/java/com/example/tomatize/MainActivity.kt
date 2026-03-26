package com.example.tomatize

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.navigation.fragment.NavHostFragment
import ui.AddHabitDialog
import ui.Habit
import ui.HabitDatabaseHelper
import ui.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var databaseHelper: HabitDatabaseHelper
    private lateinit var selectorOval: View
    private lateinit var navButtons: List<FrameLayout>
    private lateinit var navIcons: List<ImageView>
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force light mode to be independent of system theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = HabitDatabaseHelper(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Initialize Custom Bottom Nav
        selectorOval = findViewById(R.id.nav_selector_oval)
        navButtons = listOf(
            findViewById(R.id.nav_home),
            findViewById(R.id.nav_statistics),
            findViewById(R.id.nav_add),
            findViewById(R.id.nav_shop),
            findViewById(R.id.nav_profile)
        )
        navIcons = listOf(
            findViewById(R.id.iv_home),
            findViewById(R.id.iv_statistics),
            findViewById(R.id.iv_add),
            findViewById(R.id.iv_shop),
            findViewById(R.id.iv_profile)
        )

        val destinationIds = listOf(
            R.id.homeFragment,
            R.id.statisticsFragment,
            R.id.addHabitFragment,
            R.id.shopFragment,
            R.id.profileFragment
        )

        navButtons.forEachIndexed { index, frameLayout ->
            frameLayout.setOnClickListener {
                val destinationId = destinationIds[index]
                if (destinationId == R.id.addHabitFragment) {
                    showAddHabitDialog()
                } else {
                    if (navController.currentDestination?.id != destinationId) {
                        navController.navigate(destinationId)
                    }
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val index = destinationIds.indexOf(destination.id)
            if (index != -1 && index != currentIndex) {
                animateSelector(index)
            }
        }

        // Initial position after layout
        selectorOval.post {
            moveSelector(0, animate = false)
        }
    }

    private fun animateSelector(newIndex: Int) {
        val container = findViewById<View>(R.id.nav_buttons_container)
        val tabWidth = container.width / 5f
        val startX = currentIndex * tabWidth + (tabWidth - selectorOval.width) / 2f
        val endX = newIndex * tabWidth + (tabWidth - selectorOval.width) / 2f

        // Move Animation
        val moveAnimator = ObjectAnimator.ofFloat(selectorOval, "translationX", startX, endX)
        
        // Stretch Animation (Rubber effect)
        // We stretch more if the distance is larger, but a fixed scale also looks good
        val stretchAnimator = ValueAnimator.ofFloat(1f, 1.25f, 1f)
        stretchAnimator.addUpdateListener { animator ->
            val scale = animator.animatedValue as Float
            selectorOval.scaleX = scale
        }

        val animatorSet = AnimatorSet().apply {
            playTogether(moveAnimator, stretchAnimator)
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Update colors
        updateNavColors(newIndex)
        
        animatorSet.start()
        currentIndex = newIndex
    }

    private fun moveSelector(index: Int, animate: Boolean) {
        val container = findViewById<View>(R.id.nav_buttons_container)
        if (container.width == 0) return // Wait for layout
        
        val tabWidth = container.width / 5f
        val targetX = index * tabWidth + (tabWidth - selectorOval.width) / 2f
        
        if (animate) {
            animateSelector(index)
        } else {
            selectorOval.translationX = targetX
            updateNavColors(index)
            currentIndex = index
        }
    }

    private fun updateNavColors(activeIndex: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.nav_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)

        navIcons.forEachIndexed { index, imageView ->
            val color = if (index == activeIndex) activeColor else inactiveColor
            ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color))
        }
    }

    fun disableNavBarSelection() {
        findViewById<View>(R.id.nav_buttons_container).isEnabled = false
        navButtons.forEach { it.isEnabled = false }
    }

    fun enableNavBarSelection() {
        findViewById<View>(R.id.nav_buttons_container).isEnabled = true
        navButtons.forEach { it.isEnabled = true }
    }

    private fun showAddHabitDialog() {
        val dialog = AddHabitDialog()

        dialog.setOnHabitAddedListener(object : AddHabitDialog.OnHabitAddedListener {
            override fun onHabitAdded(habit: Habit) {
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