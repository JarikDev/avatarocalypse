package com.jarik.coded.avatarfixer.service.utils;

import lombok.ToString;

import static com.jarik.coded.avatarfixer.service.fixer.AvatarFixerService.DEFAULT_AVATAR_BASE;


@ToString
public class AvatarFixerMetrics {
    private int attemptNumber = 0;
    private final long timeProcessingInit = System.currentTimeMillis();
    private long timeProcessingRecordInit = System.currentTimeMillis();
    private long resp200 = 0;
    private long resp404 = 0;
    private long respOther = 0;
    private long resp404DefaultS3Avatar = 0;

    private AvatarFixerMetrics() {
    }

    private static volatile AvatarFixerMetrics instance;

    public static AvatarFixerMetrics getInstance() {
        synchronized (AvatarFixerMetrics.class) {
            if (instance == null) {
                synchronized (AvatarFixerMetrics.class) {
                    if (instance == null) {
                        instance = new AvatarFixerMetrics();
                    }
                }
            }
            return instance;
        }
    }


    public synchronized void pushRespStatus(int status) {
        if (status == 200) {
            this.resp200 = resp200 + 1;
        } else if (status == 404) {
            this.resp404 = resp404 + 1;
        } else {
            this.respOther = respOther + 1;
        }
    }

    public synchronized void pushTestStatistics(String avatarPath, int status) {
        if (status == 200) {
            this.resp200 = resp200 + 1;
        } else if (status == 404) {
            this.resp404 = resp404 + 1;
            if (avatarPath.startsWith(DEFAULT_AVATAR_BASE)) {
                this.resp404DefaultS3Avatar = resp404DefaultS3Avatar + 1;
            }
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
        long averageRecordProcessingTime = attemptNumber == 0 ? 0 : (totalTimeElapsed * 1000) / attemptNumber;

        System.out.printf("### total attempts: %s ### time elapsed: %s ### average record processing time: %s ms ### resp 200: %s resp 404: %s resp other: %s ### %n",
                attemptNumber, getTotalTimeElapsedMessage(), averageRecordProcessingTime, resp200, resp404, respOther);
    }

    public synchronized void displayFinalTestMetrics() {
        long totalTimeElapsed = getTotalTimeElapsed();
        long averageRecordProcessingTime = attemptNumber == 0 ? 0 : (totalTimeElapsed * 1000) / attemptNumber;

        System.out.printf("### total attempts: %s ### time elapsed: %s ### average record processing time: %s ms ### resp 200: %s resp 404: %s resp 404 default avatar : %s resp other: %s ### ",
                attemptNumber, getTotalTimeElapsedMessage(), averageRecordProcessingTime, resp200, resp404, resp404DefaultS3Avatar, respOther);

//        System.out.printf("### total attempts: " + attemptNumber + " ### time elapsed: " + getTotalTimeElapsedMessage() +
//                " ### average record processing time: " + averageRecordProcessingTime + " ms ### " +
//                "resp 200: " + resp200 + " resp 404: " + resp404 + " resp 404 default avatar : " + resp404DefaultS3Avatar +
//                " resp other: " + respOther + " ### ");
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
