package com.jarik.coded.avatarfixer.service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class AvatarFixerService {

    private static final CloseableHttpClient client = HttpClients.createDefault();
    private final AvatarFixerMetrics avatarFixerMetrics = new AvatarFixerMetrics();
    private static final String INPUT_FILES_PATH = "D:\\downloads\\dwnlfromdwnl\\prog\\work\\avatar-fixer\\avatar-fixer\\src\\main\\resources\\avatars";
    private static final String DEFAULT_AVATAR = "https://mts-profile-s3.s3mts.ru/prod-avatars/default-avatars/avatar.png";

    public void fixEmAll() throws FileNotFoundException {
        File folder = new File(INPUT_FILES_PATH);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String[] nameSegments = file.getName().split("\\.");
                String fixedFileName = nameSegments[0] + "_fixed." + Optional.ofNullable(nameSegments[1]).orElse("csv");
                fixFile(file.getPath(), fixedFileName);
            }
        }
        avatarFixerMetrics.displayFinalMetrics();
    }

    private void fixFile(String pathToInputFile, String fixedFileName) throws FileNotFoundException {
        File csvOutputFile = new File(fixedFileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            Scanner sc = new Scanner(new File(pathToInputFile));
            sc.useDelimiter("\\n");
            while (sc.hasNext()) {
                String line = sc.next();
                System.out.println("Input line: " + line);
                String[] lineArr = line.split(";");
                String fixed = fix(lineArr[1]);
                if (fixed != null && !"null".equals(fixed)) {
                    String result = lineArr[0].trim() + ";" + fixed + ";" + getAvatarId(fixed);
                    pw.println(result);
                    System.out.println("Output line: " + result);
                }
                avatarFixerMetrics.pushMetrics();
            }
            sc.close();
        }
    }

    private String getAvatarId(String avatarLink) {
        String result = avatarLink;
        if (result.contains("/")) {
            int slashIndex = result.lastIndexOf("/") + 1;
            result = result.substring(slashIndex);
            result = getAvatarId(result);
        }
        return result;
    }

    private String fix(String input) {
        String result = null;
        List<String> avatarLinks = toAvatarLinks(input);
        List<String> defaultAvatars = avatarLinks.stream()
                .filter(l -> l.contains("default"))
                .collect(Collectors.toList());
        List<String> usersAvatars = avatarLinks.stream()
                .filter(l -> !l.contains("default"))
                .collect(Collectors.toList());

        if (!usersAvatars.isEmpty()) {
            result = getFirstExisting(usersAvatars);
        } else if (!defaultAvatars.isEmpty()) {
            result = getFirstExisting(defaultAvatars);
        }
        if (result == null) {
            result = DEFAULT_AVATAR;

        }
        return result;
    }

    private String getFirstExisting(List<String> avatarLinks) {
        return avatarLinks
                .stream()
                .filter(this::isExist)
                .findFirst()
                .orElse(null);
    }

    private List<String> toAvatarLinks(String avatarArrayStr) {
        return new ArrayList<>(Optional.ofNullable(avatarArrayStr)
                .map(arr -> arr.replace("[", "").replace("]", "").replace(" ", ""))
                .map(str -> str.split(","))
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList));
    }

    private boolean isExist(String avatarPath) {
        HttpUriRequest request = RequestBuilder.get(avatarPath).build();
        try (CloseableHttpResponse response = client.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            avatarFixerMetrics.pushRespStatus(code);
            System.out.println("Response code: " + code);
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
