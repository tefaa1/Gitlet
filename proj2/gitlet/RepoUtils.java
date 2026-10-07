package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
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

    // the blob hash depends only on the file contents, like in real git
    public static String H(File x) {
        return sha1(readContents(x));
    }

    public static String getSpecificPath(String fullPath, File gitLet) {
        Path basePath = gitLet.getParentFile().toPath();
        Path filePath = Paths.get(fullPath);
        Path relativePath = basePath.relativize(filePath);
        return relativePath.toString().replace("\\", "/");
        // Replace backslashes with forward slashes
    }

    // turns a path typed by the user (like "./src\A.java") into the
    // repository key used everywhere else (like "src/A.java")
    public static String toRepoPath(String name) {
        Path full = join(CWD, name).toPath().normalize();
        return getSpecificPath(full.toString(), GITLET_DIR);
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

    // deletes a file from the working directory, then any folders it leaves empty
    public static void deleteWorkingFile(String path) {
        File file = join(CWD, path);
        file.delete();
        File dir = file.getParentFile();
        while (dir != null && !dir.equals(CWD) && dir.isDirectory()) {
            String[] left = dir.list();
            if (left == null || left.length != 0) {
                break;
            }
            dir.delete();
            dir = dir.getParentFile();
        }
    }

    // the latest common ancestor of the two commits, following both parents
    // of merge commits: the closest ancestor of CURCOM that GIVCOM also has
    public static String splitPoint(Commit curCom, Commit givCom) {
        HashSet<String> givSet = new HashSet<>();
        ArrayDeque<Commit> stack = new ArrayDeque<>();
        stack.push(givCom);
        while (!stack.isEmpty()) {
            Commit com = stack.pop();
            if (givSet.add(com.getId())) {
                if (com.getParent() != null) {
                    stack.push(com.getParent());
                }
                if (com.getSecParent() != null) {
                    stack.push(com.getSecParent());
                }
            }
        }
        HashSet<String> curSet = new HashSet<>();
        ArrayDeque<Commit> queue = new ArrayDeque<>();
        queue.add(curCom);
        while (!queue.isEmpty()) {
            Commit com = queue.poll();
            if (givSet.contains(com.getId())) {
                return com.getId();
            }
            if (curSet.add(com.getId())) {
                if (com.getParent() != null) {
                    queue.add(com.getParent());
                }
                if (com.getSecParent() != null) {
                    queue.add(com.getSecParent());
                }
            }
        }
        return null;
    }
}
