/*
 * Copyright 2016, The Android Open Source Project
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

package com.example.android.architecture.blueprints.todoapp.addedittask

import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.support.test.espresso.IdlingResource
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.example.android.architecture.blueprints.todoapp.Injection
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.addFragmentToActivity

/**
 * Displays an add or edit task screen.
 */
class AddEditTaskActivity : AppCompatActivity() {

  private lateinit var mAddEditTaskPresenter: AddEditTaskPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.addtask_act)

    // Set up the toolbar.
    val toolbar = findViewById(R.id.toolbar) as Toolbar
    setSupportActionBar(toolbar)
    val actionBar = supportActionBar
    actionBar?.let {
      actionBar.setDisplayHomeAsUpEnabled(true)
      actionBar.setDisplayShowHomeEnabled(true)
    }

    val taskId = intent.getStringExtra(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID)
    var addEditTaskFragment: AddEditTaskFragment? = supportFragmentManager.findFragmentById(R.id.contentFrame) as AddEditTaskFragment?
    if (addEditTaskFragment == null) {
      addEditTaskFragment = AddEditTaskFragment.newInstance()

      if (intent.hasExtra(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID)) {
        actionBar?.setTitle(R.string.edit_task)
      } else {
        actionBar?.setTitle(R.string.add_task)
      }

      addFragmentToActivity(addEditTaskFragment, R.id.contentFrame)
    }

    val shouldLoadDataFromRepo = savedInstanceState?.getBoolean(SHOULD_LOAD_DATA_FROM_REPO_KEY) ?: true

    // Create the presenter
    mAddEditTaskPresenter = AddEditTaskPresenter(
        taskId,
        Injection.provideTasksRepository(applicationContext),
        addEditTaskFragment,
        shouldLoadDataFromRepo,
        Injection.provideSchedulerProvider())
  }

  override fun onSaveInstanceState(outState: Bundle) {
    // Save the state so that next time we know if we need to refresh data.
    outState.putBoolean(SHOULD_LOAD_DATA_FROM_REPO_KEY, mAddEditTaskPresenter.isDataMissing)
    super.onSaveInstanceState(outState)
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  val countingIdlingResource: IdlingResource
    @VisibleForTesting
    get() = EspressoIdlingResource.idlingResource

  companion object {

   const val REQUEST_ADD_TASK = 1

    const val SHOULD_LOAD_DATA_FROM_REPO_KEY = "SHOULD_LOAD_DATA_FROM_REPO_KEY"
  }
}
