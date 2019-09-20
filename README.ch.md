![](usage.png)

# RxDownload

![](https://img.shields.io/badge/language-kotlin-brightgreen.svg) ![](https://img.shields.io/badge/RxJava-2.0-blue.svg)

[![](https://jitpack.io/v/ssseasonnn/RxDownload.svg)](https://jitpack.io/#ssseasonnn/RxDownload)

基于RxJava打造的下载工具, 支持多线程下载和断点续传,使用Kotlin编写

*Read this in other languages: [中文](README.ch.md), [English](README.md), [Changelog](CHANGELOG.md)* 

## Prepare

- 添加jitpack仓库:

    ```gradle
    maven { url 'https://jitpack.io' }
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots/'
    }
    ```

    > 由于依赖retrofit-2.7.0, 而retrofit 2.7.0目前还没正式发布,
    因此暂时需要添加 **https://oss.sonatype.org/content/repositories/snapshots/** 仓库
    
- 添加RxDownload依赖:

    ```gradle
    //按需加载
    implementation "com.github.ssseasonnn.RxDownload:rxdownload4:1.0.4"
    implementation "com.github.ssseasonnn.RxDownload:rxdownload4-manager:1.0.4"
    implementation "com.github.ssseasonnn.RxDownload:rxdownload4-notification:1.0.4"
    implementation "com.github.ssseasonnn.RxDownload:rxdownload4-recorder:1.0.4"
    
    or: 
    //添加RxDownload4的所有依赖
    implementation "com.github.ssseasonnn:RxDownload:1.0.4"
    ```

## Basic Usage

- 开始下载:

    ```kotlin
    disposable = url.download()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                    onNext = { progress ->
                        //下载进度
                        button.text = "${progress.downloadSizeStr()}/${progress.totalSizeStr()}"
                        button.setProgress(progress)
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

- 停止下载:

    ```kotlin
    disposable.dispose()    
    ```

- 获取下载文件:

    ```kotlin
    val file = url.file() 
    // 使用文件...    
    ```

## Task Manager

- 获取一个TaskManager对象:

    ```kotlin
    val taskManager = url.manager()
    ```
    
- 订阅状态更新通知:

    ```kotlin
    //keep this tag for dispose
    val tag = taskManager.subscribe { status ->
        // 获取下载状态
        when (status) {
            is Normal -> {}
            is Started -> {}
            is Downloading -> {}
            is Paused -> {}
            is Completed -> {}
            is Failed -> {}
            is Deleted -> {}
        }
    }
        
    ``` 
    
    > **progress**可从**status**中获取, 当status为**Failed**时, 能额外从中获取**throwable**,代表失败的原因
    
- 取消状态更新订阅:

    ```kotlin
    //dispose tag
    taskManager.dispose(tag)
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

- 获取下载文件:

    ```kotlin
    val file = taskManager.file()
    // 使用文件...  
    ```

## Task Recorder

- 获取所有下载记录列表:

    ```kotlin
     RxDownloadRecorder.getAllTask()
           .observeOn(AndroidSchedulers.mainThread())
           .subscribeBy { list ->
               //list of TaskEntity                        
           }
    ```
    
- 查询某个状态的所有下载记录:

    ```kotlin
     // 查询所有下载完成的记录
     RxDownloadRecorder.getAllTaskWithStatus(Completed())
           .observeOn(AndroidSchedulers.mainThread())
           .subscribeBy { list ->
               //list of TaskEntity                        
           } 
    ``` 
    
- 分页查询下载记录列表:

    ```kotlin
     RxDownloadRecorder.getTaskList(page, pageSize)
           .observeOn(AndroidSchedulers.mainThread())
           .subscribeBy { list ->
               //list of TaskEntity                        
           }
    ```
    
- 分页查询某个状态下的下载记录列表:

    ```kotlin
     // 获取下载完成的分页列表
     RxDownloadRecorder.getTaskListWithStatus(Completed(), page, pageSize)
           .observeOn(AndroidSchedulers.mainThread())
           .subscribeBy { list ->
               //list of TaskEntity                        
           }
    ```

    > **TaskEntity** 拥有一个**abnormalExit**字段, 该字段用来表示该Task是否是被APP强制杀进程导致的暂停


- 全部开始:

    ```kotlin
     RxDownloadRecorder.startAll()
    ```
    
- 全部暂停:

    ```kotlin
     RxDownloadRecorder.stopAll()
    ```
    
- 全部删除:

    ```kotlin
     RxDownloadRecorder.deleteAll()
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
