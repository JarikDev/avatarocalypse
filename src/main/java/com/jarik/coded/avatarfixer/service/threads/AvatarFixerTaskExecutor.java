package com.jarik.coded.avatarfixer.service.threads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AvatarFixerTaskExecutor {
    private static final ExecutorService executor = Executors.newFixedThreadPool(100);
    private static volatile AvatarFixerTaskExecutor instance;

    private AvatarFixerTaskExecutor() {
    }

    public static AvatarFixerTaskExecutor getInstance() {
        synchronized (AvatarFixerTaskExecutor.class) {
            if (instance == null) {
                synchronized (AvatarFixerTaskExecutor.class) {
                    if (instance == null) {
                        instance = new AvatarFixerTaskExecutor();
                    }
                }
            }
            return instance;
        }
    }

    public void submitTask(Runnable task) {
        executor.submit(task);
    }

    public void close() throws InterruptedException {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdown();
        }
    }
}
