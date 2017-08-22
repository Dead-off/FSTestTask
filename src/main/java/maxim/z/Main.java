package maxim.z;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        if (args.length == 0 || args[0].equals("--help")) {
            System.out.println("for start program execute 'java -jar [jar_file] [absolute_path_to_fs_file_storage]'");
            return;
        }
        String pathToFile = args[0];
        Map<String, Commands> commandsMap = new HashMap<>();
        commandsMap.put("cd", Commands.CD);
        commandsMap.put("mkdir", Commands.MKDIR);
        commandsMap.put("mkfile", Commands.MKFILE);
        commandsMap.put("write", Commands.WRITE);
        commandsMap.put("read", Commands.READ);
        commandsMap.put("ls", Commands.LS);
        commandsMap.put("rm", Commands.RM);
        IFile curFile = File.rootInstance();
        printHelpMessage();
        printCurrentDirectory(curFile);
        try (IFileSystem fs = FileSystem.getFileSystem(pathToFile)) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if ("exit".equals(line)) {
                    break;
                }
                if ("--help".equals(line)) {
                    printHelpMessage();
                    continue;
                }
                String[] arg = line.split(" ");
                if (arg.length == 0) {
                    continue;
                }
                Commands command = commandsMap.get(arg[0]);
                if (command == null) {
                    continue;
                }
                if (!command.isCorrectArgsCount(arg.length)) {
                    System.out.println("incorrect args count, use --help for check commands parameters");
                    continue;
                }
                try {
                    switch (command) {
                        case CD:
                            String dir = arg[1];
                            IFile newCurFile = dir.equals("..") ? curFile.parent() : curFile.child(dir);
                            if (!fs.isDirectoryExist(newCurFile)) {
                                System.out.println(String.format("directory %s is not exist", newCurFile.getPath()));
                                break;
                            }
                            curFile = newCurFile;
                            break;
                        case LS: {
                            System.out.println(String.join(" ", fs.getFilesList(curFile)));
                            break;
                        }
                        case MKDIR:
                            fs.createDirectory(curFile, arg[1]);
                            break;
                        case MKFILE:
                            fs.createFile(curFile, arg[1]);
                            break;
                        case READ:
                            System.out.println(fs.readAsString(curFile.child(arg[1])));
                            break;
                        case WRITE:
                            fs.write(curFile.child(arg[1]), arg[2]);
                            break;
                        case RM:
                            fs.removeFile(curFile.child(arg[1]));
                            break;
                    }
                } catch (Exception e) {
                    System.out.println("command execution failed");
                    System.out.println(e.getMessage());
                }
                printCurrentDirectory(curFile);
            }
        }
    }

    private static void printHelpMessage() {
        System.out.println("commands:");
        System.out.println("cd [dir_name] - for change current directory");
        System.out.println("ls - for get files list in current directory");
        System.out.println("mkdir [dir_name] - for create new directory");
        System.out.println("mkfile [file_name] - for create new file");
        System.out.println("write [file_name] [content] - for write content to file");
        System.out.println("read [file_name] - for read file content");
        System.out.println("rm [file_name] - for remove file");
    }

    private static void printCurrentDirectory(IFile directory) {
        System.out.print(Arrays.stream(directory.parseFileNames()).reduce((s1, s2) -> s1 + "/" + s2).orElse("") + "/ >");
    }

    enum Commands {
        CD,
        MKDIR,
        MKFILE,
        LS,
        WRITE,
        READ,
        RM;

        boolean isCorrectArgsCount(int argsCount) {
            if (this == LS) {
                return argsCount == 1;
            }
            if (this == WRITE) {
                return argsCount == 3;
            }
            return argsCount == 2;
        }
    }

}
