package ua.edu.sumdu.wifi.wifichecker;

/** Class for utilities method */
public class Util {

    static String exceptionToString(Throwable e){
        StringBuilder result = new StringBuilder(e.getMessage());
        result.append("-----------\n");
        for (StackTraceElement line : e.getStackTrace()){
            result.append(line.toString());
            result.append("\n");
        }
        return result.toString();
    }

}
