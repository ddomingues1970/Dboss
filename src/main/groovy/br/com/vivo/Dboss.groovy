package br.com.vivo

class Dboss {

    static void main(String[] args) {

        def ret = new WorkFlow().execute(Arrays.asList(args))

        System.out.println(ReturnCodeMessage.geMessageByValue(ret))

        System.exit(ret)
    }
}

class WorkFlow {

    int execute(List<String> args) {

        return new Validation().validate(args)

    }

}

class Validation {

    int validate(List<String> args) {

        args.forEach {
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

    public int value() {return value}
    public String message() {return message}

    public static String geMessageByValue(int value) {
        for (ReturnCodeMessage e : values()) {
            if (e.value == value) {
                return e.message();
            }
        }
        return null;
    }

}
