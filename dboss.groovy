#!/usr/bin/env groovy

//https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-cli-commons
@Grapes([
        @Grab(group = 'org.codehaus.groovy', module = 'groovy-cli-commons', version = '3.0.14'),
        @Grab(group = 'org.eclipse.jgit', module = 'org.eclipse.jgit.ssh.jsch', version = '6.4.0.202211300538-r')
])


import groovy.cli.commons.CliBuilder
import groovy.json.JsonSlurper
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.CheckoutCommand
import org.eclipse.jgit.api.errors.GitAPIException


class Dboss {

    static void main(String[] args) {

        def verbose = args.find({ it == "-y" })?.isEmpty() ? false : true

        Util.printMessage("STEP 1 - " + CodeMessage.VALIDATING_ARGUMENT_OPTIONS.message(), verbose)

        def options = new Options().getOptions(args)

        def propertiesFile = this.getLocation().toString().replace("file:", "").replace("groovy", "properties.json")
        Validation.validatePropertiesFile(propertiesFile)

        def ret = new WorkFlow().execute(options, propertiesFile)

        Util.printMessage(CodeMessage.geMessageByValue(ret), verbose)

    }
}

class WorkFlow {

    static int execute(HashMap options, String propertiesFile) {

        def localGitDirectory = Util.getJsonObject(propertiesFile, "config")["local_git_directory"]

        def verbose = options.get("verbose") == "y"

        Util.printMessage("STEP 2 - " + CodeMessage.VALIDATING_GIT_DIRECTORY.message() + ": " + localGitDirectory, verbose)

        def ret = Validation.validateGitDirectory(localGitDirectory)

        Util.printMessage("STEP 3 - " + CodeMessage.GETTING_DATABASE_CONNECTIONS_URL.message() + ": " + propertiesFile, verbose)

        def dataBaseConnectionsUrl = DataBase.getDataBaseConnectionsURL(propertiesFile)

        Util.printMessage("STEP 3.1 - " + CodeMessage.GETTING_GIT_REPOSITORIES.message() + ": " + propertiesFile, verbose)

        def gitRepository = Util.getJsonObject(propertiesFile, "git_repositories")[options.get("git_repository")]
        def gitUser = options.get("git_user")
        def gitPassword = options.get("git_password")

        Util.printMessage("STEP 4 - " + CodeMessage.CLONING_GIT_REPOSITORY.message() + ": " + gitRepository, verbose)

        def localGitRepositoryDirectory = localGitDirectory + "/" + options.get("git_repository").toString().replace(".git", "")

        if (!Util.isDirectory(localGitRepositoryDirectory)) {

            this.exit(Git.cloneGitRepository(localGitDirectory, gitRepository, gitUser, gitPassword), localGitDirectory + " " + gitRepository)

            Util.printMessage(CodeMessage.GIT_REPOSITORY_CLONED_SUCCESSFULLY.message() + ": " + gitRepository, verbose)

        } else {

            Util.printMessage("STEP 4.1 - " + CodeMessage.PULLING_GIT_REPOSITORY.message() + ": " + gitRepository, verbose)

            this.exit(Git.pullRepository(localGitRepositoryDirectory), gitRepository)

            Util.printMessage(CodeMessage.PULL_WAS_SUCCESSFUL.message(), verbose)
        }

        Util.printMessage("STEP 5 - " + CodeMessage.SEARCH_FOR_FILES_THAT_WILL_BE_EXECUTED.message() + ": " + gitRepository, verbose)

        def gitBranch = options.get("git_branch")

        Util.printMessage("STEP 5.1 - " + CodeMessage.VALIDATE_IF_BRANCH_EXIST.message() + ": " + gitBranch, verbose)

        def fullGitBranchName = Validation.validateIfBranchExist(localGitRepositoryDirectory, gitBranch)

        Util.printMessage("STEP 5.2 - " + CodeMessage.CHECKOUT_BRANCH.message() + ": " + gitBranch, verbose)

        ret = Git.gitCheckoutBranch(localGitRepositoryDirectory, fullGitBranchName)

        return ret

    }

    static exit(int ret, String messageDetails) {
        if (ret != CodeMessage.SUCCESS.value()) {
            println(CodeMessage.geMessageByValue(ret) + ": " + messageDetails)
            System.exit(ret)
        }
    }

}

class Validation {

    static int validatePropertiesFile(String propertiesFile) {

        if (!Util.isFile(propertiesFile)) {
            WorkFlow.exit(CodeMessage.PROPERTIES_FILE_DOES_NOT_EXIST.value(), propertiesFile)
        }

    }

