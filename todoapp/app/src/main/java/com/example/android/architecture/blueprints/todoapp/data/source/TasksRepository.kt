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

package com.example.android.architecture.blueprints.todoapp.data.source

import android.support.annotation.VisibleForTesting
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.google.common.base.Preconditions.checkNotNull
import rx.Observable
import java.util.*

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
open class TasksRepository(private val tasksRemoteDataSource: TasksDataSource,
                           private val tasksLocalDataSource: TasksDataSource) : TasksDataSource {

  /**
   * This variable has package local visibility so it can be accessed from tests.
   */
  @VisibleForTesting
  var mCachedTasks: MutableMap<String, Task>? = null

  /**
   * Marks the cache as invalid, to force an update the next time data is requested. This variable
   * has package local visibility so it can be accessed from tests.
   */
  @VisibleForTesting
  internal var mCacheIsDirty = false

  /**
   * Gets tasks from cache, local data source (SQLite) or remote data source, whichever is
   * available first.
   */
  override // Respond immediately with cache if available and not dirty
      // Query the local storage if available. If not, query the network.
  val tasks: Observable<List<Task>>
    get() {
      if (mCachedTasks != null && !mCacheIsDirty) {
        return Observable.from(mCachedTasks?.values).toList()
      } else if (mCachedTasks == null) {
        mCachedTasks = mutableMapOf()
      }


      val remoteTasks = andSaveRemoteTasks

      if (mCacheIsDirty) {
        return remoteTasks
      } else {
        val localTasks = andCacheLocalTasks
        return Observable.concat(localTasks, remoteTasks)
            .filter { tasks -> !tasks.isEmpty() }
            .first()
      }
    }

  private val andCacheLocalTasks: Observable<List<Task>>
    get() = tasksLocalDataSource.tasks
        .flatMap { tasks ->
          Observable.from(tasks)
              .doOnNext { task -> mCachedTasks?.put(task.id, task) }
              .toList()
        }

  private val andSaveRemoteTasks: Observable<List<Task>>
    get() = tasksRemoteDataSource
        .tasks
        .flatMap { tasks ->
          Observable.from(tasks).doOnNext { task ->
            tasksLocalDataSource.saveTask(task)
            mCachedTasks?.put(task.id, task)
          }.toList()
        }
        .doOnCompleted { mCacheIsDirty = false }

  override fun saveTask(task: Task) {
    tasksRemoteDataSource.saveTask(task)
    tasksLocalDataSource.saveTask(task)

    if (mCachedTasks == null) {
      mCachedTasks = mutableMapOf()
    }

    mCachedTasks?.put(task.id, task)
  }

  override fun completeTask(task: Task) {
    tasksRemoteDataSource.completeTask(task)
    tasksLocalDataSource.completeTask(task)

    val completedTask = Task(task.title, task.description, task.id, true)

    if (mCachedTasks == null) {
      mCachedTasks = mutableMapOf()
    }

    mCachedTasks?.put(task.id, completedTask)
  }

  override fun completeTask(taskId: String) {
    checkNotNull(taskId)
    val taskWithId = getTaskWithId(taskId)
    if (taskWithId != null) {
      completeTask(taskWithId)
    }
  }

  override fun activateTask(task: Task) {
    tasksRemoteDataSource.activateTask(task)
    tasksLocalDataSource.activateTask(task)

    val activeTask = Task(task.title, task.description, task.id)

    if (mCachedTasks == null) {
      mCachedTasks = mutableMapOf()
    }

    mCachedTasks?.put(task.id, activeTask)
  }

  override fun activateTask(taskId: String) {
    checkNotNull(taskId)
    val taskWithId = getTaskWithId(taskId)
    if (taskWithId != null) {
      activateTask(taskWithId)
    }
  }

  override fun clearCompletedTasks() {
    tasksRemoteDataSource.clearCompletedTasks()
    tasksLocalDataSource.clearCompletedTasks()

    if (mCachedTasks == null) {
      mCachedTasks = mutableMapOf()
    }

    val it = mCachedTasks?.entries?.iterator()!!
    while (it.hasNext()) {
      val entry = it.next()
      if (entry.value.isCompleted) {
        it.remove()
      }
    }
  }

  /**
   * Gets tasks from local data source (sqlite) unless the table is new or empty. In that case it
   * uses the network data source. This is done to simplify the sample.
   */
  override fun getTask(taskId: String): Observable<Task> {
    val cachedTask = getTaskWithId(taskId)

    // Respond immediately with cache if available
    if (cachedTask != null) {
      return Observable.just(cachedTask)
    }

    // Load from server/persisted if needed.
    if (mCachedTasks == null) {
      mCachedTasks = mutableMapOf()
    }

    // Is the task in the local data source? If not, query the network.
    val localTask = getTaskWithIdFromLocalRepository(taskId)
    val remoteTask = tasksRemoteDataSource
        .getTask(taskId)
        .doOnNext { task ->
          tasksLocalDataSource.saveTask(task)
          mCachedTasks?.put(task.id, task)
        }

    return Observable.concat(localTask, remoteTask).first()
        .map { task ->
          if (task == null) {
            throw NoSuchElementException("No task found with taskId " + taskId)
          }
          task
        }
  }

  override fun refreshTasks() {
    mCacheIsDirty = true
  }

  override fun deleteAllTasks() {
    tasksRemoteDataSource.deleteAllTasks()
    tasksLocalDataSource.deleteAllTasks()

    if (mCachedTasks == null) {
      mCachedTasks = mutableMapOf()
    }

    mCachedTasks?.clear()
  }

  override fun deleteTask(taskId: String) {
    tasksRemoteDataSource.deleteTask(checkNotNull(taskId))
    tasksLocalDataSource.deleteTask(checkNotNull(taskId))

    mCachedTasks?.remove(taskId)
  }

  private fun getTaskWithId(id: String): Task? {
    checkNotNull(id)
    if (mCachedTasks ==  null || mCachedTasks?.isEmpty()!!) {
      return null
    } else {
      return mCachedTasks!![id]
    }
  }

  internal fun getTaskWithIdFromLocalRepository(taskId: String): Observable<Task> {
    return tasksLocalDataSource
        .getTask(taskId)
        .doOnNext { task -> mCachedTasks?.put(taskId, task) }
        .first()
  }

  companion object {

    var INSTANCE: TasksRepository? = null

    /**
     * Returns the single instance of this class, creating it if necessary.

     * @param tasksRemoteDataSource the backend data source
     * *
     * @param tasksLocalDataSource  the device storage data source
     * *
     * @return the [TasksRepository] instance
     */
    fun getInstance(tasksRemoteDataSource: TasksDataSource,
                    tasksLocalDataSource: TasksDataSource): TasksRepository {
      if (INSTANCE == null) {
        INSTANCE = TasksRepository(tasksRemoteDataSource, tasksLocalDataSource)
      }
      return INSTANCE!!
    }

    /**
     * Used to force [.getInstance] to create a new instance
     * next time it's called.
     */
    fun destroyInstance() {
      INSTANCE = null
    }
  }
}
