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

package com.example.android.architecture.blueprints.todoapp.tasks

import android.app.Activity
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import rx.Observable
import rx.subscriptions.CompositeSubscription

/**
 * Listens to user actions from the UI ([TasksFragment]), retrieves the data and updates the
 * UI as required.
 */
class TasksPresenter(val tasksRepository: TasksRepository,
                     val tasksView: TasksContract.View,
                     val schedulerProvider: BaseSchedulerProvider) : TasksContract.Presenter {

  /**
   * Sets the current task filtering type.

   * @param requestType Can be [TasksFilterType.ALL_TASKS],
   * *                    [TasksFilterType.COMPLETED_TASKS], or
   * *                    [TasksFilterType.ACTIVE_TASKS]
   */
  override var filtering = TasksFilterType.ALL_TASKS

  private var mFirstLoad = true

  private val mSubscriptions: CompositeSubscription = CompositeSubscription()

  init {
    tasksView.setPresenter(this)
  }

  override fun subscribe() {
    loadTasks(false)
  }

  override fun unsubscribe() {
    mSubscriptions.clear()
  }

  override fun result(requestCode: Int, resultCode: Int) {
    // If a task was successfully added, show snackbar
    if (AddEditTaskActivity.REQUEST_ADD_TASK == requestCode && Activity.RESULT_OK == resultCode) {
      tasksView.showSuccessfullySavedMessage()
    }
  }

  override fun loadTasks(forceUpdate: Boolean) {
    // Simplification for sample: a network reload will be forced on first load.
    loadTasks(forceUpdate || mFirstLoad, true)
    mFirstLoad = false
  }

  /**
   * @param forceUpdate   Pass in true to refresh the data in the [TasksDataSource]
   * *
   * @param showLoadingUI Pass in true to display a loading icon in the UI
   */
  private fun loadTasks(forceUpdate: Boolean, showLoadingUI: Boolean) {
    if (showLoadingUI) {
      tasksView.setLoadingIndicator(true)
    }
    if (forceUpdate) {
      tasksRepository.refreshTasks()
    }

    // The network request might be handled in a different thread so make sure Espresso knows
    // that the app is busy until the response is handled.
    EspressoIdlingResource.increment() // App is busy until further notice

    mSubscriptions.clear()
    val subscription = tasksRepository
        .tasks
        .flatMap { tasks -> Observable.from(tasks) }
        .filter { task ->
          when (filtering) {
            TasksFilterType.ACTIVE_TASKS -> {
              task.isActive
            }

            TasksFilterType.COMPLETED_TASKS -> {
              task.isCompleted
            }

            TasksFilterType.ALL_TASKS -> {
              true
            }

            else ->{
              true
            }
          }
        }
        .toList()
        .subscribeOn(schedulerProvider.computation())
        .observeOn(schedulerProvider.ui())
        .doOnTerminate {
          if (!EspressoIdlingResource.idlingResource.isIdleNow()) {
            EspressoIdlingResource.decrement() // Set app as idle.
          }
        }
        .subscribe(
            // onNext
            { processTasks(it) },
            // onError
            { throwable -> tasksView.showLoadingTasksError() },
            // onCompleted
            { tasksView.setLoadingIndicator(false) }
        )
    mSubscriptions.add(subscription)
  }

  private fun processTasks(tasks: List<Task>) {
    if (tasks.isEmpty()) {
      // Show a message indicating there are no tasks for that filter type.
      processEmptyTasks()
    } else {
      // Show the list of tasks
      tasksView.showTasks(tasks)
      // Set the filter label's text.
      showFilterLabel()
    }
  }

  private fun showFilterLabel() {
    when (filtering) {
      TasksFilterType.ACTIVE_TASKS -> tasksView.showActiveFilterLabel()
      TasksFilterType.COMPLETED_TASKS -> tasksView.showCompletedFilterLabel()
      else -> tasksView.showAllFilterLabel()
    }
  }

  private fun processEmptyTasks() {
    when (filtering) {
      TasksFilterType.ACTIVE_TASKS -> tasksView.showNoActiveTasks()
      TasksFilterType.COMPLETED_TASKS -> tasksView.showNoCompletedTasks()
      else -> tasksView.showNoTasks()
    }
  }

  override fun addNewTask() {
    tasksView.showAddTask()
  }

  override fun openTaskDetails(requestedTask: Task) {
    tasksView.showTaskDetailsUi(requestedTask.id)
  }

  override fun completeTask(completedTask: Task) {
    tasksRepository.completeTask(completedTask)
    tasksView.showTaskMarkedComplete()
    loadTasks(false, false)
  }

  override fun activateTask(activeTask: Task) {
    tasksRepository.activateTask(activeTask)
    tasksView.showTaskMarkedActive()
    loadTasks(false, false)
  }

  override fun clearCompletedTasks() {
    tasksRepository.clearCompletedTasks()
    tasksView.showCompletedTasksCleared()
    loadTasks(false, false)
  }

}
