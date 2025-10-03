package com.example.memberprocessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class MemberProcessorApplicationTest {

    private static final String OUTPUT_FILE = "account_flatdata.txt";
    
    @Autowired
    private MemberProcessorApplication.MemberFileService memberFileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path outputPath = Paths.get(OUTPUT_FILE);
        try {
            Files.deleteIfExists(outputPath);
        } catch (IOException e) {
        }
    }

    @AfterEach
    void tearDown() {
        Path outputPath = Paths.get(OUTPUT_FILE);
        try {
            Files.deleteIfExists(outputPath);
        } catch (IOException e) {
        }
    }

    @Test
    void testProcessFile_SingleMemberNumber() throws Exception {
        String inputFile = tempDir.resolve("test_input.txt").toString();
        String outputFile = tempDir.resolve("output.txt").toString();
        createTestInputFile(inputFile, "15123456789012345");

        memberFileService.processFile(inputFile, outputFile);

        String output = Files.readString(Paths.get(outputFile));
        assertEquals(20, output.length());
        assertTrue(output.startsWith("123456789012345"));
        assertTrue(output.endsWith("     "));
    }

    @Test
    void testProcessFile_MultipleMemberNumbers() throws Exception {
        String inputFile = tempDir.resolve("test_input.txt").toString();
        String outputFile = tempDir.resolve("output.txt").toString();
        createTestInputFile(inputFile, 
            "15123456789012345",
            "162345678901234567",
            "2012345678901234567890"
        );

        memberFileService.processFile(inputFile, outputFile);

        String output = Files.readString(Paths.get(outputFile));
        assertEquals(60, output.length());
        
        String member1 = output.substring(0, 20);
        String member2 = output.substring(20, 40);
        String member3 = output.substring(40, 60);
        
        assertTrue(member1.startsWith("123456789012345"));
        assertTrue(member2.startsWith("2345678901234567"));
        assertTrue(member3.equals("12345678901234567890"));
    }

    @Test
    void testProcessFile_20DigitMemberNumber() throws Exception {
        String inputFile = tempDir.resolve("test_input.txt").toString();
        String outputFile = tempDir.resolve("output.txt").toString();
        createTestInputFile(inputFile, "2012345678901234567890");

        memberFileService.processFile(inputFile, outputFile);

        String output = Files.readString(Paths.get(outputFile));
        assertEquals(20, output.length());
        assertEquals("12345678901234567890", output);
    }

    @Test
    void testProcessFile_15DigitMemberNumber() throws Exception {
        String inputFile = tempDir.resolve("test_input.txt").toString();
        String outputFile = tempDir.resolve("output.txt").toString();
        createTestInputFile(inputFile, "15123456789012345");

        memberFileService.processFile(inputFile, outputFile);

        String output = Files.readString(Paths.get(outputFile));
        assertEquals(20, output.length());
        assertTrue(output.startsWith("123456789012345"));
        for (int i = 15; i < 20; i++) {
            assertEquals(' ', output.charAt(i));
        }
    }

    @Test
    void testInputFileNotFound() {
        String inputFile = tempDir.resolve("nonexistent.txt").toString();
        String outputFile = tempDir.resolve("output.txt").toString();

        assertThrows(FileNotFoundException.class, () -> {
            memberFileService.processFile(inputFile, outputFile);
        });
    }

    @Test
    void testPadToLength() {
        String result1 = memberFileService.padToLength("12345", 20);
        assertEquals(20, result1.length());
        assertEquals("12345               ", result1);

        String result2 = memberFileService.padToLength("12345678901234567890", 20);
        assertEquals(20, result2.length());
        assertEquals("12345678901234567890", result2);

        String result3 = memberFileService.padToLength("123456789012345", 20);
        assertEquals(20, result3.length());
        assertEquals("123456789012345     ", result3);
    }

    @Test
    void testPadToLength_Truncation() {
        String result = memberFileService.padToLength("123456789012345678901234567890", 20);
        assertEquals(20, result.length());
        assertEquals("12345678901234567890", result);
    }

    @Test
    void testComplexScenario() throws Exception {
        String inputFile = tempDir.resolve("test_input.txt").toString();
        String outputFile = tempDir.resolve("output.txt").toString();
        createTestInputFile(inputFile,
            "15111111111111111",
            "161111111111111111",
            "1711111111111111111",
            "18111111111111111111",
            "191111111111111111111",
            "2011111111111111111111"
        );

        memberFileService.processFile(inputFile, outputFile);

        String output = Files.readString(Paths.get(outputFile));
        assertEquals(120, output.length());

        for (int i = 0; i < 6; i++) {
            String member = output.substring(i * 20, (i + 1) * 20);
            assertEquals(20, member.length());
        }
    }

    @Test
    void testInvalidFileFormat() throws Exception {
        String inputFile = tempDir.resolve("test_input.txt").toString();
        String outputFile = tempDir.resolve("output.txt").toString();
        
        try (FileOutputStream fos = new FileOutputStream(inputFile)) {
            fos.write("15123".getBytes());
        }

        assertThrows(IOException.class, () -> {
            memberFileService.processFile(inputFile, outputFile);
        });
    }

    @Test
    void testMixedLengthMemberNumbers() throws Exception {
        String inputFile = tempDir.resolve("test_input.txt").toString();
        String outputFile = tempDir.resolve("output.txt").toString();
        createTestInputFile(inputFile,
            "15ABC123456789012",
            "17XYZ12345678901234",
            "20QWERTY12345678901234"
        );

        memberFileService.processFile(inputFile, outputFile);

        String output = Files.readString(Paths.get(outputFile));
        assertEquals(60, output.length());
        
        String member1 = output.substring(0, 20);
        String member2 = output.substring(20, 40);
        String member3 = output.substring(40, 60);
        
        assertTrue(member1.startsWith("ABC123456789012"));
        assertTrue(member2.startsWith("XYZ12345678901234"));
        assertTrue(member3.equals("QWERTY12345678901234"));
        
        assertEquals(5, member1.substring(15).trim().length() == 0 ? 5 : 0);
        assertEquals(3, member2.substring(17).trim().length() == 0 ? 3 : 0);
        assertEquals(0, member3.substring(20).length());
    }

    private void createTestInputFile(String filePath, String... memberData) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            for (String data : memberData) {
                String length = data.substring(0, 2);
                String memberNumber = data.substring(2);
                fos.write(length.getBytes());
                fos.write(memberNumber.getBytes());
            }
        }
    }
}
