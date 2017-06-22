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

package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.google.common.base.Strings
import rx.subscriptions.CompositeSubscription

/**
 * Listens to user actions from the UI ([TaskDetailFragment]), retrieves the data and updates
 * the UI as required.
 */
class TaskDetailPresenter(private val mTaskId: String?,
                          val tasksRepository: TasksRepository,
                          val taskDetailView: TaskDetailContract.View,
                          val schedulerProvider: BaseSchedulerProvider) : TaskDetailContract.Presenter {

  private val mSubscriptions: CompositeSubscription = CompositeSubscription()

  init {
    this.taskDetailView.setPresenter(this)
  }

  override fun subscribe() {
    openTask()
  }

  override fun unsubscribe() {
    mSubscriptions.clear()
  }

  private fun openTask() {
    if (Strings.isNullOrEmpty(mTaskId)) {
      taskDetailView.showMissingTask()
      return
    }

    taskDetailView.setLoadingIndicator(true)
    mSubscriptions.add(tasksRepository
        .getTask(mTaskId!!)
        .subscribeOn(schedulerProvider.computation())
        .observeOn(schedulerProvider.ui())
        .subscribe(
            // onNext
            { this.showTask(it) },
            // onError
            { println(it) },
            // onCompleted
            { taskDetailView.setLoadingIndicator(false) }
        ))
  }

  override fun editTask() {
    if (Strings.isNullOrEmpty(mTaskId)) {
      taskDetailView.showMissingTask()
      return
    }
    taskDetailView.showEditTask(mTaskId!!)
  }

  override fun deleteTask() {
    if (Strings.isNullOrEmpty(mTaskId)) {
      taskDetailView.showMissingTask()
      return
    }
    tasksRepository.deleteTask(mTaskId!!)
    taskDetailView.showTaskDeleted()
  }

  override fun completeTask() {
    if (Strings.isNullOrEmpty(mTaskId)) {
      taskDetailView.showMissingTask()
      return
    }
    tasksRepository.completeTask(mTaskId!!)
    taskDetailView.showTaskMarkedComplete()
  }

  override fun activateTask() {
    if (Strings.isNullOrEmpty(mTaskId)) {
      taskDetailView.showMissingTask()
      return
    }
    tasksRepository.activateTask(mTaskId!!)
    taskDetailView.showTaskMarkedActive()
  }

  private fun showTask(task: Task) {
    val title = task.title
    val description = task.description

    if (Strings.isNullOrEmpty(title)) {
      taskDetailView.hideTitle()
    } else {
      taskDetailView.showTitle(title!!)
    }

    if (Strings.isNullOrEmpty(description)) {
      taskDetailView.hideDescription()
    } else {
      taskDetailView.showDescription(description!!)
    }
    taskDetailView.showCompletionStatus(task.isCompleted)
  }
}
