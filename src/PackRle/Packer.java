package PackRle;

import java.io.*;
import java.util.ArrayDeque;


public class Packer {

    public int pack(String inputName, String outputName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(inputName)) {
                return pack(inputStream, outputName);
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
    public int pack(InputStream in, String outputName) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(in)) {
            try (RandomAccessFile writer = new RandomAccessFile(outputName, "rw")) {

                if (in.available() == 1) {
                    char c = (char) reader.read();
                    writer.write((byte) 0);
                    writer.write(c);
                }

                char nextChar = (char) reader.read();
                ArrayDeque<Character> tempBuffer = new ArrayDeque<>();

                while (nextChar != '\uFFFF') {
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
                        //129: 128 - max length of a sequence of duplicate chars and 1 the nextChar
                        while (nextChar != tempBuffer.peekLast() && tempBuffer.size() < 129) {
                            if (nextChar == '\uFFFF') break;

                            tempBuffer.addLast(nextChar);
                            nextChar = (char) reader.read();
                        }
                        //last char in tempBuffer is not from this sequence
                        charsAmt = tempBuffer.size() - ((nextChar == '\uFFFF')? 0 : 1);
                    }

                    //write to file
                    byte previousByte = (byte) (((tempBuffer.size() == 1)? -1 : 1) * (charsAmt - 1));
                    writer.write(previousByte);

                    int charsAmtToOut = (tempBuffer.size() == 1)? 1 : charsAmt;

                    for (int i = 1; i <= charsAmtToOut; i++) {
                        byte[] charInBytes = String.valueOf(tempBuffer.pollFirst()).getBytes();
                        writer.write(charInBytes);
                    }
                }
            }
        }
        return -1;
    }


    public int unpack(String inputName, String outputName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputName)) {
            return unpack(inputName, outputStream);
        }
    }


    public int unpack (String inputName, OutputStream out) throws IOException {
        try (RandomAccessFile reader = new RandomAccessFile(inputName, "rw")) {
            try (OutputStreamWriter writer = new OutputStreamWriter(out)) {

                while (true) {
                    byte previousByte;
                    try {
                        previousByte = reader.readByte();
                    } catch (IOException e) {
                        return -1;
                    }

                    if (previousByte < 0) {
                        byte firstCharByte = reader.readByte();

                        int byteAmtInChar = (firstCharByte >= 0) ? 1 : (firstCharByte < -32) ? 2 : 3;
                        byte[] charInBytes = new byte[byteAmtInChar];

                        charInBytes[0] = firstCharByte;
                        for (int i = 1; i < byteAmtInChar; i++) {
                            charInBytes[i] = reader.readByte();
                        }

                        String charToOut = new String(charInBytes);

                        int charAmt = -1 * previousByte + 1;
                        for (int i = 1; i <= charAmt; i++) {
                            writer.write(charToOut);
                        }
                    }
                    else {
                        int charAmt = previousByte + 1;
                        for (int i = 1; i <= charAmt; i++) {
                            byte firstCharByte = reader.readByte();

                            int byteAmt = (firstCharByte >= 0) ? 1 : (firstCharByte < -32) ? 2 : 3;
                            byte[] charInBytes = new byte[byteAmt];

                            charInBytes[0] = firstCharByte;
                            for (int j = 1; j < byteAmt; j++) {
                                charInBytes[j] = reader.readByte();
                            }

                            String charToOut = new String(charInBytes);
                            writer.write(charToOut);
                        }
                    }
                }

            }
        }
    }
}
