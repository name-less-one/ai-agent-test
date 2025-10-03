package com.example.memberprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class MemberProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberProcessorApplication.class, args);
    }

    @Service
    public static class MemberFileService {

        private static final int TARGET_LENGTH = 20;

        public void processFile(String inputFilePath, String outputFilePath) throws IOException {
            Path inputPath = Paths.get(inputFilePath);
            
            if (!Files.exists(inputPath)) {
                throw new FileNotFoundException("入力ファイルが見つかりません: " + inputFilePath);
            }

            try (FileInputStream fis = new FileInputStream(inputFilePath);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 FileOutputStream fos = new FileOutputStream(outputFilePath);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                while (bis.available() > 0) {
                    int lengthByte1 = bis.read();
                    int lengthByte2 = bis.read();
                    
                    if (lengthByte1 == -1 || lengthByte2 == -1) {
                        break;
                    }

                    int memberNumberLength = Integer.parseInt(
                        String.valueOf((char) lengthByte1) + (char) lengthByte2
                    );

                    byte[] memberNumberBytes = new byte[memberNumberLength];
                    int bytesRead = bis.read(memberNumberBytes);
                    
                    if (bytesRead != memberNumberLength) {
                        throw new IOException("ファイル形式が正しくありません");
                    }

                    String memberNumber = new String(memberNumberBytes);
                    String paddedMemberNumber = padToLength(memberNumber, TARGET_LENGTH);
                    bos.write(paddedMemberNumber.getBytes());
                }
            }
        }

        public String padToLength(String memberNumber, int targetLength) {
            if (memberNumber.length() >= targetLength) {
                return memberNumber.substring(0, targetLength);
            }
            
            StringBuilder sb = new StringBuilder(memberNumber);
            while (sb.length() < targetLength) {
                sb.append(' ');
            }
            return sb.toString();
        }
    }

    @Component
    @org.springframework.context.annotation.Profile("!test")
    public static class FileProcessor implements CommandLineRunner {

        private static final String OUTPUT_FILE = "account_flatdata.txt";
        private final MemberFileService memberFileService;

        public FileProcessor(MemberFileService memberFileService) {
            this.memberFileService = memberFileService;
        }

        @Override
        public void run(String... args) throws Exception {
            if (args.length < 1) {
                System.out.println("使用方法: java -jar member-processor.jar <入力ファイルパス>");
                System.exit(1);
            }

            String inputFilePath = args[0];
            Path outputPath = Paths.get(OUTPUT_FILE);

            if (Files.exists(outputPath)) {
                System.out.println("出力ファイルが既に存在するため、終了します。");
                System.exit(0);
            }

            memberFileService.processFile(inputFilePath, OUTPUT_FILE);
            System.out.println("処理が完了しました。");
        }
    }
}
