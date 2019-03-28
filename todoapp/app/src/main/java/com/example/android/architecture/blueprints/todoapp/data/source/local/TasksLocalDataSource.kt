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
package com.example.android.architecture.blueprints.todoapp.data.source.local

import androidx.annotation.VisibleForTesting
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.LocalDataNotFoundException
import com.example.android.architecture.blueprints.todoapp.data.source.Result
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.util.AppExecutors
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of a data source as a db.
 */
class TasksLocalDataSource private constructor(
        val appExecutors: AppExecutors,
        val tasksDao: TasksDao
) : TasksDataSource {

    override suspend fun getTasks(): Result<List<Task>> = withContext(appExecutors.ioContext) {
        val tasks = tasksDao.getTasks()
        if (tasks.isNotEmpty()) {
            Result.Success(tasksDao.getTasks())
        } else {
            Result.Error(LocalDataNotFoundException())
        }
    }

    override suspend fun getTask(taskId: String): Result<Task> = withContext(appExecutors.ioContext) {
        val task = tasksDao.getTaskById(taskId)
        if (task != null) Result.Success(task) else Result.Error(LocalDataNotFoundException())
    }

    override suspend fun saveTask(task: Task) = withContext(appExecutors.ioContext) {
        tasksDao.insertTask(task)
    }

    override suspend fun completeTask(task: Task) = withContext(appExecutors.ioContext) {
        tasksDao.updateCompleted(task.id, true)
    }

    override suspend fun completeTask(taskId: String) {
        // Not required for the local data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    override suspend fun activateTask(task: Task) = withContext(appExecutors.ioContext) {
        tasksDao.updateCompleted(task.id, false)
    }

    override suspend fun activateTask(taskId: String) {
        // Not required for the local data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    override suspend fun clearCompletedTasks() {
        withContext(appExecutors.ioContext) {
            tasksDao.deleteCompletedTasks()
        }
    }

    override suspend fun refreshTasks() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    override suspend fun deleteAllTasks() = withContext(appExecutors.ioContext) {
        tasksDao.deleteTasks()
    }

    override suspend fun deleteTask(taskId: String) {
        withContext(appExecutors.ioContext) {
            tasksDao.deleteTaskById(taskId)
        }
    }

    companion object {
        private var INSTANCE: TasksLocalDataSource? = null

        @JvmStatic
        fun getInstance(appExecutors: AppExecutors, tasksDao: TasksDao): TasksLocalDataSource {
            if (INSTANCE == null) {
                synchronized(TasksLocalDataSource::javaClass) {
                    INSTANCE = TasksLocalDataSource(appExecutors, tasksDao)
                }
            }
            return INSTANCE!!
        }

        @VisibleForTesting
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
