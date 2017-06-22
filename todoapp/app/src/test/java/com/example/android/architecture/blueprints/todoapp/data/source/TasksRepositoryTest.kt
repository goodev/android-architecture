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

import android.content.Context
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.google.common.collect.Lists
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import rx.Observable
import rx.observers.TestSubscriber
import java.util.*

/**
 * Unit tests for the implementation of the in-memory repository with cache.
 */
class TasksRepositoryTest {

  private lateinit var mTasksRepository: TasksRepository

  private lateinit var mTasksTestSubscriber: TestSubscriber<List<Task>>

  @Mock
  lateinit var mTasksRemoteDataSource: TasksDataSource

  @Mock
  lateinit var mTasksLocalDataSource: TasksDataSource

  @Mock
  lateinit var mContext: Context


  @Before
  fun setupTasksRepository() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    // Get a reference to the class under test
    mTasksRepository = TasksRepository.getInstance(mTasksRemoteDataSource, mTasksLocalDataSource)

    mTasksTestSubscriber = TestSubscriber<List<Task>>()
  }

  @After
  fun destroyRepositoryInstance() {
    TasksRepository.destroyInstance()
  }

  @Test
  fun getTasks_repositoryCachesAfterFirstSubscription_whenTasksAvailableInLocalStorage() {
    // Given that the local data source has data available
    setTasksAvailable(mTasksLocalDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(mTasksRemoteDataSource)

    // When two subscriptions are set
    val testSubscriber1 = TestSubscriber<List<Task>>()
    mTasksRepository.tasks.subscribe(testSubscriber1)

    val testSubscriber2 = TestSubscriber<List<Task>>()
    mTasksRepository.tasks.subscribe(testSubscriber2)

    // Then tasks were only requested once from remote and local sources
    verify<TasksDataSource>(mTasksRemoteDataSource).tasks
    verify<TasksDataSource>(mTasksLocalDataSource).tasks
    //
    assertFalse(mTasksRepository.mCacheIsDirty)
    testSubscriber1.assertValue(TASKS)
    testSubscriber2.assertValue(TASKS)
  }

  @Test
  fun getTasks_repositoryCachesAfterFirstSubscription_whenTasksAvailableInRemoteStorage() {
    // Given that the local data source has data available
    setTasksAvailable(mTasksLocalDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(mTasksRemoteDataSource)

    // When two subscriptions are set
    val testSubscriber1 = TestSubscriber<List<Task>>()
    mTasksRepository.tasks.subscribe(testSubscriber1)

    val testSubscriber2 = TestSubscriber<List<Task>>()
    mTasksRepository.tasks.subscribe(testSubscriber2)

    // Then tasks were only requested once from remote and local sources
    verify(mTasksRemoteDataSource).tasks
    verify(mTasksLocalDataSource).tasks
    assertFalse(mTasksRepository.mCacheIsDirty)
    testSubscriber1.assertValue(TASKS)
    testSubscriber2.assertValue(TASKS)
  }

  @Test
  fun getTasks_requestsAllTasksFromLocalDataSource() {
    // Given that the local data source has data available
    setTasksAvailable(mTasksLocalDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(mTasksRemoteDataSource)

    // When tasks are requested from the tasks repository
    mTasksRepository.tasks.subscribe(mTasksTestSubscriber)

    // Then tasks are loaded from the local data source
    verify<TasksDataSource>(mTasksLocalDataSource).tasks
    mTasksTestSubscriber.assertValue(TASKS)
  }

  @Test
  fun saveTask_savesTaskToServiceAPI() {
    // Given a stub task with title and description
    val newTask = Task(TASK_TITLE, "Some Task Description")

    // When a task is saved to the tasks repository
    mTasksRepository.saveTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(mTasksRemoteDataSource).saveTask(newTask)
    verify<TasksDataSource>(mTasksLocalDataSource).saveTask(newTask)
    assertThat(mTasksRepository.mCachedTasks?.size, `is`(1))
  }

  @Test
  fun completeTask_completesTaskToServiceAPIUpdatesCache() {
    // Given a stub active task with title and description added in the repository
    val newTask = Task(TASK_TITLE, "Some Task Description")
    mTasksRepository.saveTask(newTask)

    // When a task is completed to the tasks repository
    mTasksRepository.completeTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(mTasksRemoteDataSource).completeTask(newTask)
    verify<TasksDataSource>(mTasksLocalDataSource).completeTask(newTask)
    assertThat(mTasksRepository.mCachedTasks?.size, `is`(1))
    assertThat(mTasksRepository.mCachedTasks?.get(newTask.id)?.isActive, `is`(false))
  }

  @Test
  fun completeTaskId_completesTaskToServiceAPIUpdatesCache() {
    // Given a stub active task with title and description added in the repository
    val newTask = Task(TASK_TITLE, "Some Task Description")
    mTasksRepository.saveTask(newTask)

    // When a task is completed using its id to the tasks repository
    mTasksRepository.completeTask(newTask.id)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(mTasksRemoteDataSource).completeTask(newTask)
    verify<TasksDataSource>(mTasksLocalDataSource).completeTask(newTask)
    assertThat(mTasksRepository.mCachedTasks?.size, `is`(1))
    assertThat(mTasksRepository.mCachedTasks?.get(newTask.id)?.isActive, `is`(false))
  }

  @Test
  fun activateTask_activatesTaskToServiceAPIUpdatesCache() {
    // Given a stub completed task with title and description in the repository
    val newTask = Task(TASK_TITLE, "Some Task Description", isCompleted = true)
    mTasksRepository.saveTask(newTask)

    // When a completed task is activated to the tasks repository
    mTasksRepository.activateTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(mTasksRemoteDataSource).activateTask(newTask)
    verify<TasksDataSource>(mTasksLocalDataSource).activateTask(newTask)
    assertThat(mTasksRepository.mCachedTasks?.size, `is`(1))
    assertThat(mTasksRepository.mCachedTasks?.get(newTask.id)?.isActive, `is`(true))
  }

  @Test
  fun activateTaskId_activatesTaskToServiceAPIUpdatesCache() {
    // Given a stub completed task with title and description in the repository
    val newTask = Task(TASK_TITLE, "Some Task Description", isCompleted = true)
    mTasksRepository.saveTask(newTask)

    // When a completed task is activated with its id to the tasks repository
    mTasksRepository.activateTask(newTask.id)

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(mTasksRemoteDataSource).activateTask(newTask)
    verify<TasksDataSource>(mTasksLocalDataSource).activateTask(newTask)
    assertThat(mTasksRepository.mCachedTasks?.size, `is`(1))
    assertThat(mTasksRepository.mCachedTasks?.get(newTask.id)?.isActive, `is`(true))
  }

  @Test
  fun getTask_requestsSingleTaskFromLocalDataSource() {
    // Given a stub completed task with title and description in the local repository
    val task = Task(TASK_TITLE, "Some Task Description", isCompleted = true)
    setTaskAvailable(mTasksLocalDataSource, task)
    // And the task not available in the remote repository
    setTaskNotAvailable(mTasksRemoteDataSource, task.id)

    // When a task is requested from the tasks repository
    val testSubscriber = TestSubscriber<Task>()
    mTasksRepository.getTask(task.id).subscribe(testSubscriber)

    // Then the task is loaded from the database
    verify<TasksDataSource>(mTasksLocalDataSource).getTask(eq(task.id))
    testSubscriber.assertValue(task)
  }

  @Test
  fun getTask_whenDataNotLocal_fails() {
    // Given a stub completed task with title and description in the remote repository
    val task = Task(TASK_TITLE, "Some Task Description", isCompleted = true)
    setTaskAvailable(mTasksRemoteDataSource, task)
    // And the task not available in the local repository
    setTaskNotAvailable(mTasksLocalDataSource, task.id)

    // When a task is requested from the tasks repository
    val testSubscriber = TestSubscriber<Task>()
    mTasksRepository.getTask(task.id).subscribe(testSubscriber)

    // Verify no data is returned
    testSubscriber.assertNoValues()
    // Verify that error is returned
    testSubscriber.assertError(IllegalStateException::class.java)
  }

  @Test
  fun deleteCompletedTasks_deleteCompletedTasksToServiceAPIUpdatesCache() {
    // Given 2 stub completed tasks and 1 stub active tasks in the repository
    val newTask = Task(TASK_TITLE, "Some Task Description", isCompleted = true)
    mTasksRepository.saveTask(newTask)
    val newTask2 = Task(TASK_TITLE2, "Some Task Description")
    mTasksRepository.saveTask(newTask2)
    val newTask3 = Task(TASK_TITLE3, "Some Task Description", isCompleted = true)
    mTasksRepository.saveTask(newTask3)

    // When a completed tasks are cleared to the tasks repository
    mTasksRepository.clearCompletedTasks()

    // Then the service API and persistent repository are called and the cache is updated
    verify<TasksDataSource>(mTasksRemoteDataSource).clearCompletedTasks()
    verify<TasksDataSource>(mTasksLocalDataSource).clearCompletedTasks()

    assertThat(mTasksRepository.mCachedTasks?.size, `is`(1))
    assertTrue(mTasksRepository.mCachedTasks!![newTask2.id]!!.isActive)
    assertThat(mTasksRepository.mCachedTasks!![newTask2.id]!!.title, `is`(TASK_TITLE2))
  }

  @Test
  fun deleteAllTasks_deleteTasksToServiceAPIUpdatesCache() {
    // Given 2 stub completed tasks and 1 stub active tasks in the repository
    val newTask = Task(TASK_TITLE, "Some Task Description", isCompleted = true)
    mTasksRepository.saveTask(newTask)
    val newTask2 = Task(TASK_TITLE2, "Some Task Description")
    mTasksRepository.saveTask(newTask2)
    val newTask3 = Task(TASK_TITLE3, "Some Task Description", isCompleted = true)
    mTasksRepository.saveTask(newTask3)

    // When all tasks are deleted to the tasks repository
    mTasksRepository.deleteAllTasks()

    // Verify the data sources were called
    verify<TasksDataSource>(mTasksRemoteDataSource).deleteAllTasks()
    verify<TasksDataSource>(mTasksLocalDataSource).deleteAllTasks()

    assertThat(mTasksRepository.mCachedTasks?.size, `is`(0))
  }

  @Test
  fun deleteTask_deleteTaskToServiceAPIRemovedFromCache() {
    // Given a task in the repository
    val newTask = Task(TASK_TITLE, "Some Task Description", isCompleted = true)
    mTasksRepository.saveTask(newTask)
    assertThat(mTasksRepository.mCachedTasks?.containsKey(newTask.id), `is`(true))

    // When deleted
    mTasksRepository.deleteTask(newTask.id)

    // Verify the data sources were called
    verify<TasksDataSource>(mTasksRemoteDataSource).deleteTask(newTask.id)
    verify<TasksDataSource>(mTasksLocalDataSource).deleteTask(newTask.id)

    // Verify it's removed from repository
    assertThat(mTasksRepository.mCachedTasks?.containsKey(newTask.id), `is`(false))
  }

  @Test
  fun getTasksWithDirtyCache_tasksAreRetrievedFromRemote() {
    // Given that the remote data source has data available
    setTasksAvailable(mTasksRemoteDataSource, TASKS)

    // When calling getTasks in the repository with dirty cache
    mTasksRepository.refreshTasks()
    mTasksRepository.tasks.subscribe(mTasksTestSubscriber)

    // Verify the tasks from the remote data source are returned, not the local
    verify<TasksDataSource>(mTasksLocalDataSource, never()).tasks
    verify<TasksDataSource>(mTasksRemoteDataSource).tasks
    mTasksTestSubscriber.assertValue(TASKS)
  }

  @Test
  fun getTasksWithLocalDataSourceUnavailable_tasksAreRetrievedFromRemote() {
    // Given that the local data source has no data available
    setTasksNotAvailable(mTasksLocalDataSource)
    // And the remote data source has data available
    setTasksAvailable(mTasksRemoteDataSource, TASKS)

    // When calling getTasks in the repository
    mTasksRepository.tasks.subscribe(mTasksTestSubscriber)

    // Verify the tasks from the remote data source are returned
    verify<TasksDataSource>(mTasksRemoteDataSource).tasks
    mTasksTestSubscriber.assertValue(TASKS)
  }

  @Test
  fun getTasksWithBothDataSourcesUnavailable_firesOnDataUnavailable() {
    // Given that the local data source has no data available
    setTasksNotAvailable(mTasksLocalDataSource)
    // And the remote data source has no data available
    setTasksNotAvailable(mTasksRemoteDataSource)

    // When calling getTasks in the repository
    mTasksRepository.tasks.subscribe(mTasksTestSubscriber)

    // Verify no data is returned
    mTasksTestSubscriber.assertNoValues()
    // Verify that error is returned
    mTasksTestSubscriber.assertError(NoSuchElementException::class.java)
  }

  @Test
  fun getTaskWithBothDataSourcesUnavailable_firesOnError() {
    // Given a task id
    val taskId = "123"
    // And the local data source has no data available
    setTaskNotAvailable(mTasksLocalDataSource, taskId)
    // And the remote data source has no data available
    setTaskNotAvailable(mTasksRemoteDataSource, taskId)

    // When calling getTask in the repository
    val testSubscriber = TestSubscriber<Task>()
    mTasksRepository.getTask(taskId).subscribe(testSubscriber)

    // Verify that error is returned
    testSubscriber.assertError(IllegalStateException::class.java)
  }

  @Test
  fun getTasks_refreshesLocalDataSource() {
    // Given that the remote data source has data available
    setTasksAvailable(mTasksRemoteDataSource, TASKS)

    // Mark cache as dirty to force a reload of data from remote data source.
    mTasksRepository.refreshTasks()

    // When calling getTasks in the repository
    mTasksRepository.tasks.subscribe(mTasksTestSubscriber)

    // Verify that the data fetched from the remote data source was saved in local.
    verify<TasksDataSource>(mTasksLocalDataSource, times(TASKS.size)).saveTask(any<Task>())
    mTasksTestSubscriber.assertValue(TASKS)
  }

  private fun setTasksNotAvailable(dataSource: TasksDataSource) {
    whenever(dataSource.tasks).thenReturn(Observable.just(emptyList<Task>()))
  }

  private fun setTasksAvailable(dataSource: TasksDataSource, tasks: List<Task>) {
    // don't allow the data sources to complete.
    whenever(dataSource.tasks).thenReturn(Observable.just(tasks).concatWith(Observable.never<List<Task>>()))
  }

  private fun setTaskNotAvailable(dataSource: TasksDataSource, taskId: String) {
    whenever(dataSource.getTask(eq(taskId))).thenReturn(Observable.just<Task>(null).concatWith(Observable.never<Task>()))
  }

  private fun setTaskAvailable(dataSource: TasksDataSource, task: Task) {
    whenever(dataSource.getTask(eq(task.id))).thenReturn(Observable.just(task).concatWith(Observable.never<Task>()))
  }

  companion object {

    private val TASK_TITLE = "title"

    private val TASK_TITLE2 = "title2"

    private val TASK_TITLE3 = "title3"

    private val TASKS = Lists.newArrayList(Task("Title1", "Description1"),
        Task("Title2", "Description2"))
  }
}
