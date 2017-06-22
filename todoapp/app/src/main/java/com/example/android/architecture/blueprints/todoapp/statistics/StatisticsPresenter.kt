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
import rx.Observable
import rx.subscriptions.CompositeSubscription

/**
 * Listens to user actions from the UI ([StatisticsFragment]), retrieves the data and updates
 * the UI as required.
 */
class StatisticsPresenter(val tasksRepository: TasksRepository,
                          val statisticsView: StatisticsContract.View,
                          val schedulerProvider: BaseSchedulerProvider) : StatisticsContract.Presenter {

  private val mSubscriptions: CompositeSubscription = CompositeSubscription()

  init {
    this.statisticsView.setPresenter(this)
  }

  override fun subscribe() {
    loadStatistics()
  }

  override fun unsubscribe() {
    mSubscriptions.clear()
  }

  private fun loadStatistics() {
    statisticsView.setProgressIndicator(true)

    // The network request might be handled in a different thread so make sure Espresso knows
    // that the app is busy until the response is handled.
    EspressoIdlingResource.increment() // App is busy until further notice

    val tasks = tasksRepository
        .tasks
        .flatMap<Task>({ Observable.from(it) })
    val completedTasks = tasks.filter({ it.isCompleted }).count()
    val activeTasks = tasks.filter({ it.isActive }).count()
    val subscription = Observable
        .zip(completedTasks, activeTasks) { completed, active -> Pair.create(active, completed) }
        .subscribeOn(schedulerProvider.computation())
        .observeOn(schedulerProvider.ui())
        .doOnTerminate {
          if (!EspressoIdlingResource.idlingResource.isIdleNow()) {
            EspressoIdlingResource.decrement() // Set app as idle.
          }
        }
        .subscribe(
            // onNext
            { stats -> statisticsView.showStatistics(stats.first, stats.second) },
            // onError
            { throwable -> statisticsView.showLoadingStatisticsError() },
            // onCompleted
            { statisticsView.setProgressIndicator(false) }
        )
    mSubscriptions.add(subscription)
  }
}
