/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autozap.database;

import autozap.ConfigParameter;
import autozap.dataobjects.Job;
import autozap.dataobjects.Template;
import java.util.List;

/**
 *
 * @author piotr
 */
public interface AbstractDatabase {
    public boolean createTemplate(Template template);
    public Template getTemplateByID(int id);
    public List<Template> getAllTemplates();
    public boolean renameTemplate(int id, String newName);
    public boolean setTemplateParameter(int id, ConfigParameter parameter, Object newValue);
    
    public boolean createJob(Job job);
    public Job getJobByID(int id);
    public List<Job> getAllJobs();
    
    public boolean deleteTemplate(int id);
    public boolean deleteJob(int id);
}