    static int validateGitDirectory(String directory) {

        if (Util.isDirectory(directory)) {

            return CodeMessage.SUCCESS.value()
        }

        WorkFlow.exit(CodeMessage.GIT_DIRECTORY_DOES_NOT_EXIST.value(), directory)
    }

    static String validateIfBranchExist(String localGitRepoDir, String gitBranch) {

        def branchList = Git.getGitBranches(localGitRepoDir, gitBranch)

        def fullGitBranchName = branchList.find({
            def nameSplit = it.split("/")
            def size = nameSplit.size() -1
            nameSplit[size] == gitBranch })

        if (!fullGitBranchName) {
            WorkFlow.exit(CodeMessage.BRANCH_DOES_NOT_EXIST.value(), gitBranch)
        }

        return fullGitBranchName.replace("refs/", "")

    }
}

enum CodeMessage {

    SUCCESS(0, 'Successful execution'),
    FAIL(1, 'Execution failed'),
    GIT_DIRECTORY_DOES_NOT_EXIST(2, 'Git directory does not exist'),
    CREATING_DIRECTORY(3, 'Creating directory.'),
    DIRECTORY_CREATED(4, 'Directory created.'),
    CREATING_DIRECTORY_FAILED(5, 'Creating directory failed.'),
    VALIDATING_GIT_DIRECTORY(6, 'Validating if Git directory exist.'),
    GETTING_DATABASE_CONNECTIONS_URL(6, 'Getting database connections url.'),
    GETTING_GIT_REPOSITORIES(7, 'Getting git repositories.'),
    CLONING_GIT_REPOSITORY(8, 'Cloning git repository.'),
    MISSING_REQUIRED_OPTIONS(9, 'Missing required options.'),
    CLONING_GIT_REPOSITORY_FAILED(10, 'Cloning git repository failed.'),
    GIT_REPOSITORY_CLONED_SUCCESSFULLY(11, 'Git repository cloned successfully.'),
    GIT_DIRECTORY_EXIST(12, 'Git directory exist'),
    PULLING_GIT_REPOSITORY(13, 'Pulling git repository'),
    PULL_WAS_SUCCESSFUL(14, 'The pull was successful!'),
    PULL_WAS_NOT_SUCCESSFUL(15, 'The pull was not successful.'),
    PROPERTIES_FILE_DOES_NOT_EXIST(16, 'Properties file does not exist.'),
    BRANCH_DOES_NOT_EXIST(17, 'Branch does not exist.'),
    CHECKOUT_BRANCH_FAILED(18, 'Checkout branch failed.'),
    SEARCH_FOR_FILES_THAT_WILL_BE_EXECUTED(19, 'Search for files that will be executed.'),
    VALIDATE_IF_BRANCH_EXIST(20, 'Validate if branch exist.'),
    CHECKOUT_BRANCH(21, 'Checkout branch.'),
    VALIDATING_ARGUMENT_OPTIONS(22, 'Validating input argument options')

    CodeMessage(int value, String message) {
        this.value = value
        this.message = message
    }

    private final int value
    private final String message

    def value() { return value }

    def message() { return message }

    static def geMessageByValue(int value) {
        for (CodeMessage e : values()) {
            if (e.value == value) {
                return e.message()
            }
        }
        return CodeMessage.FAIL.message()
    }

}

class Options {

    def getOptions(String[] args) {

        def cli = new CliBuilder(usage: 'dboss.groovy -u= -p= -b= -e= -d= -s= -o= -r= -i= [-v]')

        cli.with {
            g longOpt: 'git_repository', args: 1, argName: 'git_repository', required: true, 'Git repository. Ex. gsim-banco-git'
            u longOpt: 'git_user', args: 1, argName: 'git_user', required: true, 'Git user name Ex. 80830170'
            p longOpt: 'git_password', args: 1, argName: 'git_password', required: true, 'Git password'
            b longOpt: 'git_branch', args: 1, argName: 'git_branch', required: true, 'Git branch. Ex. 2022.08.26.E2'
            e longOpt: 'database_env', args: 1, argName: 'database_env', required: true, 'Database env. Ex. sigan_dev'
            d longOpt: 'database_user', args: 1, argName: 'database_user', required: true, 'Database user. Ex. sigan_cn'
            s longOpt: 'database_password', args: 1, argName: 'database_password', required: true, 'Database password'
            o longOpt: 'operation', args: 1, argName: 'operation', required: true, 'Operation. execution or rollback'
            r longOpt: 'release', args: 1, argName: 'release', required: true, 'Release. YYYYMMDDEX (YEARMONTHDAYESTEIRAID) Ex. 2023'
            i longOpt: 'project_id', args: 1, argName: 'project_id', required: true, 'Project Id. Ex. PTI1808'
            v longOpt: 'verbose', argName: 'verbose', required: false, 'Optional - Verbose flag Ex. -v)'
        }

        def optionMap = [:]
        def options = cli.parse(args)

        if (!options) {
            System.exit(CodeMessage.MISSING_REQUIRED_OPTIONS.value())
        }

        optionMap["git_repository"] = options.g ?: options.git_repository
        optionMap["git_user"] = options.u ?: options.git_user
        optionMap["git_password"] = options.p ?: options.git_password
        optionMap["git_branch"] = options.b ?: options.git_branch
        optionMap["database_env"] = options.e ?: options.database_env
        optionMap["database_user"] = options.d ?: options.dataBaseUser
        optionMap["database_password"] = options.s ?: options.database_password
        optionMap["operation"] = options.o ?: options.operation
        optionMap["release"] = options.r ?: options.release
        optionMap["project_id"] = options.i ?: options.project_id
        optionMap["verbose"] = options.v ? "y" : "n"

        return optionMap

    }

}

