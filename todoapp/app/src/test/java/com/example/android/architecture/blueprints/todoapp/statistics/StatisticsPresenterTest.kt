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

package com.example.android.architecture.blueprints.todoapp.statistics

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider
import com.google.common.collect.Lists

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import rx.Observable

import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for the implementation of [StatisticsPresenter]
 */
class StatisticsPresenterTest {

  @Mock
  private val mTasksRepository: TasksRepository? = null

  @Mock
  private val mStatisticsView: StatisticsContract.View? = null

  private var mSchedulerProvider: BaseSchedulerProvider? = null

  private var mStatisticsPresenter: StatisticsPresenter? = null

  @Before
  fun setupStatisticsPresenter() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    // Make the sure that all schedulers are immediate.
    mSchedulerProvider = ImmediateSchedulerProvider()

    // Get a reference to the class under test
    mStatisticsPresenter = StatisticsPresenter(mTasksRepository!!, mStatisticsView!!,
        mSchedulerProvider!!)

    // The presenter won't update the view unless it's active.
    `when`(mStatisticsView.isActive).thenReturn(true)

    // We subscribe the tasks to 3, with one active and two completed
    TASKS = Lists.newArrayList(Task("Title1", "Description1"),
        Task("Title2", "Description2", true), Task("Title3", "Description3", true))
  }

  @Test
  fun createPresenter_setsThePresenterToView() {
    // Get a reference to the class under test
    mStatisticsPresenter = StatisticsPresenter(mTasksRepository!!, mStatisticsView!!,
        mSchedulerProvider!!)

    // Then the presenter is set to the view
    verify(mStatisticsView).setPresenter(mStatisticsPresenter!!)
  }

  @Test
  fun loadEmptyTasksFromRepository_CallViewToDisplay() {
    // Given an initialized StatisticsPresenter with no tasks
    TASKS!!.clear()
    setTasksAvailable(TASKS!!)

    // When loading of Tasks is requested
    mStatisticsPresenter!!.subscribe()

    //Then progress indicator is shown
    verify(mStatisticsView)?.setProgressIndicator(true)

    // Callback is captured and invoked with stubbed tasks
    verify<TasksRepository>(mTasksRepository).tasks

    // Then progress indicator is hidden and correct data is passed on to the view
    verify(mStatisticsView)?.setProgressIndicator(false)
    verify(mStatisticsView)?.showStatistics(0, 0)
  }

  @Test
  fun loadNonEmptyTasksFromRepository_CallViewToDisplay() {
    // Given an initialized StatisticsPresenter with 1 active and 2 completed tasks
    setTasksAvailable(TASKS!!)

    // When loading of Tasks is requested
    mStatisticsPresenter!!.subscribe()

    //Then progress indicator is shown
    verify(mStatisticsView)?.setProgressIndicator(true)

    // Then progress indicator is hidden and correct data is passed on to the view
    verify(mStatisticsView)?.setProgressIndicator(false)
    verify(mStatisticsView)?.showStatistics(1, 2)
  }

  @Test
  fun loadStatisticsWhenTasksAreUnavailable_CallErrorToDisplay() {
    // Given that tasks data isn't available
    setTasksNotAvailable()

    // When statistics are loaded
    mStatisticsPresenter!!.subscribe()

    // Then an error message is shown
    verify(mStatisticsView)?.showLoadingStatisticsError()
  }

  private fun setTasksAvailable(tasks: List<Task>) {
    `when`(mTasksRepository!!.tasks).thenReturn(Observable.just(tasks))
  }

  private fun setTasksNotAvailable() {
    `when`(mTasksRepository!!.tasks).thenReturn(Observable.error<List<Task>>(Exception()))
  }

  companion object {

    private var TASKS: MutableList<Task>? = null
  }
}
