package PackRle;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;

import static PackRle.Packer.*;

public class PackRleLauncher {

    @Option(name = "-u", usage = "Unpack File")
    private boolean unpackFlag;

    @Option(name = "-z", forbids = {"-u"}, usage = "Pack File")
    private boolean packFlag;

    @Option(name = "-out", required = false, usage = "Name of input file")
    private String outputFileName;

    @Argument(required = true, usage = "Name of output file")
    private String inputFileName;


    public static void main(String[] args) {
        new PackRleLauncher().launch(args);
    }


    private void launch(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
            if (!unpackFlag && !packFlag) throw new CmdLineException(parser, "there is no pack option");
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar Pack-Rle.jar [-z|-u] -out outputname.rle inputname.txt");
            parser.printUsage(System.err);
        }

        if (unpackFlag) System.out.println("-u flag is set");
        if (packFlag) System.out.println("-z flag is set");

        if (outputFileName.isEmpty())
            outputFileName = inputFileName.substring(0, inputFileName.length() - 4) + ".rle";

        System.out.println("InputFile:  " + inputFileName);
        System.out.println("OutputFile:  " + outputFileName);

        try {
            if (unpackFlag) unpack(inputFileName, outputFileName);
            else pack(inputFileName, outputFileName);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
