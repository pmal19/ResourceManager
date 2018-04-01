import java.util.*;
import java.io.*;

public class ResourceManager{

	private String fileName;
	private Boolean verbose;
	private int resources[];
	private Task tasks[];
	private Activity activities[];

	private void readFile(){
		try{
			Scanner fileReader = new Scanner(new File(fileName));
			int numberOfTasks = fileReader.nextInt();
			int numberOfTypesOfResources = fileReader.nextInt();
			resources = new int[numberOfTypesOfResources];
			for(int r = 0; r < numberOfTypesOfResources; r++){
				resources[r] = fileReader.nextInt();
			}
			// System.out.println(Arrays.toString(resources));
			tasks = new Task[numberOfTasks];
			for(int t = 0; t < numberOfTasks; t++){
				Task thisTask = new Task(t,numberOfTypesOfResources);
				tasks[t] = thisTask;
			}
			int count = 0;
			while(fileReader.hasNextLine()){
				String line = fileReader.nextLine();
				if(!line.contentEquals("")){
					if(!line.contains("initiate")){
						count++;
					}
				}
			}
			activities = new Activity[count];
			fileReader.close();

			Scanner activityReader = new Scanner(new File(fileName));
			activityReader.nextInt();
			activityReader.nextInt();
			for(int r = 0; r < numberOfTypesOfResources; r++){
				activityReader.nextInt();
			}
			int a = 0;
			while(activityReader.hasNext()){
				String line = activityReader.nextLine();
				if(!line.contentEquals("")){
					String[] data = line.split("\\s+");
					String type = data[0];
					int taskId = Integer.parseInt(data[1]) - 1;
					int delay = Integer.parseInt(data[2]);
					int resourceType = Integer.parseInt(data[3]) - 1;
					int number = Integer.parseInt(data[4]);

					if(data[0].equals("initiate")){
						tasks[taskId].claim(resourceType,number);
					}
					else{
						Activity thisActivity = new Activity(type, taskId, delay, resourceType, number);
						activities[a++] = thisActivity;
						tasks[taskId].addActivity(thisActivity);
					}
				}
			}
			activityReader.close();
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}

	private void run(Boolean verbose){
		System.out.println("---------------");
        readFile();
        FIFOManager fistInFirstOut = new FIFOManager(resources,tasks,activities);
        fistInFirstOut.run(verbose);
        System.out.println("---------------");
        BankerManager bank = new BankerManager(resources,tasks,activities);
        bank.run(verbose);
        System.out.println("---------------");
    }

	public ResourceManager(String fileName,Boolean verbose){
        this.fileName = fileName;
        this.verbose = verbose;
    }

	public static void main(String[] args){
        if(args.length > 2 || args.length < 1)
            throw new IllegalArgumentException("Incorrect number of parameters.");
        Boolean verboseArg = (args.length == 2);
        String fileNameArg = (args.length == 2) ? args[1] : args[0];
        ResourceManager resourceManager = new ResourceManager(fileNameArg,verboseArg);
        resourceManager.run(verboseArg);
    }

}