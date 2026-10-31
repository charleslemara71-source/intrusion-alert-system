package com.zuvo.util;   // keep the package line Eclipse gave you

public class LoggerTest {
    public static void main(String[] args) {
        SecurityLogger.log("TEST_EVENT", "tester", "127.0.0.1", "/test");
        System.out.println("Done. Now check C:\\logs\\zuvo-app.log");
    }
}