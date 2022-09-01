package com.jarik.coded.avatarfixer;

import com.jarik.coded.avatarfixer.service.checker.AvatarFixedChecker;
import com.jarik.coded.avatarfixer.service.fixer.AvatarFixerService;
import com.jarik.coded.avatarfixer.service.utils.AvatarFixerMetrics;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class AvatarFixerMain {
    public static void main(String[] args) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        AvatarFixerMetrics metrics = AvatarFixerMetrics.getInstance();

        AvatarFixerService service = new AvatarFixerService(client, metrics);
        service.fixEmAll();

        AvatarFixedChecker tester = new AvatarFixedChecker(client, metrics);
        tester.checkFileWithFixedAvatars();

        metrics.displayFinalTestMetrics();
    }
}
