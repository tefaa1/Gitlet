#!/usr/bin/env bash
#
# End-to-end tests for Gitlet.
#
# Compiles the sources into a temporary folder, runs every scenario in a
# fresh scratch repository, and exits with a non-zero status if any check
# fails. Works on Linux, macOS and Windows (Git Bash).
#
# Usage:  bash proj2/testing/run-tests.sh
#
set -u

HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Native Windows Java needs Windows-style paths.
native() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else echo "$1"; fi; }

CLASSES="$(native "$WORK/classes")"
if ! out="$(javac -nowarn -d "$CLASSES" "$(native "$HERE/../gitlet")"/*.java 2>&1)"; then
    echo "Compilation failed:"
    echo "$out"
    exit 1
fi

PASS=0
FAIL=0

# Runs one Gitlet command; strips Windows line endings from the output.
g() { java -cp "$CLASSES" gitlet.Main "$@" | tr -d '\r'; }

# Starts a scenario in a brand-new repository.
fresh() { cd "$WORK" && rm -rf repo && mkdir repo && cd repo && g init > /dev/null; }

# The non-empty lines of one `status` section, joined by spaces.
section() {
    g status | awk -v h="=== $1 ===" '
        $0 == h  { on = 1; next }
        /^=== /  { on = 0 }
        on && NF { printf "%s%s", sep, $0; sep = " " }'
}

files() { ls -A | grep -v '^\.gitlet$' | tr '\n' ' ' | sed 's/ $//'; }

group() { printf '\n\033[1m%s\033[0m\n' "$1"; }

check() {
    if [ "$2" == "$3" ]; then
        PASS=$((PASS + 1))
        printf '  \033[32m✓\033[0m %s\n' "$1"
    else
        FAIL=$((FAIL + 1))
        printf '  \033[31m✗ %s\033[0m\n      expected: %q\n      actual:   %q\n' "$1" "$3" "$2"
    fi
}

UNTRACKED="There is an untracked file in the way; delete it, or add and commit it first."

# ---------------------------------------------------------------------------
group "Repository and command line"

cd "$WORK" && rm -rf repo && mkdir repo && cd repo
check "commands need a repository" "$(g status)" "Not in an initialized Gitlet directory."
check "init creates .gitlet" "$(g init; ls -A)" ".gitlet"
check "init twice is refused" "$(g init)" "A Gitlet version-control system already exists in the current directory."
check "no command" "$(g)" "Please enter a command."
check "unknown command" "$(g push)" "No command with that name exists."
check "wrong number of operands" "$(g log extra)" "Incorrect operands."
check "malformed checkout" "$(g checkout a b)" "Incorrect operands."
check "initial commit is dated at the epoch" "$(g log | grep -c 1970)" "1"

# ---------------------------------------------------------------------------
group "Staging: add, rm, status"

fresh
echo a > a.txt; echo b > b.txt
check "new files are untracked" "$(section 'Untracked Files')" "a.txt b.txt"
g add b.txt; g add a.txt
check "add stages files (sorted)" "$(section 'Staged Files')" "a.txt b.txt"
check "add a missing file" "$(g add nope.txt)" "File does not exist."
g commit "two files"
check "commit clears the staging area" "$(section 'Staged Files')" ""
echo changed > a.txt
check "unstaged edit is reported" "$(section 'Modifications Not Staged For Commit')" "a.txt (modified)"
g add a.txt; echo a > a.txt; g add a.txt
check "re-adding the committed version unstages it" "$(section 'Staged Files')" ""
g rm b.txt
check "rm stages a tracked file for removal" "$(section 'Removed Files')" "b.txt"
check "rm deletes the tracked file" "$(files)" "a.txt"
echo b > b.txt; g add b.txt
check "add cancels a staged removal" "$(section 'Removed Files')" ""
echo z > z.txt; g add z.txt; g rm z.txt
check "rm of a staged-only file keeps it on disk" "$(cat z.txt)" "z"
check "rm of an untracked file" "$(g rm z.txt)" "No reason to remove the file."
rm a.txt
check "deleted tracked file is reported" "$(section 'Modifications Not Staged For Commit')" "a.txt (deleted)"
g add a.txt
check "add of a deleted tracked file stages its removal" "$(section 'Removed Files')" "a.txt"

# ---------------------------------------------------------------------------
group "Commits and history"

