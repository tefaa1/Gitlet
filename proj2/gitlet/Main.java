package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author mohamed abdellatif
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                if (args.length != 1) {
                    System.out.println("invalid args");
                } else {
                    Repository.init();
                }
                break;
            case "add":
                if (args.length != 2) {
                    System.out.println("invalid args");
                } else {
                    Repository.add(args[1]);
                }
                break;
            case "commit":
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                } else if (args.length > 2) {
                    System.out.println("to indicate a multiword message, put the operand in quotation marks");
                } else {
                    if(args[1].equals("")){
                        System.out.println("Please enter a commit message.");
                        System.exit(0);
                    }
                    Repository.commit(args[1]);
                }
                break;
            case "rm":
                if (args.length != 2) {
                    System.out.println("invalid args");
                } else {
                    Repository.rm(args[1]);
                }
                break;
            case "log":
                if (args.length != 1) {
                    System.out.println("invalid args");
                } else {
                    Repository.log();
                }
                break;
            case "global-log":
                if (args.length != 1) {
                    System.out.println("invalid args");
                } else {
                    Repository.globalLog();
                }
                break;
            case "find":
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                } else if (args.length > 2) {
                    System.out.println("to indicate a multiword message, put the operand in quotation marks");
                } else {
                    if(args[1].equals("")){
                        System.out.println("Please enter a commit message.");
                        System.exit(0);
                    }
                    Repository.find(args[1]);
                }
                break;
            case "status":
                if (args.length != 1) {
                    System.out.println("invalid args");
                } else {
                    Repository.status();
                }
                break;
            case "checkout":
                if (args.length == 2) {
                    Repository.checkoutWithBranch(args[1]);
                } else if (args.length == 3) {
                    if(args[1].equals("--")) {
                        Repository.checkoutWithName(args[2]);
                    }
                    else{
                        System.out.println("Incorrect operands.");
                    }
                }// head commit
                else if (args.length == 4) {
                    if (args[2].equals ("--")) {
                        Repository.checkoutWithId(args[1], args[3]);
                    }
                    else{
                        System.out.println("Incorrect operands.");
                    }
                } else {
                    System.out.println("invalid args");
                }
                break;
            case "branch":
                if (args.length == 1) System.out.println("Please enter a branch name");
                else if (args.length == 2) {
                    Repository.branch(args[1]);
                } else {
                    System.out.println("invalid args");
                }
                break;
            case "rm-branch":
                if (args.length == 1) System.out.println("Please enter a branch name");
                else if (args.length == 2) {
                    Repository.remBranch(args[1]);
                } else {
                    System.out.println("invalid args");
                }
                break;
            case "reset":
                if (args.length == 1) System.out.println("Please enter a commit ID");
                else if (args.length == 2) {
                    Repository.reset(args[1]);
                } else {
                    System.out.println("invalid args");
                }
                break;
            case "merge":
                if (args.length == 1) System.out.println("Please enter a branch name");
                else if (args.length == 2) {
                    Repository.merge(args[1]);
                } else {
                    System.out.println("invalid args");
                }
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }
}
