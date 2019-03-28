/*
 * Copyright 2017, The Android Open Source Project
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
import com.example.android.architecture.blueprints.todoapp.data.source.Result
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Listens to user actions from the UI ([TaskDetailFragment]), retrieves the data and updates
 * the UI as required.
 */
class TaskDetailPresenter(
        private val taskId: String,
        private val tasksRepository: TasksRepository,
        private val taskDetailView: TaskDetailContract.View,
        private val coroutineScope: CoroutineScope
) : TaskDetailContract.Presenter {

    init {
        taskDetailView.presenter = this
    }

    override fun start() {
        openTask()
    }

    private fun openTask() = coroutineScope.launch {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
        } else {
            taskDetailView.setLoadingIndicator(true)
            val result = tasksRepository.getTask(taskId)
            if (isActive) { // The view may not be able to handle UI updates anymore
                if (result is Result.Success) {
                    taskDetailView.setLoadingIndicator(false)
                    showTask(result.data)
                } else {
                    taskDetailView.showMissingTask()
                }
            }
        }
    }

    override fun editTask() {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
            return
        }
        taskDetailView.showEditTask(taskId)
    }

    override fun deleteTask() = coroutineScope.launch {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
        } else {
            tasksRepository.deleteTask(taskId)
            taskDetailView.showTaskDeleted()
        }
    }

    override fun completeTask() = coroutineScope.launch {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
        } else {
            tasksRepository.completeTask(taskId)
            taskDetailView.showTaskMarkedComplete()
        }
    }

    override fun activateTask() = coroutineScope.launch {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
        } else {
            tasksRepository.activateTask(taskId)
            taskDetailView.showTaskMarkedActive()
        }
    }

    private fun showTask(task: Task) {
        with(taskDetailView) {
            if (taskId.isEmpty()) {
                hideTitle()
                hideDescription()
            } else {
                showTitle(task.title)
                showDescription(task.description)
            }
            showCompletionStatus(task.isCompleted)
        }
    }
}
