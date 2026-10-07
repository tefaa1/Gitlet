package gitlet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.RepoUtils.*;


/** Represents a gitlet repository.
 *  have all methods that represent the commands of gitlet
 *
 *  @author mohamed abdellatif
 */
public class Repository implements Serializable {

    /**
     * The current working directory.
     */
    private static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    private static final File GITLET_DIR = join(CWD, ".gitlet");
    private static File commits = join(GITLET_DIR, "commits");
    private static File blobs = join(GITLET_DIR, "blobs");
    private static File head = join(GITLET_DIR, "head");
    private static File branches = join(GITLET_DIR, "branches");
    private static File branchesSet = join(GITLET_DIR, "branches set");
    /**
     * The index: a map from the name of every tracked file to the hash of
     * the version that the next commit will contain.
     */
    private static File blobsMap = join(GITLET_DIR, "blobsMap");
    private static File stagedForAddFiles = join(GITLET_DIR, "staged add files");
    private static File stagedForRemoveFiles = join(GITLET_DIR, "staged remove files");
    private static File allFiles = join(GITLET_DIR, "allFiles");

    private static final String UNTRACKED_IN_THE_WAY = "There is an untracked file in the way;"
            + " delete it, or add and commit it first.";

    public static void init() {
        GITLET_DIR.mkdir();
        commits.mkdir();
        blobs.mkdir();
        branches.mkdir();
        if (commits.list().length == 0) {
            Commit c = new Commit("initial commit", null, null, "-1");
            String hash = sha1(serialize(c));
            c.setId(hash);
            File com = join(commits, hash);
            makeNewFile(com);
            writeObject(com, c);
            Branch master = new Branch("master", hash, true);
            File M = join(branches, master.getName());
            makeNewFile(M);
            writeObject(M, master);
            makeNewFile(head);
            writeContents(head, master.getName());
            HashSet<String> branch = new HashSet<>();
            branch.add(master.getName());
            writeObject(branchesSet, branch);
            makeNewFile(blobsMap);
            makeNewFile(stagedForAddFiles);
            makeNewFile(stagedForRemoveFiles);
            makeNewFile(allFiles);
            HashMap<String, String> noFiles = new HashMap<>();
            noFiles.put("pom.xml", "TEFA");
            noFiles.put("Makefile", "TEFA");
            noFiles.put("gitlet-design.md", "TEFA");  // just temporary ;)
            noFiles.put("testing", "TEFA");
            noFiles.put("target", "TEFA");
            noFiles.put("gitlet", "TEFA");
            noFiles.put(".idea", "TEFA");
            noFiles.put(".gitlet", "TEFA");
            writeObject(allFiles, noFiles);
            writeObject(blobsMap, new HashMap<String, String>());
            writeObject(stagedForAddFiles, new HashMap<String, String>());
            writeObject(stagedForRemoveFiles, new HashMap<String, String>());

        } else {
            String s = "A Gitlet version-control system already exists in the current directory.";
            System.out.println(s);
            System.exit(0);
        }
    }

    public static void add(String addedFile) {
        checkRepo(GITLET_DIR);
        addedFile = toRepoPath(addedFile);
        File fileByUser = join(CWD, addedFile);
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRem = readObject(stagedForRemoveFiles, HashMap.class);
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        HashMap<String, String> headRefs = headCommit().getRefs();
        if (!fileByUser.isFile()) {
            if (checkBlobs.containsKey(addedFile)) {
                // a tracked file that was deleted from disk: stage its removal
                stagedAdd.remove(addedFile);
                checkBlobs.remove(addedFile);
                if (headRefs.containsKey(addedFile)) {
                    stagedRem.put(addedFile, headRefs.get(addedFile));
                }
                writeObject(blobsMap, checkBlobs);
                writeObject(stagedForRemoveFiles, stagedRem);
                writeObject(stagedForAddFiles, stagedAdd);
            } else {
                System.out.println("File does not exist.");
            }
            return;
        }
        String newHash = saveBlob(fileByUser);
        stagedRem.remove(addedFile);
        if (newHash.equals(headRefs.get(addedFile))) {
            // same as the current commit, so there is nothing to stage
            stagedAdd.remove(addedFile);
        } else {
            stagedAdd.put(addedFile, newHash);
        }
        checkBlobs.put(addedFile, newHash);
        writeObject(blobsMap, checkBlobs);
        writeObject(stagedForAddFiles, stagedAdd);
        writeObject(stagedForRemoveFiles, stagedRem);
    }

