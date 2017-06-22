# TODO-MVP-RXJAVA-KOTLIN

Project owners: [Goodev](https://github.com/goodev).

[TODO-MVP-RXJAVA](https://github.com/googlesamples/android-architecture/tree/todo-mvp-rxjava) Project owners: [Erik Hellman](https://github.com/erikhellman) & [Florina Muntenescu (upday)](https://github.com/florina-muntenescu).

### Summary

This sample is based on the [TODO-MVP-RXJAVA](https://github.com/googlesamples/android-architecture/tree/todo-mvp-rxjava) project and uses Android Studio 3.0 to convert the Java code to Kotlin.

Compared to the TODO-MVP, both the Presenter contracts and the implementation of the Views stay the same. The changes are done to the data model layer and in the implementation of the Presenters. For the sake of simplicity we decided to keep the RxJava usage minimal, leaving optimizations like RxJava caching aside.

The data model layer exposes RxJava ``Observable`` streams as a way of retrieving tasks. The ``TasksDataSource`` interface contains methods like:

```kotlin
  val tasks: Observable<List<Task>>

  fun getTask(taskId: String): Observable<Task>
```

This is implemented in ``TasksLocalDataSource`` with the help of [SqlBrite](https://github.com/square/sqlbrite). The result of queries to the database being easily exposed as streams of data.

```kotlin
  override val tasks: Observable<List<Task>>
    get() {
      val projection = arrayOf(TaskEntry.COLUMN_NAME_ENTRY_ID, TaskEntry.COLUMN_NAME_TITLE, TaskEntry.COLUMN_NAME_DESCRIPTION, TaskEntry.COLUMN_NAME_COMPLETED)
      val sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), TaskEntry.TABLE_NAME)
      return mDatabaseHelper.createQuery(TaskEntry.TABLE_NAME, sql)
          .mapToList(mTaskMapperFunction)
    }
```

The ``TasksRepository`` combines the streams of data from the local and the remote data sources, exposing it to whoever needs it. In our project, the Presenters and the unit tests are actually the consumers of these ``Observable``s.

The Presenters subscribe to the ``Observable``s from the ``TasksRepository`` and after manipulating the data, they are the ones that decide what the views should display, in the ``.subscribe(...)`` method. Also, the Presenters are the ones that decide on the working threads. For example, in the ``StatisticsPresenter``, we decide on which thread we should do the computation of the active and completed tasks and what should happen when this computation is done: show the statistics, if all is ok; show loading statistics error, if needed; and telling the view that the loading indicator should not be visible anymore.

```kotlin
...
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
```

Handling of the working threads is done with the help of RxJava's `Scheduler`s. For example, the creation of the database together with all the database queries is happening on the IO thread. The `subscribeOn` and `observeOn` methods are used in the Presenter classes to define that the `Observer`s will operate on the computation thread and that the observing is on the main thread.

### Dependencies

* [RxJava](https://github.com/ReactiveX/RxJava)
* [RxAndroid](https://github.com/ReactiveX/RxAndroid)
* [SqlBrite](https://github.com/square/sqlbrite)
* [Kotlin](https://kotlinlang.org)


## Features

### Kotlin

### Complexity - understandability

#### Use of architectural frameworks/libraries/tools:

Building an app with RxJava is not trivial as it uses new concepts.

#### Conceptual complexity

Developers need to be familiar with RxJava, which is not trivial.

### Testability

#### Unit testing

Very High. Given that the RxJava ``Observable``s are highly unit testable, unit tests are easy to implement.

#### UI testing

Similar with TODO-MVP

### Code metrics

Compared to TODO-MVP, new classes were added for handing the ``Schedulers`` that provide the working threads.

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            49           1110           1413           3740 (3450 in MVP)
XML                             34             97            337            601
-------------------------------------------------------------------------------
SUM:                            83           1207           1750           4341

```
### Maintainability

#### Ease of amending or adding a feature

High.

#### Learning cost

Medium as RxJava is not trivial.
