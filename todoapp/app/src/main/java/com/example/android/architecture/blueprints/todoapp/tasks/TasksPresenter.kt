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
import com.google.common.base.Preconditions.checkNotNull
import rx.Observable
import rx.subscriptions.CompositeSubscription

/**
 * Listens to user actions from the UI ([TasksFragment]), retrieves the data and updates the
 * UI as required.
 */
class TasksPresenter(tasksRepository: TasksRepository,
                     tasksView: TasksContract.View,
                     schedulerProvider: BaseSchedulerProvider) : TasksContract.Presenter {

  private val mTasksRepository: TasksRepository

  private val mTasksView: TasksContract.View

  private val mSchedulerProvider: BaseSchedulerProvider

  /**
   * Sets the current task filtering type.

   * @param requestType Can be [TasksFilterType.ALL_TASKS],
   * *                    [TasksFilterType.COMPLETED_TASKS], or
   * *                    [TasksFilterType.ACTIVE_TASKS]
   */
  override var filtering = TasksFilterType.ALL_TASKS

  private var mFirstLoad = true

  private val mSubscriptions: CompositeSubscription

  init {
    mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null")
    mTasksView = checkNotNull(tasksView, "tasksView cannot be null!")
    mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null")

    mSubscriptions = CompositeSubscription()
    mTasksView.setPresenter(this)
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
      mTasksView.showSuccessfullySavedMessage()
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
      mTasksView.setLoadingIndicator(true)
    }
    if (forceUpdate) {
      mTasksRepository.refreshTasks()
    }

    // The network request might be handled in a different thread so make sure Espresso knows
    // that the app is busy until the response is handled.
    EspressoIdlingResource.increment() // App is busy until further notice

    mSubscriptions.clear()
    val subscription = mTasksRepository
        .tasks
        .flatMap { tasks -> Observable.from(tasks) }
        .filter { task ->
          when (filtering) {
            TasksFilterType.ACTIVE_TASKS -> {
              mTasksRepository
                  .tasks
                  .flatMap({ Observable.from(it) })
                  .filter({ it.isActive })

              true
            }

            TasksFilterType.COMPLETED_TASKS -> {
              mTasksRepository
                  .tasks
                  .flatMap({ Observable.from(it); })
                  .filter({ it.isCompleted })
              true
            }
            TasksFilterType.ALL_TASKS -> {
              mTasksRepository
                  .tasks
                  .flatMap({ Observable.from(it); })
                  .filter { true }
              true
            }
          }
        }
        .toList()
        .subscribeOn(mSchedulerProvider.computation())
        .observeOn(mSchedulerProvider.ui())
        .doOnTerminate {
          if (!EspressoIdlingResource.idlingResource.isIdleNow()) {
            EspressoIdlingResource.decrement() // Set app as idle.
          }
        }
        .subscribe(
            // onNext
            { this.processTasks(it) },
            // onError
            { throwable -> mTasksView.showLoadingTasksError() }
            // onCompleted
        ) { mTasksView.setLoadingIndicator(false) }
    mSubscriptions.add(subscription)
  }

  private fun processTasks(tasks: List<Task>) {
    if (tasks.isEmpty()) {
      // Show a message indicating there are no tasks for that filter type.
      processEmptyTasks()
    } else {
      // Show the list of tasks
      mTasksView.showTasks(tasks)
      // Set the filter label's text.
      showFilterLabel()
    }
  }

  private fun showFilterLabel() {
    when (filtering) {
      TasksFilterType.ACTIVE_TASKS -> mTasksView.showActiveFilterLabel()
      TasksFilterType.COMPLETED_TASKS -> mTasksView.showCompletedFilterLabel()
      else -> mTasksView.showAllFilterLabel()
    }
  }

  private fun processEmptyTasks() {
    when (filtering) {
      TasksFilterType.ACTIVE_TASKS -> mTasksView.showNoActiveTasks()
      TasksFilterType.COMPLETED_TASKS -> mTasksView.showNoCompletedTasks()
      else -> mTasksView.showNoTasks()
    }
  }

  override fun addNewTask() {
    mTasksView.showAddTask()
  }

  override fun openTaskDetails(requestedTask: Task) {
    checkNotNull(requestedTask, "requestedTask cannot be null!")
    mTasksView.showTaskDetailsUi(requestedTask.id)
  }

  override fun completeTask(completedTask: Task) {
    checkNotNull(completedTask, "completedTask cannot be null!")
    mTasksRepository.completeTask(completedTask)
    mTasksView.showTaskMarkedComplete()
    loadTasks(false, false)
  }

  override fun activateTask(activeTask: Task) {
    checkNotNull(activeTask, "activeTask cannot be null!")
    mTasksRepository.activateTask(activeTask)
    mTasksView.showTaskMarkedActive()
    loadTasks(false, false)
  }

  override fun clearCompletedTasks() {
    mTasksRepository.clearCompletedTasks()
    mTasksView.showCompletedTasksCleared()
    loadTasks(false, false)
  }

}