    public static void commit(String message) {
        checkRepo(GITLET_DIR);
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRemove = readObject(stagedForRemoveFiles, HashMap.class);
        if (stagedAdd.isEmpty() && stagedRemove.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        Branch H = readObject(join(branches, readContentsAsString(head)), Branch.class);
        Commit parent = readObject(join(commits, H.getID()), Commit.class);
        Commit com = new Commit(message, parent, null, "-1");
        com.setRefs(checkBlobs);
        String hash = sha1(serialize(com));
        com.setId(hash);
        H.setID(hash);
        File newCommit = join(commits, hash);
        makeNewFile(newCommit);
        stagedRemove.clear();
        stagedAdd.clear();
        writeObject(newCommit, com);
        writeObject(join(branches, readContentsAsString(head)), H);
        writeObject(stagedForAddFiles, stagedAdd);
        writeObject(stagedForRemoveFiles, stagedRemove);
    }

    public static void rm(String rem) {
        checkRepo(GITLET_DIR);
        rem = toRepoPath(rem);
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRemove = readObject(stagedForRemoveFiles, HashMap.class);
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        HashMap<String, String> prevRefs = headCommit().getRefs();
        boolean staged = stagedAdd.containsKey(rem);
        boolean tracked = prevRefs.containsKey(rem);
        if (!staged && !tracked) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (staged) {
            stagedAdd.remove(rem);
            checkBlobs.remove(rem);
        }
        if (tracked) {
            stagedRemove.put(rem, prevRefs.get(rem));
            checkBlobs.remove(rem);
            deleteWorkingFile(rem);
        }
        writeObject(blobsMap, checkBlobs);
        writeObject(stagedForAddFiles, stagedAdd);
        writeObject(stagedForRemoveFiles, stagedRemove);
    }

    public static void log() {
        checkRepo(GITLET_DIR);
        File H = join(branches, readContentsAsString(head));
        Branch branch = readObject(H, Branch.class);
        Commit C = readObject(join(commits, branch.getID()), Commit.class);
        while (C != null) {
            System.out.println("===\ncommit " + C.getId());
            if (!C.getMerge().equals("-1")) {
                System.out.println("Merge: " + C.getMerge());
            }
            System.out.println("Date: " + C.getTimeStamp());
            System.out.println(C.getMessage() + "\n");
            C = C.getParent();
        }
    }

    public static void globalLog() {
        checkRepo(GITLET_DIR);
        File[] files = join(commits).listFiles();
        for (File it : files) {
            Commit H = readObject(it, Commit.class);
            System.out.println("===");
            System.out.println("commit " + H.getId());
            if (!H.getMerge().equals("-1")) {
                System.out.println("Merge: " + H.getMerge());
            }
            System.out.println("Date: " + H.getTimeStamp());
            System.out.println(H.getMessage() + "\n");
        }
    }

    public static void find(String message) {
        checkRepo(GITLET_DIR);
        File[] files = join(commits).listFiles();
        boolean f = true;
        for (File it : files) {
            Commit H = readObject(it, Commit.class);
            if (H.getMessage().equals(message)) {
                f = false;
                System.out.println(H.getId());
            }
        }
        if (f) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        checkRepo(GITLET_DIR);
        TreeSet<String> branch = new TreeSet<>(readObject(branchesSet, HashSet.class));
        String cur = readContentsAsString(head);
        HashMap<String, String> curFiles = new HashMap<>();
        TreeMap<String, String> checkBlobs = new TreeMap<>(readObject(blobsMap, HashMap.class));
        getFiles(curFiles, CWD, join(CWD, ".gitlet"));
        System.out.println("=== Branches ===");
        for (String it : branch) {
            System.out.println(it.equals(cur) ? "*" + it : it);
        }
        TreeMap<String, String> stagedAdd = new TreeMap<>(readObject(stagedForAddFiles, HashMap.class));
        TreeMap<String, String> stagedRem = new TreeMap<>(readObject(stagedForRemoveFiles, HashMap.class));
        System.out.println("\n=== Staged Files ===");
        for (String it : stagedAdd.keySet()) {
            System.out.println(it);
        }
        System.out.println("\n=== Removed Files ===");
        for (String it : stagedRem.keySet()) {
            System.out.println(it);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        checkBlobs.forEach((key, value) -> {
            if (curFiles.containsKey(key)) {
                if (!value.equals(curFiles.get(key))) {
                    System.out.println(key + " (modified)");
                }
                curFiles.remove(key);
            } else {
                System.out.println(key + " (deleted)");
            }
        });
        System.out.println("\n=== Untracked Files ===");
        for (String it : new TreeSet<>(curFiles.keySet())) {
            System.out.println(it);
        }
    }

    public static void checkoutWithName(String name) {
        checkRepo(GITLET_DIR);
        checkoutWithId(headCommit().getId(), name);
    }

    public static void checkoutWithId(String id, String name) {
        checkRepo(GITLET_DIR);
        String fullId = findCommitId(id);
        if (fullId == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        name = toRepoPath(name);
        Commit H = readObject(join(commits, fullId), Commit.class);
        if (!H.getRefs().containsKey(name)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        writeWorkingFile(name, H.getRefs().get(name));
        // the restored version is not staged, so the file goes back to its committed state
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRem = readObject(stagedForRemoveFiles, HashMap.class);
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        String headHash = headCommit().getRefs().get(name);
        stagedAdd.remove(name);
        stagedRem.remove(name);
        if (headHash != null) {
            checkBlobs.put(name, headHash);
        } else {
            checkBlobs.remove(name);
        }
        writeObject(blobsMap, checkBlobs);
        writeObject(stagedForAddFiles, stagedAdd);
        writeObject(stagedForRemoveFiles, stagedRem);
    }

    public static void checkoutWithBranch(String name) {
        checkRepo(GITLET_DIR);
        HashSet<String> branchSet = readObject(branchesSet, HashSet.class);
        if (branchSet.contains(name)) {
            if (name.equals(readContentsAsString(head))) {
                System.out.println("No need to checkout the current branch.");
            } else {
                Branch branch = readObject(join(branches, name), Branch.class);
                bringCommit(branch.getID());
                writeContents(head, name);
            }
        } else {
            System.out.println("No such branch exists.");
        }
    }

    // replaces the files tracked by the head commit with the files of commit I.
    // untracked files are left alone unless I would overwrite them.
    private static void bringCommit(String I) {
        HashMap<String, String> curFiles = new HashMap<>();
        getFiles(curFiles, CWD, join(CWD, ".gitlet"));
        HashMap<String, String> curRefs = headCommit().getRefs();
        HashMap<String, String> checkoutRefs = readObject(join(commits, I), Commit.class).getRefs();
        curFiles.forEach((key, value) -> {
            if (!curRefs.containsKey(key) && checkoutRefs.containsKey(key)) {
                System.out.println(UNTRACKED_IN_THE_WAY);
                System.exit(0);
            }
        });
        curRefs.forEach((key, value) -> {
            if (!checkoutRefs.containsKey(key)) {
                deleteWorkingFile(key);
            }
        });
        checkoutRefs.forEach(Repository::writeWorkingFile);
        writeObject(blobsMap, new HashMap<>(checkoutRefs));
        writeObject(stagedForAddFiles, new HashMap<String, String>());
        writeObject(stagedForRemoveFiles, new HashMap<String, String>());
    }

    public static void branch(String name) {
        checkRepo(GITLET_DIR);
        HashSet<String> allBranches = readObject(branchesSet, HashSet.class);
        if (allBranches.contains(name)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        Branch H = readObject(join(branches, readContentsAsString(head)), Branch.class);
        Branch branch = new Branch(name, H.getID(), false);
        allBranches.add(name);
        File file = join(branches, name);
        makeNewFile(file);
        writeObject(file, branch);
        writeObject(branchesSet, allBranches);
    }

    public static void remBranch(String name) {
        checkRepo(GITLET_DIR);
        HashSet<String> allBranches = readObject(branchesSet, HashSet.class);
        if (!allBranches.contains(name)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        File branchFile = join(branches, readContentsAsString(head));
        Branch branch = readObject(branchFile, Branch.class);
        if (branch.getName().equals(name)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        allBranches.remove(name);
        File delBranch = join(branches, name);
        delBranch.delete();
        writeObject(branchesSet, allBranches);
    }

    public static void reset(String I) {
        checkRepo(GITLET_DIR);
        String fullId = findCommitId(I);
        if (fullId == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        bringCommit(fullId);
        Branch branch = readObject(join(branches, readContentsAsString(head)), Branch.class);
        branch.setID(fullId);
        writeObject(join(branches, readContentsAsString(head)), branch);
    }

    public static void merge(String name) {
        checkRepo(GITLET_DIR);
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRemove = readObject(stagedForRemoveFiles, HashMap.class);
        if (!stagedAdd.isEmpty() || !stagedRemove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        File file = join(branches, name);
        if (!file.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String S = readContentsAsString(head);
        Branch curBranch = readObject(join(branches, S), Branch.class);
        Branch givBranch = readObject(join(branches, name), Branch.class);
        if (curBranch.getName().equals(givBranch.getName())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Commit curCommit = readObject(join(commits, curBranch.getID()), Commit.class);
        Commit givCommit = readObject(join(commits, givBranch.getID()), Commit.class);
        // split Point (LCA)
        String I = splitPoint(curCommit, givCommit);
        if (I.equals(givCommit.getId())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (I.equals(curCommit.getId())) {
            // move the current branch forward to the given commit
            bringCommit(givCommit.getId());
            curBranch.setID(givCommit.getId());
            writeObject(join(branches, S), curBranch);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        HashMap<String, String> givRefs = givCommit.getRefs();
        HashMap<String, String> curRefs = curCommit.getRefs();
        HashMap<String, String> idRefs = readObject(join(commits, I), Commit.class).getRefs();
        TreeSet<String> allNames = new TreeSet<>(idRefs.keySet());
        allNames.addAll(curRefs.keySet());
        allNames.addAll(givRefs.keySet());

        // decide the merged version of every file (null means the file is removed)
        HashMap<String, String> merged = new HashMap<>();
        boolean conflict = false;
        for (String key : allNames) {
            String split = idRefs.get(key);
            String cur = curRefs.get(key);
            String giv = givRefs.get(key);
            String result;
            if (Objects.equals(cur, giv) || Objects.equals(giv, split)) {
                // same on both sides, or only the current branch changed it
                result = cur;
            } else if (Objects.equals(cur, split)) {
                // only the given branch changed it
                result = giv;
            } else {
                // both branches changed it in different ways
                result = conflictBlob(cur, giv);
                conflict = true;
            }
            if (result != null) {
                merged.put(key, result);
            }
        }
        if (merged.equals(curRefs)) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // make sure the merge won't destroy work that isn't committed
        HashMap<String, String> curFiles = new HashMap<>();
        getFiles(curFiles, CWD, join(CWD, ".gitlet"));
        for (String key : allNames) {
            if (Objects.equals(merged.get(key), curRefs.get(key)) || !curFiles.containsKey(key)) {
                continue;
            }
            if (!curRefs.containsKey(key)) {
                System.out.println(UNTRACKED_IN_THE_WAY);
                System.exit(0);
            }
            if (!curFiles.get(key).equals(curRefs.get(key))) {
                System.out.println("You have uncommitted changes.");
                System.exit(0);
            }
        }

        for (String key : allNames) {
            String result = merged.get(key);
            if (Objects.equals(result, curRefs.get(key))) {
                continue;
            }
            if (result == null) {
                deleteWorkingFile(key);
            } else {
                writeWorkingFile(key, result);
            }
        }
        String msg = "Merged " + name + " into " + S + ".";
        String merge = curCommit.getId().substring(0, 7) + " " + givCommit.getId().substring(0, 7);
        Commit com = new Commit(msg, curCommit, givCommit, merge);
        com.setRefs(merged);
        String hash = sha1(serialize(com));
        com.setId(hash);
        curBranch.setID(hash);
        File dest = join(commits, com.getId());
        makeNewFile(dest);
        writeObject(dest, com);
        writeObject(join(branches, S), curBranch);
        writeObject(blobsMap, merged);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        // hahahahahahhahahahahahhaaaah finallllyyyyyyyy
    }

    private static Commit headCommit() {
        Branch H = readObject(join(branches, readContentsAsString(head)), Branch.class);
        return readObject(join(commits, H.getID()), Commit.class);
    }

    // the full id of the only commit whose id starts with ID, or null
    private static String findCommitId(String id) {
        if (id.isEmpty()) {
            return null;
        }
        String found = null;
        for (String it : plainFilenamesIn(commits)) {
            if (it.startsWith(id)) {
                if (found != null) {
                    return null;
                }
                found = it;
            }
        }
        return found;
    }

    // stores the contents of FILE as a blob and returns its hash
    private static String saveBlob(File file) {
        byte[] contents = readContents(file);
        String hash = sha1(contents);
        File blob = join(blobs, hash);
        if (!blob.exists()) {
            writeContents(blob, contents);
        }
        return hash;
    }

    private static void writeWorkingFile(String name, String hash) {
        File dest = join(CWD, name);
        createPathIfNotExists(dest.getPath());
        writeContents(dest, readContents(join(blobs, hash)));
    }

    // writes the conflict version of a file as a blob and returns its hash
    private static String conflictBlob(String cur, String giv) {
        String s = "<<<<<<< HEAD\n";
        if (cur != null) {
            s += readContentsAsString(join(blobs, cur));
        }
        s += "=======\n";
        if (giv != null) {
            s += readContentsAsString(join(blobs, giv));
        }
        s += ">>>>>>>\n";
        byte[] contents = s.getBytes(StandardCharsets.UTF_8);
        String hash = sha1(contents);
        writeContents(join(blobs, hash), contents);
        return hash;
    }
}
