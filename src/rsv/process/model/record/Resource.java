package rsv.process.model.record;

public class Resource {
	private int id;
	public void setID(int it) { id = it; } 
	public int getID() { return id; }

	private String name;
	public void setName(String it) { name = it; } 
	public String getName() { return name; }
	
	private int group_id;
	public void setGroupID(int group_id) { this.group_id = group_id; } 
	public int getGroupID() { return this.group_id; }
}
