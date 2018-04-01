import java.text.DecimalFormat;
import java.util.*;
import java.io.*;


public class FIFOManager{

	static int resources[];
	static Task tasks[];
	static Activity activities[];
	static int available[];
	static int tempResources[];
	static int allocation[][];
	static int cycle = 0;
	static ArrayList<Task> dummy_tasks = new ArrayList<Task>();
	static ArrayList<Task> blocked_tasks = new ArrayList<Task>();
	static ArrayList<Task> running_tasks = new ArrayList<Task>();
	static Task current_task_fifo = null;

	public FIFOManager(int[] resources,Task[] tasks,Activity[] activities){
		this.resources = resources.clone();
		this.available = resources.clone();
		this.tempResources = resources.clone();
		for(int r = 0; r < resources.length; r++){
			this.tempResources[r] = 0;
		}
		this.allocation = new int[tasks.length][resources.length];
		this.activities = new Activity[activities.length];
		for(int a = 0; a < activities.length; a++){
			Activity temp = activities[a];
			Activity tempCopy = new Activity(temp);
			this.activities[a] = tempCopy;
		}
		this.tasks = new Task[tasks.length];
		for(int t = 0; t < tasks.length; t++){
			Task temp = tasks[t];
			Task tempCopy = new Task(temp);
			this.tasks[t] = tempCopy;
		}
	}

	public void run(Boolean verbose){
		try{
			System.out.println("Running FIFO");
			runFIFO(verbose);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}



	private void runFIFO(Boolean verbose){

		//////////////////////////////////////////////////////////////////////////////////////////////////

		int cycle = resources.length;
		
		for(int i = 0; i < tasks.length ;i++)
			dummy_tasks.add(tasks[i]);
			
		while(dummy_tasks.size() > 0){ //keep running until all tasks have been executed

			for(int i = 0; i < dummy_tasks.size(); i++){ //take one activity of each task
				
				current_task_fifo = dummy_tasks.get(i);
				switch(current_task_fifo.getCurrentActivity().getType()){
					case "request":
						request(current_task_fifo);
						break;
					case "release":
						release(current_task_fifo);
						break;
					case "terminate":
						terminate(current_task_fifo,cycle);
						break;
					default:
						System.out.println("Activity not recognized");
						break;
				}
			}
			
			//checking for deadlocks; if all tasks are blocked then it is a deadlock
			if(blocked_tasks.size() > 0 && running_tasks.size() == 0)
				deadlock(cycle);
				

			for(int r = 0; r < resources.length; r++){
				available[r] += tempResources[r];
				tempResources[r] = 0;
			}

			//clearing buffers and adding blocked tasks before running tasks
			dummy_tasks.clear();
			dummy_tasks.addAll(blocked_tasks);
			dummy_tasks.addAll(running_tasks);
			blocked_tasks.clear();
			running_tasks.clear();
			
			cycle++;

			// System.out.println("Cycle - " + Integer.toString(cycle));
			// printAvailable();
			// printAllocation();
		}


		printFinal();

		//////////////////////////////////////////////////////////////////////////////////////////////////

	}



	//////////////////////////////////////////////////////////////////////////////////////////////////


	//function to set the next activity of a task to be done in the next cycle; name means to be done in the end. not end the task
	public static void end(Task task){
		Activity activity = task.getNextActivity();
		task.setDelayTime(activity.delay);
		running_tasks.add(task);
	}
	//function to make a task wait 
	public static void delayed(Task task){
		task.setState("delayed");
		task.decreaseDelayTime();
		if(task.finishedDelay())
			task.setState("running");
		running_tasks.add(task);
	}
	//function to grant/reject requests for tasks using fifo manager
	public static void request(Task task){	
		if(task.finishedDelay()){

			Activity activity = task.getCurrentActivity();
			int numberRequested = activity.number;

			//grant if enough resources available in the bank else wait
			if(available[activity.resourceType] >= numberRequested){

				available[activity.resourceType] -= numberRequested;
				allocation[activity.taskId][activity.resourceType] += numberRequested;
				task.grant(activity);
				end(task);

			}
			else{	
				task.incrementWaitTime();
				blocked_tasks.add(task);
			}
		}
		else
			if(task.delayTime > 0)
				delayed(task);
		
	}
	//function to release resources
	public static void release(Task task){

		if(task.finishedDelay()){
			Activity activity = task.getCurrentActivity();
			int numberReleased = activity.number;
			int res = task.resources[activity.resourceType];
			
			//cannot release if you dont have how many you want to release
			if(res >= numberReleased){	
				task.release(activity);
				tempResources[activity.resourceType] += numberReleased;
				allocation[activity.taskId][activity.resourceType] -= numberReleased;
				end(task);
			}
			else{
				task.incrementWaitTime();
				blocked_tasks.add(task);
			}
				
		}
		else
			if(task.delayTime > 0)
				delayed(task);

	}
	//function to terminate the task; sets state and finish time
	public static void terminate(Task task, int cycle){
		if(task.finishedDelay()){
			task.terminate(cycle);
		}
		else
			if(task.delayTime > 0)
				delayed(task);
		
	}
	//function to abort tasks in case of deadlocks; aborts all except the task with maximum id
	public static void deadlock(int cycle){

		int maxId = 0;
		int index = 0;
		
		for(int t = 0; t < blocked_tasks.size(); t++){	
			if(maxId < blocked_tasks.get(t).taskID){
				maxId = blocked_tasks.get(t).taskID;
				index = t;
			}
		}
		
		Task task = blocked_tasks.get(index);
		int i = 0;

		while(i < blocked_tasks.size()-1){	
			for(Task t:blocked_tasks){
				if(t.taskID != maxId){
					t.abort(cycle);
					for(int r = 0; r < resources.length; r++){ //releasing held resources
						tempResources[r] += t.resources[r];
						t.resources[r] = 0;
					}
				}
			}
			i++;
		}
		blocked_tasks.clear();
		blocked_tasks.add(task);//adding back the task with max id.
	}
	






	//////////////////////////////////////////////////////////////////////////////////////////////////



	public void printAvailable(){
		System.out.println("************* FIFO AVAILABLE *************");
		for(int i = 0; i < available.length; i++){
			System.out.print(available[i]+"  ");
		}
		System.out.println();
	}

	public void printAllocation(){
		System.out.println("************* FIFO ALLOCATION *************");
		for(int i = 0; i < tasks.length; i++){
			for(int j = 0; j < resources.length; j++){
				System.out.print(allocation[i][j]+"  ");
			}
			System.out.println();
		}
	}
	
	public void printFinal(){
		System.out.println("          FIFO ");
		DecimalFormat dfPrint = new DecimalFormat("####");
		for(int i = 0; i < tasks.length; i++){
			System.out.print("Task " + (tasks[i].taskID+1)+"       ");
			if(tasks[i].aborted()){
				System.out.print("Aborted");
			}
			else{
				float averageWait = ((float)tasks[i].waitTime)/tasks[i].totalTime*100;
				System.out.printf("%d	%d	%.0f%%",tasks[i].totalTime,tasks[i].waitTime,averageWait);
			}
			System.out.println();
		}
		System.out.print("Total        ");
		int totalTime = 0;
		int totalBlockedTime = 0;
		for(int i = 0; i < tasks.length; i++){
			totalTime += tasks[i].totalTime;
			totalBlockedTime += tasks[i].waitTime;
		}
		float overallAverageWait = ((float)totalBlockedTime)/totalTime*100;
		System.out.printf("%d	%d	%.0f%%",totalTime,totalBlockedTime,overallAverageWait);
		System.out.println();
	}


}