package com.example.mwastools.UtilityClasses.NetorkUtility;

/**
 * This class holds the output fields of a remote bash script after or during its execution.
 */
public class ReturnPayload {
    public int status_code = 0;
    public int exitCode = -2001;
    public String stdout = "";
    public String stderr = "";

    ReturnPayload(int status_code, int exitCode, String stdout, String stderr) {
        this.status_code = status_code;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    ReturnPayload(){}
}

