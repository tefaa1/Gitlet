package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

import static gitlet.Utils.*;

public class RepoUtils {
    private static final File CWD = new File(System.getProperty("user.dir"));
    private static final File GITLET_DIR = join(CWD, ".gitlet");

    public static void makeNewFile(File newFile) {
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void createPathIfNotExists(String pathString) {
        // Create a Path object from the provided string
        Path path = Paths.get(pathString);

        // Get the parent directory of the file, ignoring the last part (file name)
        Path parentDir = path.getParent();

        try {
            // Check if the parent directory exists
            if (parentDir != null && Files.notExists(parentDir)) {
                // Create the directories if they do not exist
                Files.createDirectories(parentDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkRepo(File check) {
        if (!check.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    public static String H(File x) {
        return sha1(serialize(x)) + sha1(readContentsAsString(x));
    }

    public static String getSpecificPath(String fullPath, File gitLet) {
        Path basePath = gitLet.getParentFile().toPath();
        Path filePath = Paths.get(fullPath);
        Path relativePath = basePath.relativize(filePath);
        return relativePath.toString().replace("\\", "/");
        // Replace backslashes with forward slashes
    }

    // recursion method to get all files
    public static void getFiles(HashMap<String, String> allFiles, File W, File gitLet) {
        File C = join(GITLET_DIR, "allFiles");
        HashMap<String, String> curFiles = readObject(C, HashMap.class);
        if (W.isDirectory()) {
            if (!curFiles.containsKey(W.getName())) {
                File[] files = W.listFiles();
                for (File it : files) {
                    getFiles(allFiles, it, gitLet);
                }
            }
        } else {
            if (!curFiles.containsKey(W.getName())) {
                allFiles.put(getSpecificPath(W.getAbsolutePath(), gitLet), H(W));
            }
        }
    }

    public static void deleteFiles(File W) {
        File C = join(GITLET_DIR, "allFiles");
        HashMap<String, String> curFiles = readObject(C, HashMap.class);
        if (W.isDirectory()) {
            if (!curFiles.containsKey(W.getName())) {
                File[] files = W.listFiles();
                for (File it : files) {
                    deleteFiles(it);
                }
                W.delete();
            }
        } else {
            if (!curFiles.containsKey(W.getName())) {
                W.delete();
            }
        }
    }

    public static String splitPoint(HashSet<String> curSet
            , HashSet<String> givSet, Commit curCom, Commit givCom) {
        while (curCom != null || givCom != null) {
            if (curCom != null) {
                String id = curCom.getId();
                if (givSet.contains(id)) {
                    return id;
                }
                curSet.add(id);
                curCom = curCom.getParent();
            }
            if (givCom != null) {
                String id = givCom.getId();
                if (curSet.contains(id)) {
                    return id;
                }
                givSet.add(id);
                givCom = givCom.getParent();
            }
        }
        return null;
    }
}
