package com.example.tomatize

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.navigation.fragment.NavHostFragment
import ui.AddHabitDialog
import ui.Habit
import ui.HabitDatabaseHelper
import ui.HabitReminderWorker
import ui.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var databaseHelper: HabitDatabaseHelper
    private lateinit var selectorOval: View
    private lateinit var navButtons: List<FrameLayout>
    private lateinit var navIcons: List<ImageView>
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkNotificationPermission()

        // Handle Splash Screen
        val splashScreen = findViewById<View>(R.id.splash_screen)
        splashScreen.postDelayed({
            splashScreen.animate()
                .alpha(0f)
                .setDuration(500)
                .setInterpolator(AccelerateInterpolator())
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreen.visibility = View.GONE
                    }
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
        }, 1000)

        databaseHelper = HabitDatabaseHelper(this)
        scheduleDailyReminder()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val rootLayout = findViewById<View>(R.id.main_root)
        val customBottomNav = findViewById<View>(R.id.custom_bottom_nav)
        
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top)
            customBottomNav.updatePadding(bottom = systemBars.bottom)
            windowInsets
        }

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

        selectorOval.post {
            moveSelector(0, animate = false)
        }
    }

    private fun animateSelector(newIndex: Int) {
        val container = findViewById<View>(R.id.nav_buttons_container)
        val tabWidth = container.width / 5f
        val startX = currentIndex * tabWidth + (tabWidth - selectorOval.width) / 2f
        val endX = newIndex * tabWidth + (tabWidth - selectorOval.width) / 2f

        val moveAnimator = ObjectAnimator.ofFloat(selectorOval, "translationX", startX, endX)
        
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

        updateNavColors(newIndex)
        animatorSet.start()
        currentIndex = newIndex
    }

    private fun moveSelector(index: Int, animate: Boolean) {
        val container = findViewById<View>(R.id.nav_buttons_container)
        if (container.width == 0) return
        
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
            if (index != 2) {
                val color = if (index == activeIndex) activeColor else inactiveColor
                ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color))
            }
        }
    }

    // Missing methods added here
    fun setNavBarVisibility(visible: Boolean) {
        findViewById<View>(R.id.custom_bottom_nav).visibility = if (visible) View.VISIBLE else View.GONE
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
                    android.widget.Toast.makeText(this@MainActivity, "Привычка добавлена!", android.widget.Toast.LENGTH_SHORT).show()
                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                    if (currentFragment is HomeFragment) currentFragment.refreshHabits()
                }
            }
        })
        dialog.show(supportFragmentManager, "AddHabitDialog")
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun scheduleDailyReminder() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20) // Время напоминания
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<HabitReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HabitDailyReminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )

    }
}