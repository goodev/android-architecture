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
package com.example.android.architecture.blueprints.todoapp.statistics

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.DataSourceException
import com.example.android.architecture.blueprints.todoapp.data.source.Result
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.runBlockingSilent
import kotlinx.coroutines.experimental.Unconfined
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit tests for the implementation of [StatisticsPresenter]
 */
class StatisticsPresenterTest {

    @Mock private lateinit var tasksRepository: TasksRepository
    @Mock private lateinit var statisticsView: StatisticsContract.View
    private lateinit var statisticsPresenter: StatisticsPresenter
    private lateinit var tasks: MutableList<Task>

    @Before
    fun setupStatisticsPresenter() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this)

        // Get a reference to the class under test
        statisticsPresenter = StatisticsPresenter(tasksRepository, statisticsView, coroutineScope = Unconfined)

        // The presenter won't update the view unless it's active.
        `when`(statisticsView.isActive).thenReturn(true)

        // We start the tasks to 3, with one active and two completed
        tasks = mutableListOf(Task("Title1", "Description1"),
                Task("Title2", "Description2").apply { isCompleted = true },
                Task("Title3", "Description3").apply { isCompleted = true })
    }

    @Test
    fun createPresenter_setsThePresenterToView() {
        // Get a reference to the class under test
        statisticsPresenter = StatisticsPresenter(tasksRepository, statisticsView, coroutineScope = Unconfined)

        // Then the presenter is set to the view
        verify(statisticsView).presenter = statisticsPresenter
    }

    @Test
    fun loadEmptyTasksFromRepository_CallViewToDisplay() = runBlockingSilent {
        setTasksAvailable(tasksRepository, tasks)

        // Given an initialized StatisticsPresenter with no tasks
        tasks.clear()

        // When loading of Tasks is requested
        statisticsPresenter.start()

        //Then progress indicator is shown
        verify(statisticsView).setProgressIndicator(true)

        // Then progress indicator is hidden and correct data is passed on to the view
        verify(statisticsView).setProgressIndicator(false)
        verify(statisticsView).showStatistics(0, 0)
    }

    @Test
    fun loadNonEmptyTasksFromRepository_CallViewToDisplay() = runBlockingSilent {
        setTasksAvailable(tasksRepository, tasks)

        // Given an initialized StatisticsPresenter with 1 active and 2 completed tasks

        // When loading of Tasks is requested
        statisticsPresenter.start()

        //Then progress indicator is shown
        verify(statisticsView).setProgressIndicator(true)


        // Then progress indicator is hidden and correct data is passed on to the view
        verify(statisticsView).setProgressIndicator(false)
        verify(statisticsView).showStatistics(1, 2)
    }

    @Test
    fun loadStatisticsWhenTasksAreUnavailable_CallErrorToDisplay() = runBlockingSilent {
        // Tasks data isn't available
        setTasksNotAvailable(tasksRepository)

        // When statistics are loaded
        statisticsPresenter.start()

        // Then an error message is shown
        verify(statisticsView).showLoadingStatisticsError()
    }

    private suspend fun setTasksAvailable(dataSource: TasksDataSource, tasks: List<Task>) {
        `when`(dataSource.getTasks()).thenReturn(Result.Success(tasks))
    }

    private suspend fun setTasksNotAvailable(dataSource: TasksDataSource) {
        `when`(dataSource.getTasks()).thenReturn(Result.Error(DataSourceException()))
    }
}
