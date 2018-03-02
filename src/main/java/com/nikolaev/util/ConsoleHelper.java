package com.nikolaev.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {

    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message) {
        System.out.println(message);
    }

    public static String readString() {
        String text;
        try {
            text = reader.readLine();
        } catch (IOException e) {
            System.out.println("Oops something wrong. Try again");
            text = readString();
        }
        return text;
    }

    public static int readInt() {
        int num;
        try {
            num = Integer.valueOf(readString());
        } catch (NumberFormatException e) {
            System.out.println("Oops something wrong. Try again");
            num = readInt();
        }
        return num;
    }
}