fresh
check "commit with nothing staged" "$(g commit 'empty')" "No changes added to the commit."
echo 1 > f.txt; g add f.txt
check "commit with an empty message" "$(g commit '')" "Please enter a commit message."
g commit "first"; echo 2 > f.txt; g add f.txt; g commit "second"
check "log lists the branch newest first" "$(g log | grep -Ev '^(commit|Date|===)|^$' | tr '\n' ' ')" "second first initial commit "
check "find prints the matching commit" "$(g find first | wc -c | tr -d ' ')" "41"
check "find with no match" "$(g find nothing)" "Found no commit with that message."
check "global-log shows every commit" "$(g global-log | grep -c '^commit ')" "3"

# ---------------------------------------------------------------------------
group "Checkout and reset"

fresh
echo v1 > f.txt; g add f.txt; g commit "v1"; echo v2 > f.txt; g add f.txt; g commit "v2"
ID="$(g find v1)"
echo junk > f.txt; g checkout -- f.txt
check "checkout -- restores the head version" "$(cat f.txt)" "v2"
g checkout "${ID:0:8}" -- f.txt
check "checkout <short id> -- restores an older version" "$(cat f.txt)" "v1"
check "the restored version is not staged" "$(section 'Staged Files')|$(section 'Modifications Not Staged For Commit')" "|f.txt (modified)"
check "checkout of a file the commit lacks" "$(g checkout -- nope.txt)" "File does not exist in that commit."
check "checkout with an unknown id" "$(g checkout ffffff -- f.txt)" "No commit with that id exists."
check "checkout of an unknown branch" "$(g checkout nope)" "No such branch exists."
check "checkout of the current branch" "$(g checkout master)" "No need to checkout the current branch."

fresh
echo a > a.txt; g add a.txt; g commit "a"; g branch other
echo b > b.txt; g add b.txt; g commit "b"
echo keep > untracked.txt; echo staged > staged.txt; g add staged.txt
g checkout other
check "checkout removes files the branch doesn't track" "$(files)" "a.txt staged.txt untracked.txt"
check "checkout leaves untracked files alone" "$(cat untracked.txt)" "keep"
check "checkout clears the staging area" "$(section 'Staged Files')" ""
echo mine > b.txt
check "checkout refuses to overwrite an untracked file" "$(g checkout master)" "$UNTRACKED"
check "the untracked file is untouched" "$(cat b.txt)" "mine"
rm b.txt; g checkout master
g reset "$(g find a | cut -c1-6)"
check "reset <short id> moves the branch" "$(g log | sed -n 4p)" "a"
check "reset removes files the commit doesn't track" "$(files)" "a.txt staged.txt untracked.txt"
check "reset with an unknown id" "$(g reset 0000000)" "No commit with that id exists."

# ---------------------------------------------------------------------------
group "Branches"

fresh
g branch zeta; g branch alpha
check "status lists branches sorted, current starred" "$(section Branches)" "alpha *master zeta"
check "branch with an existing name" "$(g branch alpha)" "A branch with that name already exists."
check "rm-branch of the current branch" "$(g rm-branch master)" "Cannot remove the current branch."
check "rm-branch of an unknown branch" "$(g rm-branch nope)" "A branch with that name does not exist."
g rm-branch zeta
check "rm-branch deletes the pointer" "$(section Branches)" "alpha *master"

# ---------------------------------------------------------------------------
group "Merge"

fresh
echo base > m.txt; echo base > o.txt; echo base > d.txt; echo base > same.txt; echo base > gone.txt
for f in m.txt o.txt d.txt same.txt gone.txt; do g add "$f"; done; g commit "base"
g branch other
echo master > m.txt; g add m.txt; echo new > new-on-master.txt; g add new-on-master.txt
echo same > same.txt; g add same.txt; g rm gone.txt; g commit "master work"
g checkout other
echo other > o.txt; g add o.txt; g rm d.txt; echo new > new-on-other.txt; g add new-on-other.txt
echo same > same.txt; g add same.txt; g commit "other work"
g checkout master
check "clean merge prints nothing" "$(g merge other)" ""
check "keeps a change made only on the current branch" "$(cat m.txt)" "master"
check "takes a change made only on the given branch" "$(cat o.txt)" "other"
check "keeps a change made the same way on both sides" "$(cat same.txt)" "same"
check "applies additions and removals from both sides" "$(files)" "m.txt new-on-master.txt new-on-other.txt o.txt same.txt"
check "creates a merge commit" "$(g log | sed -n 5p)" "Merged other into master."
check "log shows both parents" "$(g log | sed -n 3p | cut -c1-7)" "Merge: "
check "the merge commit stores the merged snapshot" "$(section 'Modifications Not Staged For Commit')" ""
g checkout other; g checkout master
check "switching away and back restores the merged files" "$(files)" "m.txt new-on-master.txt new-on-other.txt o.txt same.txt"
check "merging an ancestor" "$(g merge other)" "Given branch is an ancestor of the current branch."
check "merging a branch with itself" "$(g merge master)" "Cannot merge a branch with itself."
check "merging an unknown branch" "$(g merge nope)" "A branch with that name does not exist."

