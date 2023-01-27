#!/usr/bin/env groovy

package br.com.vivo

// https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-cli-commons
@Grapes(
        @Grab(group = 'org.codehaus.groovy', module = 'groovy-cli-commons', version = '3.0.14')
)

import groovy.cli.commons.CliBuilder

class Dboss {

    static void main(String[] args) {

        HashMap options = new Options().getOptions(args)

        options.println()

        def ret = new WorkFlow().execute(options)

        (options.get("verbose") == "y" || options.get("verbose") == "Y") ? println(CodeMessage.geMessageByValue(ret)) : null

        System.out.println(ret)

    }
}

class WorkFlow {

    static int execute(HashMap options) {

        def BASE_GIT_DIRECTORY = "/Git/"
        def current_directory = System.getenv("PWD") + BASE_GIT_DIRECTORY
        def verbose = (options.get("verbose") == "y" || options.get("verbose") == "Y")

        verbose ? println("STEP 1 - " + CodeMessage.VALIDATING_GIT_DIRECTORY.message() + ":" + current_directory) : null
        def ret = Validation.validateGitDirectory(current_directory)

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
    VALIDATING_GIT_DIRECTORY(6, 'Validating if Git directory exist in current directory.')

    CodeMessage(int value, String message) {
        this.value = value
        this.message = message
    }

    private final int value
    private final String message

    int value() { return value }

    String message() { return message }

    static String geMessageByValue(int value) {
        for (CodeMessage e : values()) {
            if (e.value == value) {
                return e.message()
            }
        }
        return null
    }

    static int geValue(int value) {
        for (CodeMessage e : values()) {
            if (e.value == value) {
                return e.value()
            }
        }
        return null
    }


}

class Options {

    HashMap getOptions(String[] args) {

        def cli = new CliBuilder(usage: 'groovy Dboss.groovy -u= -p= -b= -e= -d= -s= -o= -r= -i= [-v=]')

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
            v longOpt: 'verbose', args: 1, argName: 'verbose', required: false, defaultValue: 'n', 'Optional - Verbose output (y/n)'
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
        optionMap["verbose"] = options.v ?: options.verbose

        return optionMap

    }

}

class Util {

    static boolean isDirectory(String directory) {
        def file = new File(directory)
        return file.isDirectory()
    }

    static boolean createDirectory(String directory) {
        def file = new File(directory)
        return file.mkdir()
    }

}