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
package com.example.android.architecture.blueprints.todoapp.data.source

import com.example.android.architecture.blueprints.todoapp.anyMockito
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.eq
import com.example.android.architecture.blueprints.todoapp.util.runBlockingSilent
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for the implementation of the in-memory repository with cache.
 */
class TasksRepositoryTest {

    private val TASK_TITLE_1 = "title1"
    private val TASK_TITLE_2 = "title2"
    private val TASK_TITLE_3 = "title3"
    private val TASK_GENERIC_DESCRIPTION = "Some task description"
    private val TASKS: List<Task> = listOf(Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION),
            Task(TASK_TITLE_2, TASK_GENERIC_DESCRIPTION))
    private lateinit var tasksRepository: TasksRepository

    @Mock private lateinit var tasksRemoteDataSource: TasksDataSource
    @Mock private lateinit var tasksLocalDataSource: TasksDataSource

    @Before
    fun setupTasksRepository() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this)

        // Get a reference to the class under test
        tasksRepository = TasksRepository.getInstance(tasksRemoteDataSource, tasksLocalDataSource)
    }

    @After
    fun destroyRepositoryInstance() {
        TasksRepository.destroyInstance()
    }

    @Test
    fun getTasks_repositoryCachesAfterFirstApiCall() = runBlockingSilent {
        // When two calls are issued to the tasks repository
        twoTasksLoadCallsToRepository()

        // Then tasks were only requested once from Service API
        verify(tasksRemoteDataSource).getTasks()
    }

    @Test
    fun getTasks_requestsAllTasksFromLocalDataSource() = runBlockingSilent {
        setTasksAvailable(tasksLocalDataSource, TASKS)

        // When tasks are requested from the tasks repository
        tasksRepository.getTasks()

        // Then tasks are loaded from the local data source
        verify(tasksLocalDataSource).getTasks()
    }

    @Test
    fun saveTask_savesTaskToServiceAPI() = runBlockingSilent {
        // Given a stub task with title and description
        val newTask = Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION)

        // When a task is saved to the tasks repository
        tasksRepository.saveTask(newTask)

        // Then the service API and persistent repository are called and the cache is updated
        verify(tasksRemoteDataSource).saveTask(newTask)
        verify(tasksLocalDataSource).saveTask(newTask)
        assertThat(tasksRepository.cachedTasks.size, `is`(1))
    }

    @Test
    fun completeTask_completesTaskToServiceAPIUpdatesCache() = runBlockingSilent {
        with(tasksRepository) {
            // Given a stub active task with title and description added in the repository
            val newTask = Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION)
            saveTask(newTask)

            // When a task is completed to the tasks repository
            completeTask(newTask)

            // Then the service API and persistent repository are called and the cache is updated
            verify(tasksRemoteDataSource).completeTask(newTask)
            verify(tasksLocalDataSource).completeTask(newTask)
            assertThat(cachedTasks.size, `is`(1))
            val cachedNewTask = cachedTasks[newTask.id]
            assertNotNull(cachedNewTask as Task)
            assertThat(cachedNewTask.isActive, `is`(false))
        }
    }

    @Test
    fun completeTaskId_completesTaskToServiceAPIUpdatesCache() = runBlockingSilent {
        // Given a stub active task with title and description added in the repository
        val newTask = Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION)
        with(tasksRepository) {
            saveTask(newTask)

            // When a task is completed using its id to the tasks repository
            completeTask(newTask.id)

            // Then the service API and persistent repository are called and the cache is updated
            verify(tasksRemoteDataSource).completeTask(newTask)
            verify(tasksLocalDataSource).completeTask(newTask)
            assertThat(cachedTasks.size, `is`(1))
            val cachedNewTask = cachedTasks[newTask.id]
            assertNotNull(cachedNewTask as Task)
            assertThat(cachedNewTask.isActive, `is`(false))
        }
    }

    @Test
    fun activateTask_activatesTaskToServiceAPIUpdatesCache() = runBlockingSilent {
        // Given a stub completed task with title and description in the repository
        val newTask = Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION).apply { isCompleted = true }
        with(tasksRepository) {
            tasksRepository.saveTask(newTask)
            // When a completed task is activated to the tasks repository
            tasksRepository.activateTask(newTask)
            // Then the service API and persistent repository are called and the cache is updated
            verify(tasksRemoteDataSource).activateTask(newTask)
            verify(tasksLocalDataSource).activateTask(newTask)
            assertThat(cachedTasks.size, `is`(1))
            val cachedNewTask = cachedTasks[newTask.id]
            assertNotNull(cachedNewTask as Task)
            assertThat(cachedNewTask.isActive, `is`(true))
        }
    }

    @Test
    fun activateTaskId_activatesTaskToServiceAPIUpdatesCache() = runBlockingSilent {
        // Given a stub completed task with title and description in the repository
        val newTask = Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION).apply { isCompleted = true }
        with(tasksRepository) {
            saveTask(newTask)

            // When a completed task is activated with its id to the tasks repository
            activateTask(newTask.id)

            // Then the service API and persistent repository are called and the cache is updated
            verify(tasksRemoteDataSource).activateTask(newTask)
            verify(tasksLocalDataSource).activateTask(newTask)
            assertThat(cachedTasks.size, `is`(1))
            val cachedNewTask = cachedTasks[newTask.id]
            assertNotNull(cachedNewTask as Task)
            assertThat(cachedNewTask.isActive, `is`(true))
        }
    }

    @Test
    fun getTask_requestsSingleTaskFromLocalDataSource() = runBlockingSilent {
        `when`(tasksLocalDataSource.getTask(TASK_TITLE_1)).thenReturn(Result.Success(Task()))

        // When a task is requested from the tasks repository
        tasksRepository.getTask(TASK_TITLE_1)

        // Then the task is loaded from the database
        verify(tasksLocalDataSource).getTask(eq(TASK_TITLE_1))
    }

    @Test
    fun deleteCompletedTasks_deleteCompletedTasksToServiceAPIUpdatesCache() = runBlockingSilent {
        with(tasksRepository) {
            // Given 2 stub completed tasks and 1 stub active tasks in the repository
            val newTask = Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION).apply { isCompleted = true }
            saveTask(newTask)
            val newTask2 = Task(TASK_TITLE_2, TASK_GENERIC_DESCRIPTION)
            saveTask(newTask2)
            val newTask3 = Task(TASK_TITLE_3, TASK_GENERIC_DESCRIPTION).apply { isCompleted = true }
            saveTask(newTask3)

            // When a completed tasks are cleared to the tasks repository
            clearCompletedTasks()


            // Then the service API and persistent repository are called and the cache is updated
            verify(tasksRemoteDataSource).clearCompletedTasks()
            verify(tasksLocalDataSource).clearCompletedTasks()

            assertThat(cachedTasks.size, `is`(1))
            val task = cachedTasks[newTask2.id]
            assertNotNull(task as Task)
            assertTrue(task.isActive)
            assertThat(task.title, `is`(TASK_TITLE_2))
        }
    }

    @Test
    fun deleteAllTasks_deleteTasksToServiceAPIUpdatesCache() = runBlockingSilent {
        with(tasksRepository) {
            // Given 2 stub completed tasks and 1 stub active tasks in the repository
            val newTask = Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION).apply { isCompleted = true }
            saveTask(newTask)
            val newTask2 = Task(TASK_TITLE_2, TASK_GENERIC_DESCRIPTION)
            saveTask(newTask2)
            val newTask3 = Task(TASK_TITLE_3, TASK_GENERIC_DESCRIPTION).apply { isCompleted = true }
            saveTask(newTask3)

            // When all tasks are deleted to the tasks repository
            deleteAllTasks()

            // Verify the data sources were called
            verify(tasksRemoteDataSource).deleteAllTasks()
            verify(tasksLocalDataSource).deleteAllTasks()

            assertThat(cachedTasks.size, `is`(0))
        }
    }

    @Test
    fun deleteTask_deleteTaskToServiceAPIRemovedFromCache() = runBlockingSilent {
        with(tasksRepository) {
            // Given a task in the repository
            val newTask = Task(TASK_TITLE_1, TASK_GENERIC_DESCRIPTION).apply { isCompleted }
            saveTask(newTask)
            assertThat(cachedTasks.containsKey(newTask.id), `is`(true))

            // When deleted
            deleteTask(newTask.id)

            // Verify the data sources were called
            verify(tasksRemoteDataSource).deleteTask(newTask.id)
            verify(tasksLocalDataSource).deleteTask(newTask.id)

            // Verify it's removed from repository
            assertThat(cachedTasks.containsKey(newTask.id), `is`(false))
        }
    }

    @Test
    fun getTasksWithDirtyCache_tasksAreRetrievedFromRemote() = runBlockingSilent {
        // And the remote data source has data available
        setTasksAvailable(tasksRemoteDataSource, TASKS)

        with(tasksRepository) {
            // When calling getTasks in the repository with dirty cache
            refreshTasks()
            getTasks()
        }

        // Verify the tasks from the remote data source are returned, not the local
        verify(tasksLocalDataSource, never()).getTasks()
        verify(tasksRemoteDataSource).getTasks()
        val result = tasksRepository.getTasks()
        assertThat(result, instanceOf(Result.Success::class.java))
        if (result is Result.Success) {
            assertThat(result.data, `is`(TASKS))
        }
    }

    @Test
    fun getTasksWithLocalDataSourceUnavailable_tasksAreRetrievedFromRemote() = runBlockingSilent {
        // And the local data source has no data available
        setTasksNotAvailable(tasksLocalDataSource)

        // And the remote data source has data available
        setTasksAvailable(tasksRemoteDataSource, TASKS)

        // When calling getTasks in the repository
        tasksRepository.getTasks()

        // Verify the tasks from the local data source are returned
        verify(tasksRemoteDataSource).getTasks()
        val result = tasksRepository.getTasks()
        assertThat(result, instanceOf(Result.Success::class.java))
        if (result is Result.Success) {
            assertThat(result.data, `is`(TASKS))
        }
    }

    @Test
    fun getTasksWithBothDataSourcesUnavailable_firesOnDataUnavailable() = runBlockingSilent {
        // And the local data source has no data available
        setTasksNotAvailable(tasksLocalDataSource)

        // And the remote data source has no data available
        setTasksNotAvailable(tasksRemoteDataSource)

        // When calling getTasks in the repository
        val result = tasksRepository.getTasks()

        // Verify no data is returned
        assertThat(result, instanceOf(Result.Error::class.java))
    }

    @Test
    fun getTaskWithBothDataSourcesUnavailable_firesOnDataUnavailable() = runBlockingSilent {
        // Given a task id
        val taskId = "123"

        // And the local data source has no data available
        setTaskNotAvailable(tasksLocalDataSource, taskId)

        // And the remote data source has no data available
        setTaskNotAvailable(tasksRemoteDataSource, taskId)

        // When calling getTask in the repository
        val result = tasksRepository.getTask(taskId)

        // Verify no data is returned
        assertThat(result, instanceOf(Result.Error::class.java))
    }

    @Test
    fun getTasks_refreshesLocalDataSource() = runBlockingSilent {
        // Make the remote data source return data
        setTasksAvailable(tasksRemoteDataSource, TASKS)

        // Mark cache as dirty to force a reload of data from remote data source.
        tasksRepository.refreshTasks()

        // When calling getTasks in the repository
        tasksRepository.getTasks()

        // Verify that the data fetched from the remote data source was saved in local.
        verify(tasksLocalDataSource, times(TASKS.size)).saveTask(anyMockito())
    }

    /**
     * Convenience method that issues two calls to the tasks repository
     */
    private fun twoTasksLoadCallsToRepository() = runBlockingSilent {
        // Local data source doesn't have data yet
        `when`(tasksLocalDataSource.getTasks()).thenReturn(Result.Error(DataSourceException()))

        // Trigger callback so tasks are cached
        `when`(tasksRemoteDataSource.getTasks()).thenReturn(Result.Success(TASKS))

        // When tasks are requested from repository
        tasksRepository.getTasks() // First call to API

        // Use the Mockito Captor to capture the callback
        verify(tasksLocalDataSource).getTasks()

        // Verify the remote data source is queried
        verify(tasksRemoteDataSource).getTasks()

        tasksRepository.getTasks() // Second call to API
    }

    private fun setTasksNotAvailable(dataSource: TasksDataSource) = runBlockingSilent {
        `when`(dataSource.getTasks()).thenReturn(Result.Error(DataSourceException()))
    }

    private fun setTasksAvailable(dataSource: TasksDataSource, tasks: List<Task>) = runBlockingSilent {
        `when`(dataSource.getTasks()).thenReturn(Result.Success(tasks))
    }

    private fun setTaskNotAvailable(dataSource: TasksDataSource, taskId: String) = runBlockingSilent {
        `when`(dataSource.getTask(taskId)).thenReturn(Result.Error(DataSourceException()))
    }

}
