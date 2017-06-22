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

package com.example.android.architecture.blueprints.todoapp.data.source.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksPersistenceContract.TaskEntry
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.google.common.base.Preconditions.checkNotNull
import com.squareup.sqlbrite.BriteDatabase
import com.squareup.sqlbrite.SqlBrite
import rx.Observable
import rx.functions.Func1


/**
 * Concrete implementation of a data source as a db.
 */
class TasksLocalDataSource// Prevent direct instantiation.
private constructor(context: Context,
                    schedulerProvider: BaseSchedulerProvider) : TasksDataSource {

  private val mDatabaseHelper: BriteDatabase

  private val mTaskMapperFunction: Func1<Cursor, Task>

  init {
    checkNotNull(context, "context cannot be null")
    checkNotNull(schedulerProvider, "scheduleProvider cannot be null")
    val dbHelper = TasksDbHelper(context)
    val sqlBrite = SqlBrite.create()
    mDatabaseHelper = sqlBrite.wrapDatabaseHelper(dbHelper, schedulerProvider.io())
    mTaskMapperFunction = Func1<Cursor, Task> { this.getTask(it) }
  }

  private fun getTask(c: Cursor): Task {
    val itemId = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_ENTRY_ID))
    val title = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_TITLE))
    val description = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_DESCRIPTION))
    val completed = c.getInt(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_COMPLETED)) == 1
    return Task(title, description, itemId, completed)
  }

  override val tasks: Observable<List<Task>>
    get() {
      val projection = arrayOf(TaskEntry.COLUMN_NAME_ENTRY_ID, TaskEntry.COLUMN_NAME_TITLE, TaskEntry.COLUMN_NAME_DESCRIPTION, TaskEntry.COLUMN_NAME_COMPLETED)
      val sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), TaskEntry.TABLE_NAME)
      return mDatabaseHelper.createQuery(TaskEntry.TABLE_NAME, sql)
          .mapToList(mTaskMapperFunction)
    }

  override fun getTask(taskId: String): Observable<Task> {
    val projection = arrayOf(TaskEntry.COLUMN_NAME_ENTRY_ID, TaskEntry.COLUMN_NAME_TITLE, TaskEntry.COLUMN_NAME_DESCRIPTION, TaskEntry.COLUMN_NAME_COMPLETED)
    val sql = String.format("SELECT %s FROM %s WHERE %s LIKE ?",
        TextUtils.join(",", projection), TaskEntry.TABLE_NAME, TaskEntry.COLUMN_NAME_ENTRY_ID)
    return mDatabaseHelper.createQuery(TaskEntry.TABLE_NAME, sql, taskId)
        .mapToOneOrDefault(mTaskMapperFunction, null)
  }

  override fun saveTask(task: Task) {
    checkNotNull(task)
    val values = ContentValues()
    values.put(TaskEntry.COLUMN_NAME_ENTRY_ID, task.id)
    values.put(TaskEntry.COLUMN_NAME_TITLE, task.title)
    values.put(TaskEntry.COLUMN_NAME_DESCRIPTION, task.description)
    values.put(TaskEntry.COLUMN_NAME_COMPLETED, task.isCompleted)
    mDatabaseHelper.insert(TaskEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE)
  }

  override fun completeTask(task: Task) {
    completeTask(task.id)
  }

  override fun completeTask(taskId: String) {
    val values = ContentValues()
    values.put(TaskEntry.COLUMN_NAME_COMPLETED, true)

    val selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?"
    val selectionArgs = arrayOf(taskId)
    mDatabaseHelper.update(TaskEntry.TABLE_NAME, values, selection, *selectionArgs)
  }

  override fun activateTask(task: Task) {
    activateTask(task.id)
  }

  override fun activateTask(taskId: String) {
    val values = ContentValues()
    values.put(TaskEntry.COLUMN_NAME_COMPLETED, false)

    val selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?"
    val selectionArgs = arrayOf(taskId)
    mDatabaseHelper.update(TaskEntry.TABLE_NAME, values, selection, *selectionArgs)
  }

  override fun clearCompletedTasks() {
    val selection = TaskEntry.COLUMN_NAME_COMPLETED + " LIKE ?"
    val selectionArgs = arrayOf("1")
    mDatabaseHelper.delete(TaskEntry.TABLE_NAME, selection, *selectionArgs)
  }

  override fun refreshTasks() {
    // Not required because the {@link TasksRepository} handles the logic of refreshing the
    // tasks from all the available data sources.
  }

  override fun deleteAllTasks() {
    mDatabaseHelper.delete(TaskEntry.TABLE_NAME, null)
  }

  override fun deleteTask(taskId: String) {
    val selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?"
    val selectionArgs = arrayOf(taskId)
    mDatabaseHelper.delete(TaskEntry.TABLE_NAME, selection, *selectionArgs)
  }

  companion object {

    private var INSTANCE: TasksLocalDataSource? = null

    fun getInstance(
        context: Context,
        schedulerProvider: BaseSchedulerProvider): TasksLocalDataSource {
      if (INSTANCE == null) {
        INSTANCE = TasksLocalDataSource(context, schedulerProvider)
      }
      return INSTANCE!!
    }

    fun destroyInstance() {
      INSTANCE = null
    }
  }
}
