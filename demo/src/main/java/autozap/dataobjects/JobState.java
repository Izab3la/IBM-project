/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.dataobjects;

/**
 *
 * @author piotr
 */
public class JobState {
    private String jobName;
    private String jobLogs;
    private State jobState;

    public JobState(String jobName, String jobLogs, State jobState) {
        this.jobName = jobName;
        this.jobLogs = jobLogs;
        this.jobState = jobState;
    }
    
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobLogs() {
        return jobLogs;
    }

    public void setJobLogs(String jobLogs) {
        this.jobLogs = jobLogs;
    }

    public State getJobState() {
        return jobState;
    }

    public void setJobState(State jobState) {
        this.jobState = jobState;
    }
    
    public static enum State{
        RUNNING,
        COMPLETED,
        ERRORED;
    }
}
