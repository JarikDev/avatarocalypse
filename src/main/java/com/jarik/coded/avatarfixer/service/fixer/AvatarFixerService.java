package com.jarik.coded.avatarfixer.service.fixer;

import com.jarik.coded.avatarfixer.service.utils.AvatarFixerMetrics;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AvatarFixerService {
    private final CloseableHttpClient client;

    private final AvatarFixerMetrics metrics;
    private static final String INPUT_FILES_PATH = "D:\\downloads\\dwnlfromdwnl\\prog\\work\\avatar-fixer\\avatar-fixer\\src\\main\\resources\\avatars";
    //    public static final String DEFAULT_AVATAR = "https://mts-profile-s3.s3mts.ru/prod-avatars/default-avatars/avatar.png";
    public static final String DEFAULT_AVATAR_BASE = "https://static.ssl.mts.ru/mts_rf/images/profile_default/default-avatar-";
    private static final String NEW_LINE_SEPARATOR = System.getProperty("line.separator");

    public AvatarFixerService(CloseableHttpClient client, AvatarFixerMetrics metrics) {
        this.metrics = metrics;
        this.client = client;
    }

    public void fixEmAll() throws IOException {
        File folder = new File(INPUT_FILES_PATH);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String[] nameSegments = file.getName().split("\\.");
                String fixedFileName = nameSegments[0] + "_fixed_" + LocalDate.now() + "." + Optional.ofNullable(nameSegments[1]).orElse("csv");
                fixFile(file.getPath(), fixedFileName);
            }
        }
        metrics.displayFinalMetrics();
    }

    private void fixFile(String pathToInputFile, String fixedFileName) throws IOException {
        long howMuchToSkip = 0;

        File csvOutputFile = new File(fixedFileName);
        Path pathToOutputFile = Paths.get(fixedFileName);
        boolean isCreatedFile = false;
        if (csvOutputFile.exists()) {
            howMuchToSkip = Files.lines(pathToOutputFile).count();
        } else {
            csvOutputFile.createNewFile();
            isCreatedFile = true;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(pathToOutputFile, StandardOpenOption.APPEND)) {
            Scanner sc = new Scanner(new File(pathToInputFile));
            sc.useDelimiter("\\n");

            while (sc.hasNext()) {
                if (howMuchToSkip > 0) {
                    --howMuchToSkip;
                    sc.next();
                    continue;
                }
                String line = sc.next();
                line = line.replace(" ", "");
                System.out.println("Input line: " + line);
                String[] lineArr = line.split(";");
                String fixed = fix(lineArr[1], lineArr[2]);
                String result = lineArr[0].trim() + ";" + fixed + ";" + getAvatarId(fixed);
                System.out.println("Output line: " + result);
                metrics.pushMetrics();
                if (isCreatedFile) {
                    writer.write(result);
                    isCreatedFile = false;
                } else {
                    writer.newLine();
                    writer.write(result);
                }
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

    private String fix(String aratarLinks, String avatarIds) {

        String result = null;
        List<String> avatarLinksPriorityOne = getPriorityOneLinks(aratarLinks, avatarIds);
        if (!avatarLinksPriorityOne.isEmpty()) {
            result = getFirstExisting(avatarLinksPriorityOne);
            if (result != null) {
                return result;
            }
        }

        List<String> avatarLinks = toAvatarLinks(aratarLinks);
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

    private List<String> getPriorityOneLinks(String avatarArrayStr, String avatarIds) {
        List<String> avatarIdList = new ArrayList<>(Optional.ofNullable(avatarIds)
                .map(arr -> arr.replace("[", "").replace("]", "").replace(" ", ""))
                .map(str -> str.split(","))
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList));
        return new ArrayList<>(Optional.ofNullable(avatarArrayStr)
                .map(arr -> arr.replace("[", "").replace("]", "").replace(" ", ""))
                .map(str -> str.split(","))
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList))
                .stream()
                .filter(link -> {
                    String avatarIdFromLink = extractAvatarId(link);
                    return avatarIdList.contains(avatarIdFromLink) && !avatarIdFromLink.contains("default");
                })
                .collect(Collectors.toList());
    }

    private String extractAvatarId(String avatarLink) {
        if (avatarLink.contains("/")) {
            return StringUtils.substringAfterLast(avatarLink, "/");
        } else {
            return avatarLink;
        }
    }

    private boolean isExist(String avatarPath) {
        HttpUriRequest request = RequestBuilder.head(avatarPath).build();
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
