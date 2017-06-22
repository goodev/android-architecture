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

package com.example.android.architecture.blueprints.todoapp.data

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.test.suitebuilder.annotation.LargeTest

import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksDbHelper
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksLocalDataSource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import rx.observers.TestSubscriber

import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsCollectionContaining.hasItems
import org.hamcrest.core.IsNot.not
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat

/**
 * Integration test for the [TasksDataSource], which uses the [TasksDbHelper].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TasksLocalDataSourceTest {

  private lateinit var mSchedulerProvider: BaseSchedulerProvider

  private lateinit var mLocalDataSource: TasksLocalDataSource

  @Before
  fun setup() {
    TasksLocalDataSource.destroyInstance()
    mSchedulerProvider = ImmediateSchedulerProvider

    mLocalDataSource = TasksLocalDataSource.getInstance(
        InstrumentationRegistry.getTargetContext(), mSchedulerProvider)
  }

  @After
  fun cleanUp() {
    mLocalDataSource.deleteAllTasks()
  }

  @Test
  fun testPreConditions() {
    assertNotNull(mLocalDataSource)
  }

  @Test
  fun saveTask_retrievesTask() {
    // Given a new task
    val newTask = Task(TITLE, "")

    // When saved into the persistent repository
    mLocalDataSource.saveTask(newTask)

    // Then the task can be retrieved from the persistent repository
    val testSubscriber = TestSubscriber<Task>()
    mLocalDataSource.getTask(newTask.id).subscribe(testSubscriber)
    testSubscriber.assertValue(newTask)
  }

  @Test
  fun completeTask_retrievedTaskIsComplete() {
    // Given a new task in the persistent repository
    val newTask = Task(TITLE, "")
    mLocalDataSource.saveTask(newTask)

    // When completed in the persistent repository
    mLocalDataSource.completeTask(newTask)

    // Then the task can be retrieved from the persistent repository and is complete
    val testSubscriber = TestSubscriber<Task>()
    mLocalDataSource.getTask(newTask.id).subscribe(testSubscriber)
    testSubscriber.assertValueCount(1)
    val result = testSubscriber.onNextEvents[0]
    assertThat(result.isCompleted, `is`(true))
  }

  @Test
  fun activateTask_retrievedTaskIsActive() {
    // Given a new completed task in the persistent repository
    val newTask = Task(TITLE, "")
    mLocalDataSource.saveTask(newTask)
    mLocalDataSource.completeTask(newTask)

    // When activated in the persistent repository
    mLocalDataSource.activateTask(newTask)

    // Then the task can be retrieved from the persistent repository and is active
    val testSubscriber = TestSubscriber<Task>()
    mLocalDataSource.getTask(newTask.id).subscribe(testSubscriber)
    testSubscriber.assertValueCount(1)
    val result = testSubscriber.onNextEvents[0]
    assertThat(result.isActive, `is`(true))
    assertThat(result.isCompleted, `is`(false))
  }

  @Test
  fun clearCompletedTask_taskNotRetrievable() {
    // Given 2 new completed tasks and 1 active task in the persistent repository
    val newTask1 = Task(TITLE, "")
    mLocalDataSource.saveTask(newTask1)
    mLocalDataSource.completeTask(newTask1)
    val newTask2 = Task(TITLE2, "")
    mLocalDataSource.saveTask(newTask2)
    mLocalDataSource.completeTask(newTask2)
    val newTask3 = Task(TITLE3, "")
    mLocalDataSource.saveTask(newTask3)

    // When completed tasks are cleared in the repository
    mLocalDataSource.clearCompletedTasks()

    // Then the completed tasks cannot be retrieved and the active one can
    val testSubscriber = TestSubscriber<List<Task>>()
    mLocalDataSource.tasks.subscribe(testSubscriber)
    val result = testSubscriber.onNextEvents[0]
    assertThat(result, not(hasItems(newTask1, newTask2)))
  }

  @Test
  fun deleteAllTasks_emptyListOfRetrievedTask() {
    // Given a new task in the persistent repository and a mocked callback
    val newTask = Task(TITLE, "")
    mLocalDataSource.saveTask(newTask)

    // When all tasks are deleted
    mLocalDataSource.deleteAllTasks()

    // Then the retrieved tasks is an empty list
    val testSubscriber = TestSubscriber<List<Task>>()
    mLocalDataSource.tasks.subscribe(testSubscriber)
    val result = testSubscriber.onNextEvents[0]
    assertThat(result.isEmpty(), `is`(true))
  }

  @Test
  fun getTasks_retrieveSavedTasks() {
    // Given 2 new tasks in the persistent repository
    val newTask1 = Task(TITLE, "")
    mLocalDataSource.saveTask(newTask1)
    val newTask2 = Task(TITLE, "")
    mLocalDataSource.saveTask(newTask2)

    // Then the tasks can be retrieved from the persistent repository
    val testSubscriber = TestSubscriber<List<Task>>()
    mLocalDataSource.tasks.subscribe(testSubscriber)
    val result = testSubscriber.onNextEvents[0]
    assertThat(result, hasItems(newTask1, newTask2))
  }

  @Test
  fun getTask_whenTaskNotSaved() {
    //Given that no task has been saved
    //When querying for a task, null is returned.
    val testSubscriber = TestSubscriber<Task>()
    mLocalDataSource.getTask("1").subscribe(testSubscriber)
    testSubscriber.assertValue(null)
  }

  companion object {

    private val TITLE = "title"

    private val TITLE2 = "title2"

    private val TITLE3 = "title3"
  }
}