g checkout other; echo again > o.txt; g add o.txt; g commit "other again"; g checkout master
check "split point follows both parents (no false conflict)" "$(g merge other)" ""
check "second merge takes the new change" "$(cat o.txt)" "again"

fresh
echo base > f.txt; echo keep > k.txt; g add f.txt; g add k.txt; g commit "base"
g branch other
echo "master version" > f.txt; echo "master edit" > k.txt; g add f.txt; g add k.txt; g commit "master"
g checkout other
echo "other version" > f.txt; g add f.txt; g rm k.txt; g commit "other"
g checkout master
check "conflicting merge reports it" "$(g merge other)" "Encountered a merge conflict."
check "conflict markers hold both versions" "$(cat f.txt)" "$(printf '<<<<<<< HEAD\nmaster version\n=======\nother version\n>>>>>>>')"
check "modify/delete conflict" "$(cat k.txt)" "$(printf '<<<<<<< HEAD\nmaster edit\n=======\n>>>>>>>')"
check "the conflicted result is committed" "$(section 'Modifications Not Staged For Commit')" ""

fresh
echo a > a.txt; g add a.txt; g commit "a"
g branch dev; g checkout dev; echo b > b.txt; g add b.txt; g commit "b"; g checkout master
check "fast-forward" "$(g merge dev)" "Current branch fast-forwarded."
check "fast-forward keeps the current branch checked out" "$(section Branches)" "dev *master"
check "fast-forward brings the files" "$(cat b.txt)" "b"

fresh
echo a > a.txt; g add a.txt; g commit "a"; g branch other
echo m > m.txt; g add m.txt; g commit "m"
g checkout other; echo x > x.txt; echo a2 > a.txt; g add x.txt; g add a.txt; g commit "x"; g checkout master
echo s > s.txt; g add s.txt
check "merge with staged changes" "$(g merge other)" "You have uncommitted changes."
g rm s.txt; rm -f s.txt
echo mine > x.txt
check "merge refuses to overwrite an untracked file" "$(g merge other)" "$UNTRACKED"
rm x.txt; echo dirty > a.txt
check "merge refuses to overwrite unstaged edits" "$(g merge other)" "You have uncommitted changes."
check "the unstaged edit is untouched" "$(cat a.txt)" "dirty"
echo a > a.txt; echo note > notes.txt
check "unrelated untracked files don't block a merge" "$(g merge other)" ""
check "and survive it" "$(cat notes.txt)" "note"

# ---------------------------------------------------------------------------
group "Subdirectories and paths"

fresh
mkdir -p src/deep; echo x > src/deep/A.java
check "files in subdirectories are found" "$(section 'Untracked Files')" "src/deep/A.java"
g add src/deep/A.java; g commit "nested"
check "rm works inside subdirectories" "$(g rm src/deep/A.java)" ""
check "folders left empty are removed" "$(files)" ""
g checkout -- src/deep/A.java
check "checkout recreates missing folders" "$(cat src/deep/A.java)" "x"
check "and unstages the removal" "$(section 'Removed Files')" ""
echo y > src/deep/A.java; g add ./src/deep/../deep/A.java
check "paths are normalized" "$(section 'Staged Files')" "src/deep/A.java"
echo s > "my notes.txt"; g add "my notes.txt"
check "file names with spaces" "$(section 'Staged Files')" "my notes.txt src/deep/A.java"
g rm "my notes.txt" > /dev/null; rm "my notes.txt"
if [[ "$OSTYPE" == msys* || "$OSTYPE" == cygwin* ]]; then
    g rm src/deep/A.java > /dev/null; mkdir -p src/deep; echo y > src/deep/A.java; g add 'src\deep\A.java'
    check "backslash paths work on Windows" "$(section 'Staged Files')" "src/deep/A.java"
fi

fresh
echo a > a.txt; g add a.txt; g commit "a"
cd "$WORK" && rm -rf moved && mv repo moved && cd moved
check "a moved repository stays clean" "$(section 'Modifications Not Staged For Commit')" ""

# ---------------------------------------------------------------------------
printf '\n\033[1m%d passed, %d failed\033[0m\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
