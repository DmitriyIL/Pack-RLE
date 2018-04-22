import PackRle.Packer;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.io.*;


public class PackRleTest {
    /**
     * В expectedContent записывается сначало previousByte,
     * указывающий на колличество символов в последовательности,
     * вот  таблица соответсствия:
     *
     * ========================================================
     * -1 -2 -3 ... -126 -127 -128  - previous byte
     *  2  3  4 ...  127  128  129  - amount of duplicate chars
     *
     *  0  1  2 ...  125  126  127  - previous byte
     *  1  2  3 ...  126  127  128  - amount of different chars
     * ========================================================
     *
     * затем сами символ(-ы) последовательности
     */
    @Test
    public void Pack(){
        Packer packer = new Packer();
        try {
            packer.pack("files/in1.txt", "files/packed.txt");
            String expectedContent = toChar(-3) + "a" + toChar(4) + "bcdef";
            assertFileContent("files/packed.txt", expectedContent);
            FileUtils.write(new File("files/packed.txt"), "");

            packer.pack("files/in2.txt", "files/packed.txt");
            expectedContent = toChar(0) + "a" + toChar(-2) + "b" + toChar(0) + "a";
            assertFileContent("files/packed.txt", expectedContent);
            FileUtils.write(new File("files/packed.txt"), "");

            packer.pack("files/in3.txt", "files/packed.txt");
            expectedContent = toChar(-128) + "a" + toChar(0) + "a";
            assertFileContent("files/packed.txt", expectedContent);
            FileUtils.write(new File("files/packed.txt"), "");

            StringBuilder line = new StringBuilder("");
            for (int i = 1; i <= 64; i++) line.append("ab");
            packer.pack("files/in4.txt", "files/packed.txt");
            expectedContent = toChar(127) + line.toString() + toChar(1) + "ab";
            assertFileContent("files/packed.txt", expectedContent);
            FileUtils.write(new File("files/packed.txt"), "");

            packer.pack("files/in5.txt", "files/packed.txt");
            expectedContent = toChar(0) + "a";
            assertFileContent("files/packed.txt", expectedContent);
            FileUtils.write(new File("files/packed.txt"), "");

            packer.pack("files/in6.txt", "files/packed.txt");
            expectedContent = toChar(-4) + toCharArray("ф") + toChar(5) + toCharArray("гdшлfн");
            assertFileContent("files/packed.txt", expectedContent);
            FileUtils.write(new File("files/packed.txt"), "");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    public void Unpack(){
        Packer packer = new Packer();
        try {
            for (int i = 1; i <= 6; i++) {
                String inputFileName = "files/in" + i + ".txt";

                packer.pack(inputFileName, "files/packed.txt");
                packer.unpack("files/packed.txt", "files/unpacked.txt");

                RandomAccessFile inputFile = new RandomAccessFile(inputFileName, "rw");
                RandomAccessFile unpackedFile = new RandomAccessFile("files/unpacked.txt", "rw");

                String expectedContent = inputFile.readLine();
                String actualContent = unpackedFile.readLine();

                assertEquals(expectedContent, actualContent);

                FileUtils.write(new File("files/packed.txt"), "");
                FileUtils.write(new File("files/unpacked.txt"), "");
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void assertFileContent(String name, String expectedContent) throws IOException {
        RandomAccessFile reader = new RandomAccessFile(name, "rw");
        String actualContent = reader.readLine();
        assertEquals(expectedContent, actualContent);
        reader.close();
    }

    /**
     * Если previousByte < 0, то RandomAccessFile считывает его в обратном машинном коде,
     * поэтому при записи в expectedContent мы все острицательные байты представляем в
     * обратном машинном коде, путем прибавления к ним 256.
     */
    private char toChar(int previousByte) {
        return (previousByte < 0) ? (char) (256 + previousByte) : (char) previousByte;
    }

    /**
     * Есть символы, которые хранятся двумя или тремя байтами.
     * Эта функция делает из первоначального символа 2 или 3 символа,
     * соответствующих каждому байту первоначального.
     * Таким образом получается удобное предстваление строки expectedContent для сравнения
     * c информацией в файле, т.к. RandomAccessFile считывает файл по байтам,
     * не выделяя символы самостоятельно.
     */
    private String toCharArray(String str) {
        byte[] bytes = str.getBytes();
        StringBuilder bytesInChars = new StringBuilder(bytes.length);
        for (byte aByte : bytes) {
            bytesInChars.append(toChar(aByte));
        }
        return  bytesInChars.toString();
    }
}
