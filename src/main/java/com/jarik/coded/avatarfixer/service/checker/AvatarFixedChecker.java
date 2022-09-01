package com.jarik.coded.avatarfixer.service.checker;

import com.jarik.coded.avatarfixer.service.utils.AvatarFixerMetrics;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AvatarFixedChecker {
    private final CloseableHttpClient client;
    private final AvatarFixerMetrics metrics;
    //    private static final String INPUT_FILES_PATH = "D:\\downloads\\dwnlfromdwnl\\prog\\work\\avatar-fixer\\avatar-fixer\\avatarstwofield_fixed.csv";
//    private static final String INPUT_FILES_PATH = "D:\\downloads\\dwnlfromdwnl\\prog\\work\\avatar-fixer\\avatar-fixer\\test_fixed.csv";
    private static final String INPUT_FILES_PATH = "test_fixed_2022-08-30.csv";

    public AvatarFixedChecker(CloseableHttpClient client, AvatarFixerMetrics metrics) {
        this.client = client;
        this.metrics = metrics;
    }

    private Set<String> getFixedFiles() {
        String userDir = System.getProperty("user.dir");
        return Stream.of(Objects.requireNonNull(new File(userDir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .filter(fn -> fn.endsWith(".csv"))
                .collect(Collectors.toSet());
    }

    public void checkFileWithFixedAvatars() throws FileNotFoundException {
        Set<String> fixedFiles = getFixedFiles();
        for (String fileName : fixedFiles) {
            Scanner sc = new Scanner(new File(fileName));
            sc.useDelimiter("\\n");
            while (sc.hasNext()) {
                String line = sc.next();
                String[] lineArr = line.split(";");
                String link = lineArr[1];
                System.out.println("### Testing link: " + link);
                System.out.println("Response code: " + getResponseCode(link));
                metrics.pushMetrics();
            }
            sc.close();
            metrics.displayFinalTestMetrics();
        }
    }

    private Integer getResponseCode(String avatarPath) {
        HttpUriRequest request = RequestBuilder.get(avatarPath).build();
        try (CloseableHttpResponse response = client.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            metrics.pushTestStatistics(avatarPath, code);
            System.out.println("Response code: " + code);
            return code;
        } catch (Exception e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
