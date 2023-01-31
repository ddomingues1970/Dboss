#!/usr/bin/env groovy

//https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-cli-commons
@Grapes([
    @Grab(group='org.codehaus.groovy', module = 'groovy-cli-commons', version = '3.0.14'),
    @Grab(group='org.eclipse.jgit', module='org.eclipse.jgit', version='6.4.0.202211300538-r')
])


import groovy.cli.commons.CliBuilder
import groovy.json.JsonSlurper

class Dboss {

    static void main(String[] args) {

        def options = new Options().getOptions(args)
        def baseGitDirectory = System.getenv("PWD") + "/Git/"
        def propertiesFile = System.getenv("PWD") + "/dboss.properties"
        def ret = new WorkFlow().execute(options, baseGitDirectory, propertiesFile)

        if(options.get("verbose") == "y") {
            //TODO: Use log.info
            println(CodeMessage.geMessageByValue(ret))
        }

        System.out.println(ret)

    }
}

class WorkFlow {

    static int execute(HashMap options, String baseGitDirectory, String propertiesFile) {

        def verbose = options.get("verbose") == "y"

        if(verbose) {
            println("STEP 1 - " + CodeMessage.VALIDATING_GIT_DIRECTORY.message() + ": " + baseGitDirectory)
        }
        def ret = Validation.validateGitDirectory(baseGitDirectory)

        if(verbose) {
            println("STEP 2 - " + CodeMessage.GETTING_DATABASE_CONNECTIONS_URL.message() + ": " + propertiesFile)
        }

        def dataBaseConnectionsUrl = DataBase.getDataBaseConnectionsURL(propertiesFile)

        if(verbose) {
            dataBaseConnectionsUrl.each { key, value ->
                println "$key : $value"
            }
        }

        if(verbose) {
            println("STEP 2.1 - " + CodeMessage.GETTING_GIT_REPOSITORIES.message() + ": " + propertiesFile)
        }

        def gitRepositories = Git.getGitRepositories(propertiesFile)

        if(verbose) {
            gitRepositories.each { key, value ->
                println "$key : $value"
            }
        }

        def repository = "https://github.com/ddomingues1970/Dboss.git"

        if(verbose) {
            println("STEP 3 - " + CodeMessage.CLONING_GIT_REPOSITORIES.message() + ": " + propertiesFile)
        }

        //ret = Git.cloneGitRepository(baseGitDirectory, repository, "v1.0.0-Beta")

        //TODO: Implment other executions

        return ret

    }

}

class Validation {

    static int validateGitDirectory(String directory) {

        if (Util.isDirectory(directory)) {
            return CodeMessage.SUCCESS.value()
        }

        println(CodeMessage.GIT_DIRECTORY_DOES_NOT_EXIST.message() + ":" + directory)
        println(CodeMessage.CREATING_DIRECTORY.message() + ":" + directory)

        if (Util.createDirectory(directory)) {
            println(CodeMessage.DIRECTORY_CREATED.message() + ":" + directory)
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
    CLONING_GIT_REPOSITORIES(8, 'Cloning git repositories.')

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
            u longOpt: 'gitUser', args: 1, argName: 'gitUser', required: true, 'Git user name Ex. 80830170'
            p longOpt: 'gitPassword', args: 1, argName: 'gitPassword', required: true, 'Git password'
            b longOpt: 'gitBranch', args: 1, argName: 'gitBranch', required: true, 'Git branch. Ex. 2022.08.26.E2'
            e longOpt: 'dataBaseEnv', args: 1, argName: 'dataBaseEnv', required: true, 'Database env. Ex. sigan_dev'
            d longOpt: 'dataBaseUser', args: 1, argName: 'dataBaseUser', required: true, 'Database user. Ex. sigan_cn'
            s longOpt: 'dataBasePassword', args: 1, argName: 'dataBasePassword', required: true, 'Database password'
            o longOpt: 'operation', args: 1, argName: 'operation', required: true, 'Operation. execution or rollback'
            r longOpt: 'release', args: 1, argName: 'release', required: true, 'Release. YYYYMMDDEX (YEARMONTHDAYESTEIRAID) Ex. 2023'
            i longOpt: 'projectId', args: 1, argName: 'projectId', required: true, 'Project Id. Ex. PTI1808'
            v longOpt: 'verbose', argName: 'verbose', required: false, 'Optional - Verbose flag Ex. -v)'
        }

        def optionMap = [:]
        def options = cli.parse(args)
        if (!options) {
            return optionMap
        }

        optionMap["gitUser"] = options.u ?: options.gitUser
        optionMap["gitPassword"] = options.p ?: options.gitPassword
        optionMap["gitBranch"] = options.b ?: options.gitBranch
        optionMap["dataBaseEnv"] = options.e ?: options.dataBaseEnv
        optionMap["dataBaseUser"] = options.d ?: options.dataBaseUser
        optionMap["dataBasePassword"] = options.s ?: options.dataBasePassword
        optionMap["operation"] = options.o ?: options.operation
        optionMap["release"] = options.r ?: options.release
        optionMap["projectId"] = options.i ?: options.projectId
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

    def static cloneGitRepository(String gitBaseDirectory, String repository, String branch) {

        def gitDirectory = new File(gitBaseDirectory)

        def git = org.eclipse.jgit.api.Git.cloneRepository().setGitDir(gitDirectory).setBranch(branch).setURI(repository).call()

        println("Cloned repository: " + git.getRepository().getDirectory())

        return CodeMessage.SUCCESS.value()

    }

}