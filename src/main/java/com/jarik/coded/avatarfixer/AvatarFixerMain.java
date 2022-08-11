package com.jarik.coded.avatarfixer;

import com.jarik.coded.avatarfixer.service.AvatarFixerService;

import java.io.FileNotFoundException;

public class AvatarFixerMain {
    public static void main(String[] args) throws FileNotFoundException {
        AvatarFixerService service = new AvatarFixerService();
        service.fixEmAll();
    }
}
