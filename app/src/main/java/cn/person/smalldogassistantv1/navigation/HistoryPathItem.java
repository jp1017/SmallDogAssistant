package cn.person.smalldogassistantv1.navigation;


public class HistoryPathItem {
	private String startname = null;
	private String endname = null;
	
	public HistoryPathItem(String startname, String endname) {
		this.startname = startname;
		this.endname = endname;
	}

	public String getStartname() {
		return startname;
	}

	public void setStartname(String startname) {
		this.startname = startname;
	}

	public String getEndname() {
		return endname;
	}

	public void setEndname(String endname) {
		this.endname = endname;
	}

}
