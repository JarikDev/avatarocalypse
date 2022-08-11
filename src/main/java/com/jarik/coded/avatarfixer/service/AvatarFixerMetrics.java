package com.jarik.coded.avatarfixer.service;

import lombok.ToString;

@ToString
public class AvatarFixerMetrics {
    private int attemptNumber = 0;
    private final long timeProcessingInit = System.currentTimeMillis();
    private long timeProcessingRecordInit = System.currentTimeMillis();
    private long resp200 = 0;
    private long resp404 = 0;
    private long respOther = 0;

    public synchronized void pushRespStatus(int status) {
        if (status == 200) {
            this.resp200 = resp200 + 1;
        } else if (status == 404) {
            this.resp404 = resp404 + 1;
        } else {
            this.respOther = respOther + 1;
        }
    }

    public synchronized void pushMetrics() {
        attemptNumber = attemptNumber + 1;
        long timeElapsedForOneRecord = System.currentTimeMillis() - timeProcessingRecordInit;
        System.out.println("### attempt: " + attemptNumber + " ### time elapsed: " + timeElapsedForOneRecord + " ### ");
        timeProcessingRecordInit = System.currentTimeMillis();
    }

    public synchronized void displayFinalMetrics() {
        long totalTimeElapsed = getTotalTimeElapsed();
        long averageRecordProcessingTime = (totalTimeElapsed * 1000) / attemptNumber;
        System.out.println("### total attempts: " + attemptNumber + " ### time elapsed: " + getTotalTimeElapsedMessage() +
                " ### average record processing time: " + averageRecordProcessingTime + " ms ### " +
                "resp 200: " + resp200 + "resp 404: " + resp404 + "resp other: " + respOther +
                " ### ");
    }

    private long getTotalTimeElapsed() {
        return (System.currentTimeMillis() - timeProcessingInit) / 1000;
    }

    private String getTotalTimeElapsedMessage() {
        long timeElapsedSeconds = getTotalTimeElapsed();
        long hours = timeElapsedSeconds / 3600;
        long minutes = (timeElapsedSeconds - hours * 3600) / 60;
        long seconds = (timeElapsedSeconds - hours * 3600 - minutes * 60);
        return hours + " hours " + minutes + " minutes " + seconds + " seconds ";
    }
}
