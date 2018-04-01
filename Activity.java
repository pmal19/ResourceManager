import java.util.*;
import java.io.*;

public class Activity{

	String type = "";
	int taskId = -1;
	int delay = 0;
	int resourceType = -1;
	int number = 0;

	public Activity(String type, int taskId, int delay, int resourceType, int number){
		this.type = type;
		this.taskId = taskId;
		this.delay = delay;
		this.resourceType = resourceType;
		this.number = number;
		String p = type + " " + Integer.toString(taskId) + " " + Integer.toString(delay) + " " + Integer.toString(resourceType) + " " + Integer.toString(number) + "init";
		// System.out.println(p);
	}

	public Activity(Activity ref){
		this.taskId = ref.taskId;
		this.delay = ref.delay;
		this.resourceType = ref.resourceType;
		this.number = ref.number;
		this.type = ref.type;
	}

	public String getType(){
		String p = type + Integer.toString(taskId);
		// System.out.println(p);
		return type;
	}

	public void print(){
		String p = type + " " + Integer.toString(taskId) + " " + Integer.toString(delay) + " " + Integer.toString(resourceType) + " " + Integer.toString(number);
		System.out.println(p);
	}

}