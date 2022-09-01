package com.jarik.coded.avatarfixer.service.threads;

import com.jarik.coded.avatarfixer.service.utils.AvatarFixerMetrics;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class AvatarFixerTask implements Runnable {
    private  final String DEFAULT_AVATAR_BASE = "https://static.ssl.mts.ru/mts_rf/images/profile_default/default-avatar-";
    private final String line;
    private final AvatarFixerMetrics metrics;
    private final PrintWriter pw;
    private final CloseableHttpClient client;

    public AvatarFixerTask(String line, AvatarFixerMetrics metrics, PrintWriter pw, CloseableHttpClient client) {
        this.line = line;
        this.metrics = metrics;
        this.pw = pw;
        this.client = client;
    }

    @Override
    public void run() {
        System.out.println("Input line: " + line);
        String[] lineArr = line.split(";");
        String fixed = fix(lineArr[1]);
        String result = lineArr[0].trim() + ";" + fixed + ";" + getAvatarId(fixed);
        System.out.println("Output line: " + result);
        metrics.pushMetrics();
        pw.println(result);
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
        }
        if (result == null && !defaultAvatars.isEmpty()) {
            result = getFirstExisting(defaultAvatars);
        }
        if (result == null) {
            result = getDefaultAvatar();
        }
        return result;
    }

    private String getDefaultAvatar() {
        Random random = new Random();
        int avatarIndex = random.nextInt(199) + 1;
        String avatarIndexStr = avatarIndex < 10 ? "00".concat(String.valueOf(avatarIndex)) :
                avatarIndex > 99 ? String.valueOf(avatarIndex) : "0".concat(String.valueOf(avatarIndex));
        return DEFAULT_AVATAR_BASE.concat(avatarIndexStr).concat(".png");

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
            metrics.pushRespStatus(code);
            System.out.println("Response code: " + code);
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
