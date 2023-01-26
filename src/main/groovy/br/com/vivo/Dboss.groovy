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

        //def ret = new WorkFlow().execute(options)

        //System.out.println(ReturnCodeMessage.geMessageByValue(ret))

    }
}

class WorkFlow {

    int execute(HashMap options) {

        options.forEach {
            it.println()
        }

        //return new Validation().validateInputParameters(args)

    }

}

class Validation {

    int validateInputParameters(List<String> parameters) {

        parameters.forEach {
            System.out.println(it.toString())
        }

        return ReturnCodeMessage.SUCCESS_EXIT_CODE.value()
    }

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

        def cli = new CliBuilder(usage: 'groovy Dboss.groovy -u= -p=')

        /*
        vGitRepository=$1
        --vgitUser
        --vGitPassword=$3
        vGitBranch=$4
        vDatabaseEnv=$5
        vDatabaseUser=$6
        vDatabasePassword=$7
        vOperation=$8
        vRelease=$9
        vProjectID=${10}
        */

        cli.with {
            u longOpt: 'gitUser', args: 1, argName: 'gitUser', required: true, 'Git user name'
            p longOpt: 'gitPassword', args: 1, argName: 'gitPassword', required: true, 'Git password'
        }

        def optionMap = [:]
        def options = cli.parse(args)
        if (!options) {
            return optionMap
        }

        optionMap["gitUser"] = options.u ?: options.gitUser
        optionMap["gitPassword"] = options.p ?: options.gitPassword

        println("OPtionsMap size:" + optionMap.size())
        println(optionMap.get("gitUser"))
        println(optionMap.get("gitPassword"))

        return optionMap

    }

}


class Util {

    static splitStringBySign(String sign, String value) {

        return Arrays.asList(value.split(sign, value))

    }

}