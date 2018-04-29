package PackRle;

import java.io.*;
import java.util.ArrayDeque;


public class Packer {

    private static final char endOfFile = '\uFFFF';

    public static void pack(String inputName, String outputName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(inputName)) {
                pack(inputStream, outputName);
        }
    }


    /**
     * Упаковка методом rle.
     *
     * Повторяющиеся символы переписываются в выходной файл в одном экземпляре,
     * при этом сначало записывается previousByte, указывающий на кол-во символов до сжатия.
     * Неповторяющиеся последовательности символов переписываются в выходной файл
     * без изменений, при этом сначало записывается previousByte,
     * указывающий на кол-во символов до сжатия.
     *
     * PreviousByte:
     * Для указвания последовательности из повторяющихся символов PB отрицательный.
     * Для указвания последовательности из НЕповторяющихся символов PB положительный.
     * Также, стоит учитывать что не сущесвтует последовательности из 1 и 0 символов.
     *
     * Вот  таблица соответсствия:
     * ========================================================
     * -1 -2 -3 ... -126 -127 -128  - previous byte
     *  2  3  4 ...  127  128  129  - amount of duplicate chars
     *
     *  0  1  2 ...  125  126  127  - previous byte
     *  1  2  3 ...  126  127  128  - amount of different chars
     * ========================================================
     *
     * RandomAccessFile используется для побитовой записи в файл.
     */
    public static void pack(InputStream in, String outputName) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(in)) {
            try (RandomAccessFile outputFile = new RandomAccessFile(outputName, "rw")) {


            ArrayDeque<Character> tempBuffer = new ArrayDeque<>();

                if (in.available() == 1) {
                    tempBuffer.addLast((char) reader.read());
                    writePackSequence(1, tempBuffer, outputFile);
                }

                char nextChar = (char) reader.read();

                while (nextChar != endOfFile) {
                    if (tempBuffer.isEmpty()) {
                        tempBuffer.addLast(nextChar);
                        nextChar = (char) reader.read();
                    }

                    int charsAmt = 1;

                    //129 - max length of a sequence of duplicate characters
                    while (nextChar == tempBuffer.peekLast() && charsAmt < 129) {
                        charsAmt++;
                        nextChar = (char) reader.read();
                    }

                    if (charsAmt == 1) {
                        //129: 128 is the max length of a sequence of different chars and 1 is the nextChar
                        while (nextChar != tempBuffer.peekLast() && tempBuffer.size() < 129) {
                            if (nextChar == endOfFile) break;

                            tempBuffer.addLast(nextChar);
                            nextChar = (char) reader.read();
                        }
                        //last char in tempBuffer is not from this sequence
                        charsAmt = tempBuffer.size() - ((nextChar == endOfFile)? 0 : 1);
                    }

                    writePackSequence(charsAmt, tempBuffer, outputFile);
                }
            }
        }
    }

    /**
     * Записывает в outputFile упакованную методом rle последовательность
     *
     * @param charsAmt - колличество символов в последовательности из InputFile
     * @param buffer - передается ссылка на дек (!)
     */
    private static void writePackSequence(int charsAmt, ArrayDeque<Character> buffer,
                                          RandomAccessFile outputFile) throws IOException {
            byte previousByte = (byte) (((buffer.size() == 1)? -1 : 1) * (charsAmt - 1));
            outputFile.write(previousByte);

            int charsAmtToOut = (buffer.size() == 1)? 1 : charsAmt;

            for (int i = 1; i <= charsAmtToOut; i++) {
                byte[] charInBytes = String.valueOf(buffer.pollFirst()).getBytes();
                outputFile.write(charInBytes);
            }
    }


    public static void main(String[] args) {
        try {
            pack("files/in1.txt", "files/packed.txt");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    public static void unpack(String inputName, String outputName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputName)) {
            unpack(inputName, outputStream);
        }
    }


    private static void unpack (String inputName, OutputStream out) throws IOException {
        try (RandomAccessFile inputFile = new RandomAccessFile(inputName, "rw")) {
            try (OutputStreamWriter writer = new OutputStreamWriter(out)) {

                while (true) {
                    byte previousByte;
                    try {
                        previousByte = inputFile.readByte();
                    } catch (EOFException e) {
                        return;
                    }

                    if (previousByte < 0) {
                        String charToOut = readOneChar(inputFile);
                        int charAmt = -1 * previousByte + 1;
                        for (int i = 1; i <= charAmt; i++) {
                            writer.write(charToOut);
                        }
                    }
                    else {
                        int charAmt = previousByte + 1;
                        for (int i = 1; i <= charAmt; i++) {
                            String charToOut = readOneChar(inputFile);
                            writer.write(charToOut);
                        }
                    }
                }

            }
        }
    }


    /**
     * Читает 1 2 или 3 байта из входного файла, и преобразует их в один символ
     *
     * @param inputFile - RandomAccessFile, файл, с которого читается 1 символ (1|2|3 байта)
     * @return Возвращается String, всегда из одного символа
     */
    private static String readOneChar(RandomAccessFile inputFile) throws IOException {
        byte firstCharByte = inputFile.readByte();
        int byteAmtInChar = (firstCharByte >= 0) ? 1 : (firstCharByte < -32) ? 2 : 3;

        byte[] charInBytes = new byte[byteAmtInChar];
        charInBytes[0] = firstCharByte;
        for (int i = 1; i < byteAmtInChar; i++) {
            charInBytes[i] = inputFile.readByte();
        }

        return new String(charInBytes);
    }
}
