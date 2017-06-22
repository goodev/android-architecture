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

import android.support.v4.util.Pair
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.google.common.base.Preconditions.checkNotNull
import rx.Observable
import rx.functions.Func1
import rx.subscriptions.CompositeSubscription

/**
 * Listens to user actions from the UI ([StatisticsFragment]), retrieves the data and updates
 * the UI as required.
 */
class StatisticsPresenter(tasksRepository: TasksRepository,
                          statisticsView: StatisticsContract.View,
                          schedulerProvider: BaseSchedulerProvider) : StatisticsContract.Presenter {

  private val mTasksRepository: TasksRepository

  private val mStatisticsView: StatisticsContract.View

  private val mSchedulerProvider: BaseSchedulerProvider

  private val mSubscriptions: CompositeSubscription

  init {
    mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null")
    mStatisticsView = checkNotNull(statisticsView, "statisticsView cannot be null!")
    mSchedulerProvider = checkNotNull(schedulerProvider, "schedulerProvider cannot be null")

    mSubscriptions = CompositeSubscription()
    mStatisticsView.setPresenter(this)
  }

  override fun subscribe() {
    loadStatistics()
  }

  override fun unsubscribe() {
    mSubscriptions.clear()
  }

  private fun loadStatistics() {
    mStatisticsView.setProgressIndicator(true)

    // The network request might be handled in a different thread so make sure Espresso knows
    // that the app is busy until the response is handled.
    EspressoIdlingResource.increment() // App is busy until further notice

    val tasks = mTasksRepository
        .tasks
        .flatMap<Task>(Func1<List<Task>, Observable<out Task>> { Observable.from(it) })
    val completedTasks = tasks.filter(Func1<Task, Boolean> { it.isCompleted }).count()
    val activeTasks = tasks.filter(Func1<Task, Boolean> { it.isActive }).count()
    val subscription = Observable
        .zip(completedTasks, activeTasks) { completed, active -> Pair.create(active, completed) }
        .subscribeOn(mSchedulerProvider.computation())
        .observeOn(mSchedulerProvider.ui())
        .doOnTerminate {
          if (!EspressoIdlingResource.idlingResource.isIdleNow()) {
            EspressoIdlingResource.decrement() // Set app as idle.
          }
        }
        .subscribe(
            // onNext
            { stats -> mStatisticsView.showStatistics(stats.first, stats.second) },
            // onError
            { throwable -> mStatisticsView.showLoadingStatisticsError() }
            // onCompleted
        ) { mStatisticsView.setProgressIndicator(false) }
    mSubscriptions.add(subscription)
  }
}
