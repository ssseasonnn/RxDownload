![](usage.png)

# RxDownload

![](https://img.shields.io/badge/language-kotlin-brightgreen.svg) ![](https://img.shields.io/badge/RxJava-2.0-blue.svg)

[![](https://jitpack.io/v/ssseasonnn/RxDownload.svg)](https://jitpack.io/#ssseasonnn/RxDownload)

A multi-threaded download tool written with RxJava and Kotlin

*Read this in other languages: [中文](README.ch.md), [English](README.md)* 


## Basic Usage

- Start download:

    ```kotlin
    disposable = url.download()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                    onNext = { 
                        //download progress
                        button.text = "${it.downloadSizeStr()}/${it.totalSizeStr()}"
                        button.setProgress(it)
                    },
                    onComplete = {
                        //download complete
                        button.text = "Open"
                    },
                    onError = {
                        //download failed
                        button.text = "Retry"
                    }
            )    
    ```

- Stop download:

    ```kotlin
    disposable.dispose()    
    ```
    
- Get download file:

    ```kotlin
    val file = url.file() 
    // use file...   
    ```

## Task Manager

- Get a TaskManager object:

    ```kotlin
    val taskManager = url.manager()
    ```
    
- Subscribe to status update:

    ```kotlin
    taskManager.subscribe { status ->
        // Get download status
        btn_action.setStatus(status)
    }
        
    ``` 
    
- Cancel status update subscription:

    ```kotlin
    taskManager.dispose()
    ```
    
- Start download:

    ```kotlin
    taskManager.start()
    ```

- Stop download:

    ```kotlin
    taskManager.stop()
    ```
    
- Delete download:

    ```kotlin
    taskManager.delete()
    ```

- Get download file:

    ```kotlin
    val file = taskManager.file() 
    // use file...   
    ```

## License

> ```
> Copyright 2019 Season.Zlc
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
> ```
