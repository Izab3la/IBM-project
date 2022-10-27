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
public interface AbstractInterface {
    public boolean registerJob(Job job);
    public JobState getJobState(String name);
}
