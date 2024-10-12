package gitlet;

import java.io.*;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.RepoUtils.*;


/** Represents a gitlet repository.
 *  have all methods that represent the commands of gitlet
 *
 *  does at a high level.
 *
 *  @author mohamed abdellatif
 */
public class Repository implements Serializable {
    /**
     *
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

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
     * The staged file will contain a map where the key is the name of the file
     * ,and the value is the hash code of the file.
     */
    private static File blobsMap = join(GITLET_DIR, "blobsMap");
    private static File stagedForAddFiles = join(GITLET_DIR, "staged add files");
    private static File stagedForRemoveFiles = join(GITLET_DIR, "staged remove files");
    private static File allFiles = join(GITLET_DIR, "allFiles");

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
        File fileByUser = join(CWD, addedFile);
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRem = readObject(stagedForRemoveFiles, HashMap.class);
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        if (!fileByUser.exists()) {
            if (checkBlobs.containsKey(addedFile)) {
                stagedRem.put(addedFile, checkBlobs.get(addedFile));
                stagedAdd.remove(addedFile);
                checkBlobs.remove(addedFile);
                writeObject(blobsMap, checkBlobs);
                writeObject(stagedForRemoveFiles, stagedRem);
                writeObject(stagedForAddFiles, stagedAdd);
            } else {
                System.out.println("File does not exist.");
            }
        } else {
            String newHash = H(fileByUser);
            if (stagedAdd.containsKey(addedFile)) { // no commit
                String oldHash = stagedAdd.get(addedFile);
                if (!oldHash.equals(newHash)) {
                    File blob = join(blobs, oldHash);
                    blob.delete();
                    File newBlob = join(blobs, newHash);
                    writeContents(newBlob, readContents(fileByUser));
                    stagedAdd.replace(addedFile, newHash);
                    checkBlobs.replace(addedFile, newHash);
                    writeObject(blobsMap, checkBlobs);
                    writeObject(stagedForAddFiles, stagedAdd);
                }
            } else {
                // you may just make a commit
                if (checkBlobs.containsKey(addedFile)) {
                    if (!checkBlobs.get(addedFile).equals(newHash)) {
                        File newBlob = join(blobs, newHash);
                        writeContents(newBlob, readContents(fileByUser));
                        stagedAdd.put(addedFile, newHash);
                        checkBlobs.replace(addedFile, newHash);
                        writeObject(blobsMap, checkBlobs);
                        writeObject(stagedForAddFiles, stagedAdd);
                    }
                } else {
                    // you might never add this file
                    File newBlob = join(blobs, newHash);
                    writeContents(newBlob, readContents(fileByUser));
                    if (stagedRem.containsKey(addedFile)) {
                        stagedRem.remove(addedFile);
                    } else {
                        stagedAdd.put(addedFile, newHash);
                    }
                    checkBlobs.put(addedFile, newHash);
                    writeObject(blobsMap, checkBlobs);
                    writeObject(stagedForAddFiles, stagedAdd);
                    writeObject(stagedForRemoveFiles, stagedRem);
                }
            }
        }
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
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRemove = readObject(stagedForRemoveFiles, HashMap.class);
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        Branch br = readObject(join(branches, readContentsAsString(head)), Branch.class);
        Commit com = readObject(join(commits, br.getID()), Commit.class);
        HashMap<String, String> prevRefs = com.getRefs();
        File check = join(CWD, rem);
        if (!check.exists()) {
            if (checkBlobs.containsKey(rem)) {
                stagedRemove.put(rem, checkBlobs.get(rem));
                stagedAdd.remove(rem);
                checkBlobs.remove(rem);
                writeObject(blobsMap, checkBlobs);
                writeObject(stagedForRemoveFiles, stagedRemove);
                writeObject(stagedForAddFiles, stagedAdd);
            } else {
                System.out.println("File does not exist.");
            }
        } else {
            String hash = H(check);
            if (hash.equals(stagedAdd.get(rem))) {
                checkBlobs.remove(rem);
                stagedAdd.remove(rem);
                writeObject(blobsMap, checkBlobs);
                writeObject(stagedForAddFiles, stagedAdd);
            } else {
                if (hash.equals(prevRefs.get(rem))) {
                    stagedAdd.remove(rem);
                    stagedRemove.put(rem, hash);
                    checkBlobs.remove(rem);
                    File del = join(CWD, rem);
                    restrictedDelete(del);
                    writeObject(blobsMap, checkBlobs);
                    writeObject(stagedForAddFiles, stagedAdd);
                    writeObject(stagedForRemoveFiles, stagedRemove);
                } else {
                    System.out.println("No reason to remove the file.");
                }
            }
        }
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
        HashSet<String> branch = readObject(branchesSet, HashSet.class);
        String cur = readContentsAsString(head);
        HashMap<String, String> curFiles = new HashMap<>();
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        getFiles(curFiles, CWD, join(CWD, ".gitlet"));
        System.out.println("=== Branches ===");
        for (String it : branch) {
            if (it.equals(cur)) {
                System.out.print("*");
            }
            System.out.println(it);
        }
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRem = readObject(stagedForRemoveFiles, HashMap.class);
        System.out.println("\n=== Staged Files ===");
        for (String it : stagedAdd.keySet()) {
            System.out.println(it);
        }
        System.out.println("\n=== Removed Files ===");
        List<String> list = new ArrayList<>();
        stagedRem.forEach((key, value) -> {
            File test = join(key);
            if (test.exists()) {
                if (value.equals(H(test))) {
                    list.add(key);
                }
            }
        });
        for (String it : list) {
            stagedRem.remove(it);
        }
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
        curFiles.forEach((key, value) -> {
            System.out.println(key);
        });
    }

    public static void checkoutWithName(String name) {
        checkRepo(GITLET_DIR);
        String headName = readContentsAsString(head);
        Branch H = readObject(join(branches, headName), Branch.class);
        Commit com = readObject(join(commits, H.getID()), Commit.class);
        checkoutWithId(com.getId(), name);
    }

    public static void checkoutWithId(String id, String name) {
        checkRepo(GITLET_DIR);
        File fileCommit = join(commits, id);
        if (!fileCommit.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit H = readObject(fileCommit, Commit.class);
        if (H.getRefs().containsKey(name)) {
            String hash = H.getRefs().get(name);
            File oldVersion = join(blobs, hash);
            File newVersion = join(CWD, name);
            createPathIfNotExists(name);
            writeContents(newVersion, readContents(oldVersion));
            HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
            stagedAdd.remove(name);
            writeObject(stagedForAddFiles, stagedAdd);
        } else {
            System.out.println("File does not exist in that commit.");
        }
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

    private static void bringCommit(String I) {
        HashMap<String, String> curFiles = new HashMap<>();
        getFiles(curFiles, CWD, join(CWD, ".gitlet"));
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        Branch top = readObject(join(branches, readContentsAsString(head)), Branch.class);
        Commit test = readObject(join(commits, top.getID()), Commit.class);
        HashMap<String, String> curRefs = test.getRefs();
        Commit test1 = readObject(join(commits, I), Commit.class);
        HashMap<String, String> checkoutRefs = test1.getRefs();
        curFiles.forEach((key, value) -> {
            if (checkoutRefs.containsKey(key)) {
                if (!value.equals(checkBlobs.get(key))) {
                    String s = "There is an untracked file in the way;"
                            + " delete it, or add and commit it first.";
                    System.out.println(s);
                    System.exit(0);
                }
            }
        });
        deleteFiles(CWD);
        checkBlobs.clear();
        Commit com = readObject(join(commits, I), Commit.class);
        HashMap<String, String> refs = com.getRefs();
        refs.forEach((key, value) -> {
            checkBlobs.put(key, value);
            createPathIfNotExists(key);
            File dest = join(CWD, key);
            File source = join(blobs, value);
            makeNewFile(dest);
            writeContents(dest, readContents(source));
        });
        writeObject(blobsMap, checkBlobs);
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
        File file = join(commits, I);
        if (!file.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        bringCommit(I);
        Branch branch = readObject(join(branches, readContentsAsString(head)), Branch.class);
        branch.setID(I);
        writeObject(join(branches, readContentsAsString(head)), branch);
    }

    public static void merge(String name) {
        checkRepo(GITLET_DIR);
        HashMap<String, String> stagedAdd = readObject(stagedForAddFiles, HashMap.class);
        HashMap<String, String> stagedRemove = readObject(stagedForRemoveFiles, HashMap.class);
        if (!stagedAdd.isEmpty() || !stagedRemove.isEmpty()) {
            String s = "There is an untracked file in the way;"
                    + " delete it, or add and commit it first.";
            System.out.println(s);
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
        HashMap<String, String> curFiles = new HashMap<>();
        getFiles(curFiles, CWD, join(CWD, ".gitlet"));
        HashMap<String, String> checkBlobs = readObject(blobsMap, HashMap.class);
        if (!checkBlobs.equals(curFiles)) {
            String s = "There is an untracked file in the way;"
                    + " delete it, or add and commit it first.";
            System.out.println(s);
            System.exit(0);
        }
        Commit curCommit = readObject(join(commits, curBranch.getID()), Commit.class);
        Commit givCommit = readObject(join(commits, givBranch.getID()), Commit.class);
        // split Point (LCA)
        String I = splitPoint(new HashSet<>(), new HashSet<>(), curCommit, givCommit);
        if (I.equals(givCommit.getId())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (I.equals(curCommit.getId())) {
            checkoutWithBranch(name);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        HashMap<String, String> givRefs = givCommit.getRefs();
        HashMap<String, String> curRefs = curCommit.getRefs();
        Commit idCommit = readObject(join(commits, I), Commit.class);
        HashMap<String, String> idRefs = idCommit.getRefs();
        deleteFiles(CWD);
        checkBlobs.clear();
        givRefs.forEach((key, value) -> {
            if (curRefs.containsKey(key)) {
                if (value.equals(curRefs.get(key))) {
                    // 3
                    createPathIfNotExists(key);
                    File source = join(blobs, value);
                    File dest = join(key);
                    makeNewFile(dest);
                    writeContents(dest, readContents(source));
                    checkBlobs.put(key, value);
                    writeObject(blobsMap, checkBlobs);
                } else {
                    if (value.equals(idRefs.get(key))) {
                        // 2
                        createPathIfNotExists(key);
                        File source = join(blobs, value);
                        File dest = join(key);
                        makeNewFile(dest);
                        writeContents(dest, readContents(source));
                        checkBlobs.put(key, value);
                        writeObject(blobsMap, checkBlobs);
                    } else if (curRefs.get(key).equals(idRefs.get(key))) {
                        // 1
                        createPathIfNotExists(key);
                        File source = join(blobs, curRefs.get(key));
                        File dest = join(key);
                        makeNewFile(dest);
                        writeContents(dest, readContents(source));
                        checkBlobs.put(key, curRefs.get(key));
                        writeObject(blobsMap, checkBlobs);
                    } else {
                        // 8
                        String s = "<<<<<<< HEAD\n";
                        File curBlob = join(blobs, curRefs.get(key));
                        if (curBlob.exists()) {
                            s += readContents(curBlob);
                        }
                        s += "\n=======";
                        File givBlob = join(blobs, value);
                        if (givBlob.exists()) {
                            s += readContents(givBlob);
                        }
                        s += "\n>>>>>>>";
                        createPathIfNotExists(key);
                        File dest = join(key);
                        makeNewFile(dest);
                        writeContents(dest, s);
                        File newBlob = join(blobs, H(dest));
                        makeNewFile(newBlob);
                        writeContents(newBlob, s);
                        checkBlobs.put(key, H(dest));
                        writeObject(blobsMap, checkBlobs);
                        System.out.println("Encountered a merge conflict.");
                    }
                }
                curRefs.remove(key);
            } else {
                if (idRefs.containsKey(key)) {
                    if (!idRefs.get(key).equals(value)) {
                        // 8
                        String s = "<<<<<<< HEAD\n=======\n";
                        File givBlob = join(blobs, value);
                        s += readContents(givBlob);
                        s += "\n >>>>>>>";
                        createPathIfNotExists(key);
                        File dest = join(key);
                        makeNewFile(dest);
                        writeContents(dest, s);
                        File newBlob = join(blobs, H(dest));
                        makeNewFile(newBlob);
                        writeContents(newBlob, s);
                        checkBlobs.put(key, H(dest));
                        writeObject(blobsMap, checkBlobs);
                        System.out.println("Encountered a merge conflict.");
                    }
                } else {
                    // 5
                    createPathIfNotExists(key);
                    File dest = join(key);
                    makeNewFile(dest);
                    File source = join(blobs, value);
                    writeContents(dest, readContents(source));
                    checkBlobs.put(key, H(dest));
                    writeObject(blobsMap, checkBlobs);
                }
            }
        });
        curRefs.forEach((key, value) -> {
            if (idRefs.containsKey(key)) {
                if (!idRefs.get(key).equals(value)) {
                    // 8
                    String s = "<<<<<<< HEAD\n";
                    File curBlob = join(blobs, curRefs.get(key));
                    s += readContents(curBlob);
                    s += "\n=======\n>>>>>>>";
                    createPathIfNotExists(key);
                    File dest = join(key);
                    makeNewFile(dest);
                    writeContents(dest, s);
                    File newBlob = join(blobs, H(dest));
                    makeNewFile(newBlob);
                    writeContents(newBlob, s);
                    checkBlobs.put(key, H(dest));
                    writeObject(blobsMap, checkBlobs);
                    System.out.println("Encountered a merge conflict.");
                }
            } else {
                createPathIfNotExists(key);
                File source = join(blobs, value);
                File dest = join(key);
                makeNewFile(dest);
                writeContents(dest, readContents(source));
                checkBlobs.put(key, value);
                writeObject(blobsMap, checkBlobs);
                // 4
            }
        });
        String msg = "Merged " + name + " into " + readContentsAsString(head);
        String merge = curCommit.getId().substring(0, 7) + " " + givCommit.getId().substring(0, 7);
        Commit com = new Commit(msg, curCommit, givCommit, merge);
        File temp = join(branches, readContentsAsString(head));
        Branch cur = readObject(temp, Branch.class);
        String hash = sha1(serialize(com));
        com.setId(hash);
        cur.setID(hash);
        File dest = join(commits, com.getId());
        makeNewFile(dest);
        writeObject(dest, com);
        writeObject(temp, cur);
        // hahahahahahhahahahahahhaaaah finallllyyyyyyyy
    }
}
