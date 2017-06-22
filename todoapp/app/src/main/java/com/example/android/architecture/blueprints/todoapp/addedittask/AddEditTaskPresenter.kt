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
import rx.subscriptions.CompositeSubscription

/**
 * Listens to user actions from the UI ([AddEditTaskFragment]), retrieves the data and updates
 * the UI as required.
 *
 * Creates a presenter for the add/edit view.

 * @param taskId                 ID of the task to edit or null for a new task
 * *
 * @param tasksRepository        a repository of data for tasks
 * *
 * @param addTaskView            the add/edit view
 * *
 * @param shouldLoadDataFromRepo whether data needs to be loaded or not (for config changes)
 */
class AddEditTaskPresenter(private val mTaskId: String?,
                           val tasksRepository: TasksDataSource,
                           val addTaskView: AddEditTaskContract.View, shouldLoadDataFromRepo: Boolean,
                           val schedulerProvider: BaseSchedulerProvider) : AddEditTaskContract.Presenter {


  override var isDataMissing: Boolean = false

  private val mSubscriptions: CompositeSubscription = CompositeSubscription()

  init {
    isDataMissing = shouldLoadDataFromRepo
    this.addTaskView.setPresenter(this)
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
    mSubscriptions.add(tasksRepository
        .getTask(mTaskId!!)
        .subscribeOn(schedulerProvider.computation())
        .observeOn(schedulerProvider.ui())
        .subscribe(
            // onNext
            { (title, description) ->
              if (addTaskView.isActive) {
                addTaskView.setTitle(title!!)
                addTaskView.setDescription(description!!)

                isDataMissing = false
              }
            },
            // onError
            { _ ->
              if (addTaskView.isActive) {
                addTaskView.showEmptyTaskError()
              }
            }
        ))
  }

  private val isNewTask: Boolean
    get() = mTaskId == null

  private fun createTask(title: String, description: String) {
    val newTask = Task(title, description)
    if (newTask.isEmpty) {
      addTaskView.showEmptyTaskError()
    } else {
      tasksRepository.saveTask(newTask)
      addTaskView.showTasksList()
    }
  }

  private fun updateTask(title: String, description: String) {
    if (isNewTask) {
      throw RuntimeException("updateTask() was called but task is new.")
    }
    tasksRepository.saveTask(Task(title, description, mTaskId!!))
    addTaskView.showTasksList() // After an edit, go back to the list.
  }
}
