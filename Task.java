import java.util.*;
import java.io.*;

public class Task{

    int taskID;
    String state = "initiate";
    int totalTime = 0;
    int waitTime = 0;
    int resourcesClaim[];
    int resources[];
    ArrayList<Activity> activities = new ArrayList<Activity>();
    int thisActivity = 0;
    int delayTime = 0;
    Activity currentActivity = null;

    public Task(int taskID, int totalResources){
        this.taskID = taskID;
        resources = new int[totalResources];
        resourcesClaim = new int[totalResources];
        for(int r = 0; r < totalResources; r++){
            resources[r] = 0;
            resourcesClaim[r] = 0;
        }
    }

    public Task(Task ref){
        this.taskID = ref.taskID;
        this.state = ref.state;
        this.totalTime = ref.totalTime;
        this.waitTime = ref.waitTime;
        this.resources = ref.resources.clone();
        this.resourcesClaim = ref.resourcesClaim.clone();
        this.activities.addAll(ref.activities);
        this.thisActivity = ref.thisActivity;
        this.delayTime = ref.delayTime;
        this.currentActivity = ref.currentActivity;
    }

    public Boolean exceedsClaim(Activity activity){
        return resources[activity.resourceType] + activity.number > resourcesClaim[activity.resourceType];
    }    

    public void release(Activity activity){
        resources[activity.resourceType] -= activity.number;
    }

    public void grant(Activity activity){
        resources[activity.resourceType] += activity.number;
    }

    public void terminate(int cycle){
        this.state = "terminated";
        this.totalTime = cycle;
    }

    public void abort(int cycle){
        this.state = "aborted";
        this.totalTime = 0;
        this.waitTime = 0;
        // System.out.println("Aborting Task - "+Integer.toString(taskID+1)+" cycle - "+Integer.toString(cycle));
    }

    public void claim(int resourceType, int totalClaim){
        resourcesClaim[resourceType] = totalClaim;
    }

    public void addActivity(Activity activity){
        // System.out.println(activity.type);
        this.activities.add(activity);
    }

    public Activity getNextActivity(){
        currentActivity = activities.get(++thisActivity);
        delayTime = currentActivity.delay;
        return currentActivity == null ? null : currentActivity;
    }

    public Activity getCurrentActivity(){
        if(currentActivity == null){
            currentActivity = activities.get(thisActivity);
            delayTime = currentActivity.delay;
        }
        return currentActivity;
    }

    public void resetCurrentActivity(){
        currentActivity = null;
    }

    // public void next(){
    //     thisActivity++;
    // }

    public String getState(){
        return state;
    }

    public void setState(String state){
        this.state = state;
    }

    public Boolean terminated(){
        return state.equals("terminated");
    }

    public Boolean aborted(){
        return state.equals("aborted");
    }

    public Boolean finished(){
        return terminated() || aborted();
    }

    public void setDelayTime(int delay){
        this.delayTime = delay;
    }

    public void decreaseDelayTime(){
        this.delayTime--;
    }

    public Boolean finishedDelay(){
        return this.delayTime <= 0;
    }

    public void incrementWaitTime(){
        this.waitTime++;
    }
}