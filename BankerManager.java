import java.text.DecimalFormat;
import java.util.*;
import java.io.*;


public class BankerManager{

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
	static Task current_task_banker = null;

	public BankerManager(int[] resources,Task[] tasks,Activity[] activities){
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
			System.out.println("Running Banker");
			runBanker(verbose);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}


//////////////////////////////////////////////////////////////////////////////////////////////////

	public static void runBanker(Boolean verbose){
		// Run cycle starts from after number of initiate activities/cycles
		int cycle = resources.length;

		// Terminating if claim greater than available
		for(int i = 0; i < tasks.length ;i++){
			Boolean acceptable = true;
			Task t = tasks[i];
			for(int r = 0; r < resources.length; r++){
				acceptable &= (t.resourcesClaim[r] <= resources[r]);
			}
			if(acceptable){
				dummy_tasks.add(t);
			}
			else{
				System.out.println("Task "+Integer.toString(i+1)+" aborted before start due to excess claim");
				t.abort(0);
			}
		}
			
		while(dummy_tasks.size() > 0){ //keep running until all tasks have been executed

			for(int i = 0; i < dummy_tasks.size(); i++){ //take one activity of each task
				
				current_task_banker = dummy_tasks.get(i);
				switch(current_task_banker.getCurrentActivity().getType()){
					case "request":
						request(current_task_banker,cycle);
						break;
					case "release":
						release(current_task_banker);
						break;
					case "terminate":
						terminate(current_task_banker,cycle);
						break;
					default:
						System.out.println("Activity not recognized");
						break;
				}
			}
			
			//checking for deadlocks; if all tasks are blocked then it is a deadlock
			if(blocked_tasks.size() > 0 && running_tasks.size() == 0){
				Boolean aborted = false;
				for(Task t: blocked_tasks){
					if(t.getState().equals("aborted")){
						aborted = true;
						for(int r = 0; r < resources.length; r++) 
							available[r] += t.resources[r];
						blocked_tasks.remove(t);
						break;
					}
				}
				
				if(!aborted)
					deadlock(cycle);
			}
				

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
	}


	//checks if the state upon resource grant will be a safe state
	public static Boolean safeState(Task task){

		Activity activity = task.getCurrentActivity();
		int[] availableCopy = available.clone();
		Boolean safe = true;
		Boolean notDeadlocked = false;
		// Task thisTaskCopy = new Task(task);
		ArrayList<Task> tasksListCopy = new ArrayList<Task>();
		for(Task t:dummy_tasks){ //creating a copy of tasks
			Task temp = new Task(t);
			if(temp.taskID == task.taskID){
				temp.resources[activity.resourceType] += activity.number;
				availableCopy[activity.resourceType] -= activity.number;
			}
			tasksListCopy.add(temp);
		}
		

		//state is safe if all tasks terminate meaning tasksListCopy is empty
		while(safe){

			safe = false;
			for(int j = 0; j < tasksListCopy.size(); j++){ 

				Task temp = tasksListCopy.get(j);
				notDeadlocked = true;

				for(int r = 0; r < availableCopy.length; r++){
					if((temp.resourcesClaim[r] - temp.resources[r]) > availableCopy[r]){ //if the task can finish given the current resources
						notDeadlocked = false;
						break;
					}
				}
				
				if(notDeadlocked){
					safe = true; //simulate task finished and resources returned
					for(int r = 0; r < availableCopy.length; r++)
						availableCopy[r] += temp.resources[r]; 
					
					tasksListCopy.remove(temp);
					
				}
			}

			if(tasksListCopy.isEmpty())
				return true;
		}
			
		return false;
	}
	//function for granting/rejecting requests; works by checking safe state status
	public static void request(Task task, int cycle){
		if(task.finishedDelay()){

			Activity activity = task.getCurrentActivity();
			int numberRequested = activity.number;

			Boolean safe = false;

			if(task.exceedsClaim(activity)){
				System.out.println("Task "+Integer.toString(task.taskID+1)+" aborted during cycle "+Integer.toString(cycle)+" due to excess claim");
				task.abort(cycle);
				blocked_tasks.add(task);
			}
			
			safe = safeState(task);

			// System.out.println("Task "+Integer.toString(task.taskID)+" safe - "+String.valueOf(safe)+" exceed - "+String.valueOf(task.exceedsClaim(activity)));

			//if the task is not aborted,we have resources and the state is safe, grant resources.
			if(!task.getState().equals("aborted")){

				if(available[activity.resourceType] >= numberRequested && safe){

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
		}
		else
			if(task.delayTime > 0)
				delayed(task);
	}

//////////////////////////////////////////////////////////////////////////////////////////////////

	

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



	public static void printAvailable(){
		System.out.println("************* Banker AVAILABLE *************");
		for(int i = 0; i < available.length; i++){
			System.out.print(available[i]+"  ");
		}
		System.out.println();
	}

	public static void printAllocation(){
		System.out.println("************* Banker ALLOCATION *************");
		for(int i = 0; i < tasks.length; i++){
			for(int j = 0; j < resources.length; j++){
				System.out.print(allocation[i][j]+"  ");
			}
			System.out.println();
		}
	}
	
	public static void printFinal(){
		System.out.println("          Banker ");
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