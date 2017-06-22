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

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.google.common.base.Preconditions.checkNotNull
import rx.subscriptions.CompositeSubscription

/**
 * Listens to user actions from the UI ([AddEditTaskFragment]), retrieves the data and updates
 * the UI as required.
 */
class AddEditTaskPresenter
/**
 * Creates a presenter for the add/edit view.

 * @param taskId                 ID of the task to edit or null for a new task
 * *
 * @param tasksRepository        a repository of data for tasks
 * *
 * @param addTaskView            the add/edit view
 * *
 * @param shouldLoadDataFromRepo whether data needs to be loaded or not (for config changes)
 */
(private val mTaskId: String?, tasksRepository: TasksDataSource,
 addTaskView: AddEditTaskContract.View, shouldLoadDataFromRepo: Boolean,
 schedulerProvider: BaseSchedulerProvider) : AddEditTaskContract.Presenter {

  private val mTasksRepository: TasksDataSource

  private val mAddTaskView: AddEditTaskContract.View

  private val mSchedulerProvider: BaseSchedulerProvider

  override var isDataMissing: Boolean = false
    private set

  private val mSubscriptions: CompositeSubscription

  init {
    mTasksRepository = checkNotNull(tasksRepository)
    mAddTaskView = checkNotNull(addTaskView)
    isDataMissing = shouldLoadDataFromRepo

    mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null!")

    mSubscriptions = CompositeSubscription()
    mAddTaskView.setPresenter(this)
  }

  override fun subscribe() {
    if (!isNewTask && isDataMissing) {
      populateTask()
    }
  }

  override fun unsubscribe() {
    mSubscriptions.clear()
  }

  override fun saveTask(title: String, description: String) {
    if (isNewTask) {
      createTask(title, description)
    } else {
      updateTask(title, description)
    }
  }

  override fun populateTask() {
    if (isNewTask) {
      throw RuntimeException("populateTask() was called but task is new.")
    }
    mSubscriptions.add(mTasksRepository
        .getTask(mTaskId!!)
        .subscribeOn(mSchedulerProvider.computation())
        .observeOn(mSchedulerProvider.ui())
        .subscribe(
            // onNext
            { task ->
              if (mAddTaskView.isActive) {
                mAddTaskView.setTitle(task.title!!)
                mAddTaskView.setDescription(task.description!!)

                isDataMissing = false
              }
            } // onError
        ) { e ->
          if (mAddTaskView.isActive) {
            mAddTaskView.showEmptyTaskError()
          }
        })
  }

  private val isNewTask: Boolean
    get() = mTaskId == null

  private fun createTask(title: String, description: String) {
    val newTask = Task(title, description)
    if (newTask.isEmpty) {
      mAddTaskView.showEmptyTaskError()
    } else {
      mTasksRepository.saveTask(newTask)
      mAddTaskView.showTasksList()
    }
  }

  private fun updateTask(title: String, description: String) {
    if (isNewTask) {
      throw RuntimeException("updateTask() was called but task is new.")
    }
    mTasksRepository.saveTask(Task(title, description, mTaskId!!))
    mAddTaskView.showTasksList() // After an edit, go back to the list.
  }
}
