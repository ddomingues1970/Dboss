#!/usr/bin/env groovy

//https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-cli-commons
@Grapes([
        @Grab(group = 'org.codehaus.groovy', module = 'groovy-cli-commons', version = '3.0.14'),
        @Grab(group = 'org.eclipse.jgit', module = 'org.eclipse.jgit.ssh.jsch', version = '6.4.0.202211300538-r')
])


import groovy.cli.commons.CliBuilder
import groovy.json.JsonSlurper

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider


class Dboss {

    static void main(String[] args) {

        def options = new Options().getOptions(args)

        def ret = new WorkFlow().execute(options)

        if (options.get("verbose") == "y") {
            //TODO: Use log.info
            println(CodeMessage.geMessageByValue(ret))
        }

    }
}

class WorkFlow {

    static int execute(HashMap options) {

        def propertiesFile = System.getenv("PWD") + "/dboss.properties.json"
        def localGitDirectory = Util.getJsonObject(propertiesFile, "config")["local_git_directory"]

        def verbose = options.get("verbose") == "y"

        if (verbose) {
            println("STEP 1 - " + CodeMessage.VALIDATING_GIT_DIRECTORY.message() + ": " + localGitDirectory)
        }
        def ret = Validation.validateGitDirectory(localGitDirectory, verbose)

        if (verbose) {
            println("STEP 2 - " + CodeMessage.GETTING_DATABASE_CONNECTIONS_URL.message() + ": " + propertiesFile)
        }

        def dataBaseConnectionsUrl = DataBase.getDataBaseConnectionsURL(propertiesFile)

        if (verbose) {
            dataBaseConnectionsUrl.each { key, value ->
                println "$key : $value"
            }
        }

        if (verbose) {
            println("STEP 2.1 - " + CodeMessage.GETTING_GIT_REPOSITORIES.message() + ": " + propertiesFile)
        }

        def gitRepositories = Git.getGitRepositories(propertiesFile)

        if (verbose) {
            gitRepositories.each { key, value ->
                println "$key : $value"
            }
        }

        def repository = Util.getJsonObject(propertiesFile, "git_repositories")[options.get("git_repository")]
        def gitUser = options.get("git_user")
        def gitPassword = options.get("git_password")

        if (verbose) {
            println("STEP 3 - " + CodeMessage.CLONING_GIT_REPOSITORIES.message() + ": " + repository)
        }

        ret = Git.cloneGitRepository(localGitDirectory, repository, gitUser, gitPassword)

        //TODO: Implment other executions

        return ret

    }

}

class Validation {

    static int validateGitDirectory(String directory, boolean verbose) {

        if (Util.isDirectory(directory)) {
            return CodeMessage.SUCCESS.value()
        }

        if (verbose) {
            println(CodeMessage.GIT_DIRECTORY_DOES_NOT_EXIST.message() + ":" + directory)
            println(CodeMessage.CREATING_DIRECTORY.message() + ":" + directory)
        }

        if (Util.createDirectory(directory)) {
            if (verbose) {
                println(CodeMessage.DIRECTORY_CREATED.message() + ":" + directory)
            }
            return CodeMessage.SUCCESS.value()
        }

        println(CodeMessage.CREATING_DIRECTORY_FAILED.message() + " : " + directory)
        System.exit(CodeMessage.CREATING_DIRECTORY_FAILED.value())

        return CodeMessage.CREATING_DIRECTORY_FAILED.value()

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
    CLONING_GIT_REPOSITORIES(8, 'Cloning git repositories.'),
    MISSING_REQUIRED_OPTIONS(9, 'Missing required options.')

    CodeMessage(int value, String message) {
        this.value = value
        this.message = message
    }

    private final def value
    private final def message

    def value() { return value }

    def message() { return message }

    static def geMessageByValue(int value) {
        for (CodeMessage e : values()) {
            if (e.value == value) {
                return e.message()
            }
        }
        return null
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
            print(CodeMessage.MISSING_REQUIRED_OPTIONS.message())
            System.exit(CodeMessage.MISSING_REQUIRED_OPTIONS.value())
        }
        /*
        def REQUIRED_ARGUMENT_COUNT = 9
            if (options?.arguments()?.size() < REQUIRED_ARGUMENT_COUNT && options?.arguments()?.size() > 1) {
            print(CodeMessage.MISSING_REQUIRED_OPTIONS.message() + "Arguments count: " + options?.arguments()?.size())
            System.exit(CodeMessage.MISSING_REQUIRED_OPTIONS.value())
        }
        */

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

    static def createDirectory(String directory) {
        def file = new File(directory)
        return file.mkdir()
    }

    static def getJsonObject(String jsonFileName, String objectName) {

        def jsonObject = new JsonSlurper().parse(new File(jsonFileName))[objectName]

        return jsonObject
    }

}

class DataBase {

    def static getDataBaseConnectionsURL(String propertiesFile) {

        def objectName = "database_connections_url"

        //TODO: Validate if file exist

        def dataBaseConnectionsUrl = Util.getJsonObject(propertiesFile, objectName)

        return dataBaseConnectionsUrl
    }


}

class Git {

    def static getGitRepositories(String propertiesFile) {

        def objectName = "git_repositories"

        //TODO: Validate if file exist

        def gitRepositories = Util.getJsonObject(propertiesFile, objectName)

        return gitRepositories
    }

    def static cloneGitRepository1(String gitBaseDirectory, String repository, String branch) {

        // Create the CredentialsProvider with the given username and password
        def credentialsProvider = new UsernamePasswordCredentialsProvider("80830170", "senha")

        // Clone the repository using the Git class
        def git = org.eclipse.jgit.api.Git.cloneRepository()
                .setDirectory(new File(gitBaseDirectory))
                .setCredentialsProvider(credentialsProvider)
                .setURI(repository)
                .call()

        println("Cloned repository: " + git.getRepository().getDirectory())

        return CodeMessage.SUCCESS.value()

    }

    def static cloneGitRepository(String localGitDirectory, String repository, String gitUser, String gitPassword) {

        def encodedUser = URLEncoder.encode(gitUser, "UTF-8")
        def encodedPassword = URLEncoder.encode(gitPassword, "UTF-8")

        def uri = "https://" + encodedUser + ":" + encodedPassword + "@" + repository
        def processBuilder = new ProcessBuilder("git", "clone", uri.toString())
        processBuilder.directory(new File(localGitDirectory))
        processBuilder.redirectErrorStream(true)
        def process = processBuilder.start()

        def output = new StringBuilder()
        process.inputStream.eachLine { line ->
            output.append(line).append("\n")
        }

        process.waitFor()
        println(output)

        return CodeMessage.SUCCESS.value()

    }

}
