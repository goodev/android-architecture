/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.statistics

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.GravityCompat
import com.example.android.architecture.blueprints.todoapp.Injection
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.util.replaceFragmentInActivity
import com.example.android.architecture.blueprints.todoapp.util.setupActionBar
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.statistics_act.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * Show statistics for tasks.
 */
class StatisticsActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.statistics_act)

        // Set up the toolbar.
        setupActionBar(R.id.toolbar) {
            setTitle(R.string.statistics_title)
            setHomeAsUpIndicator(R.drawable.ic_menu)
            setDisplayHomeAsUpEnabled(true)
        }

        drawer_layout.setStatusBarBackground(R.color.colorPrimaryDark)
        setupDrawerContent(nav_view)

        val statisticsFragment = supportFragmentManager
                .findFragmentById(R.id.contentFrame) as StatisticsFragment?
                ?: StatisticsFragment.newInstance().also {
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }

        StatisticsPresenter(
                Injection.provideTasksRepository(applicationContext), statisticsFragment, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Open the navigation drawer when the home icon is selected from the toolbar.
            drawer_layout.openDrawer(GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.list_navigation_menu_item) {
                NavUtils.navigateUpFromSameTask(this@StatisticsActivity)
            }
            // Close the navigation drawer when an item is selected.
            menuItem.isChecked = true
            drawer_layout.closeDrawers()
            true
        }
    }
}