class Util {

    static def isDirectory(String directory) {
        def file = new File(directory)
        return file.isDirectory()
    }

    static def isFile(String filePath) {
        def file = new File(filePath)
        return file.isFile()
    }


    static def getJsonObject(String jsonFileName, String objectName) {

        def jsonObject = new JsonSlurper().parse(new File(jsonFileName))[objectName]

        return jsonObject
    }

    static printMessage(String message, boolean verbose) {
        if (verbose) {
            println(message)
        }
    }

}

class DataBase {

    def static getDataBaseConnectionsURL(String propertiesFile) {

        def objectName = "database_connections_url"

        def dataBaseConnectionsUrl = Util.getJsonObject(propertiesFile, objectName)

        return dataBaseConnectionsUrl
    }


}

class Git {

    def static gitCheckoutBranch(String localGitRepoDir, String fullGitBranchName) {

        def git = org.eclipse.jgit.api.Git.open(new File(localGitRepoDir))

        try {
            git.checkout().setCreateBranch(false).setName(fullGitBranchName).call()
        } catch (GitAPIException e) {
            println(CodeMessage.CHECKOUT_BRANCH_FAILED.message() + ": " + fullGitBranchName)
        } finally {
            git.close()
        }

        return CodeMessage.SUCCESS.value()

    }

    def static cloneGitRepository(String localGitDirectory, String gitRepository, String gitUser, String gitPassword) {

        def encodedUser = URLEncoder.encode(gitUser, "UTF-8")
        def encodedPassword = URLEncoder.encode(gitPassword, "UTF-8")

        def uri = "https://" + encodedUser + ":" + encodedPassword + "@" + gitRepository
        def processBuilder = new ProcessBuilder("git", "clone", uri.toString())
        processBuilder.directory(new File(localGitDirectory))
        processBuilder.redirectErrorStream(true)
        def process = processBuilder.start()

        def output = new StringBuilder()
        process.inputStream.eachLine { line ->
            output.append(line).append("\n")
        }

        process.waitFor()

        int ret = process.getAt("exitcode")

        if (ret != CodeMessage.SUCCESS.value()) {
            println(CodeMessage.CLONING_GIT_REPOSITORY_FAILED.message() + ": " + uri.replaceAll(encodedPassword, "*password*"))
            println(output)
            return ret
        }

        return CodeMessage.SUCCESS.value()
    }

    def static pullRepository(String localGitRepoDir) throws GitAPIException {

        //Shoud be in master branch to execute pull command
        Git.gitCheckoutBranch(localGitRepoDir, "master")

        def git = org.eclipse.jgit.api.Git.open(new File(localGitRepoDir))

        def config = git.getRepository().getConfig();
        config.setBoolean("http", null, "sslVerify", false);
        config.save();

        def pullCmd = git.pull()
        def pullResult = pullCmd.call()
        git.close()

        if (!pullResult.successful) {
            println(CodeMessage.PULL_WAS_NOT_SUCCESSFUL.message())
            println("Fetch status: " + pullResult.fetchResult.toString())
            println("Merge status: " + pullResult.mergeResult.toString())
            println("Rebase status: " + pullResult.rebaseResult.toString())
            return CodeMessage.PULL_WAS_NOT_SUCCESSFUL.value()
        }

        return CodeMessage.SUCCESS.value()

    }

    def static getGitBranches(String localGitRepoDir, String branchName) throws GitAPIException {
        def git = org.eclipse.jgit.api.Git.open(new File(localGitRepoDir))

        def branchListCmd = git.branchList()
        def branchList = branchListCmd.setListMode(ListBranchCommand.ListMode.ALL).call()
        git.close()

        def branchNameList = new ArrayList<String>()

        branchList.each {
           branchNameList.add(it.name)
        }

        return branchNameList

    }

}
