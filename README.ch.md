![](usage.png)

# RxDownload

![](https://img.shields.io/badge/language-kotlin-brightgreen.svg) ![](https://img.shields.io/badge/RxJava-2.0-blue.svg)

[![](https://jitpack.io/v/ssseasonnn/RxDownload.svg)](https://jitpack.io/#ssseasonnn/RxDownload)

基于RxJava打造的下载工具, 支持多线程下载和断点续传,使用Kotlin编写

*Read this in other languages: [中文](README.ch.md), [English](README.md)* 


## Basic Usage

开始下载:

```kotlin
disposable = url.download()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeBy(
                onNext = {
                    //下载进度
                    button.text = "${it.downloadSizeStr()}/${it.totalSizeStr()}"
                    button.setProgress(it)
                },
                onComplete = {
                    //下载完成
                    button.text = "打开"
                },
                onError = {
                    //下载失败
                    button.text = "重试"
                }
        )    
```

停止下载:

```kotlin
disposable.dispose()    
```

## Task Manager

- 获取一个TaskManager对象:

    ```kotlin
    val taskManager = url.manager()
    ```
    
- 订阅状态更新通知:

    ```kotlin
    taskManager.subscribe { status ->
        // 获取下载状态
        btn_action.setStatus(status)
    }
        
    ``` 
    
- 取消状态更新订阅:

    ```kotlin
    taskManager.dispose()
    ```
    
- 开始下载:

    ```kotlin
    taskManager.start()
    ```

- 停止下载:

    ```kotlin
    taskManager.stop()
    ```
    
- 删除下载:

    ```kotlin
    taskManager.delete()
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
