package br.com.vivo

// https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-cli-commons
@Grapes(
        @Grab(group='org.codehaus.groovy', module='groovy-cli-commons', version='3.0.14')
)

import groovy.cli.commons.CliBuilder

class Dboss {

    static void main(String[] args) {

        HashMap options = new Options().getOptions(args)

        options.println()

        def ret = new WorkFlow().execute(options)

        System.out.println(ReturnCodeMessage.geMessageByValue(ret))

    }
}

class WorkFlow {

    int execute(HashMap options) {

        //TODO: implement Validations

        println options

        return ReturnCodeMessage.SUCCESS_EXIT_CODE.value()

        //return new Validation().validateInputParameters(args)

    }

}

class Validation {

    //TODO: Implement validation methods

}

enum ReturnCodeMessage {

    SUCCESS_EXIT_CODE(0, 'Execution with success'),
    FAIL(1, 'Execution failed')

    ReturnCodeMessage(int value, String message) {
        this.value = value
        this.message = message
    }

    private final int value
    private final String message

    public int value() { return value }

    public String message() { return message }

    public static String geMessageByValue(int value) {
        for (ReturnCodeMessage e : values()) {
            if (e.value == value) {
                return e.message();
            }
        }
        return null;
    }


}

class Options {

    HashMap getOptions(String[] args) {

        def cli = new CliBuilder(usage: 'groovy Dboss.groovy -u= -p= -b= -e= -d= -s= -o= -r= -i=')

        cli.with {
            u longOpt: 'gitUsfr', args: 1, argName: 'gitUser', required: true, 'Git user name'
            p longOpt: 'gitPassword', args: 1, argName: 'gitPassword', required: true, 'Git password'
            b longOpt: 'gitBranch', args: 1, argName: 'gitBranch', required: true, 'Git branch'
            e longOpt: 'dataBaseEnv', args: 1, argName: 'dataBaseEnv', required: true, 'Database env'
            d longOpt: 'dataBaseUser', args: 1, argName: 'dataBaseUser', required: true, 'Database user'
            s longOpt: 'dataBasePassword', args: 1, argName: 'dataBasePassword', required: true, 'Database password'
            o longOpt: 'operation', args: 1, argName: 'operation', required: true, 'Operation'
            r longOpt: 'release', args: 1, argName: 'release', required: true, 'Release'
            i longOpt: 'projectId', args: 1, argName: 'projectId', required: true, 'Project Id'
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

        return optionMap

    }

}
