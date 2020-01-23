package main;

public class Task {
   
    private String[] task = new String[4];
  
	public Task(String typeService, String dateEnd, String status, String techService) {
		task[0] = typeService;
		task[1] = dateEnd;
		task[2] = status;
        task[3] = techService;
    }
    
    public String getTypeService() {
        return task[0];
    }
    
    public String getDateEnd() {
    	return task[1];
    }
    
    public String getStatus() {
    	return task[2];
	}

    public String getTechService() {
        return task[3];
    }
}
