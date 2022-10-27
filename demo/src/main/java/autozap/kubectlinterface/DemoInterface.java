/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.kubectlinterface;

import autozap.dataobjects.Job;
import autozap.dataobjects.JobState;

/**
 *
 * @author piotr
 */
public class DemoInterface implements AbstractInterface{

    @Override
    public boolean registerJob(Job job) {
        return true;
    }

    @Override
    public JobState getJobState(String name) {
        if(!name.equals("demo")) return new JobState(
            name,
            "These are logs\nMore logs\neven more logs",
            name.contains("ERR") ? JobState.State.ERRORED : JobState.State.RUNNING
        );
        
        return new JobState(
                "Demo Job", 
                "[INFO]: Start Demo Job\n"
              + "[INFO]: Lorem ipsum dolor sit amet....\n"
              + "[INFO]: Demo Job has finished successfully",
                JobState.State.COMPLETED
        );
    }
    
}
