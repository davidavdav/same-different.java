package src;

import java.util.*;
import java.text.*;

public class Task {
	public Task(String line) {
		String [] parts = line.split("\\s+");
		num = parts[0];
		wavA = parts[1];
		wavB = parts[2];
		extra = "";
		for (int i = 3; i < parts.length; ++i)
			extra += parts[i] + " ";
		extra = extra.trim();
		if (extra.length() == 0)
			extra = null;
	}
	public String num, wavA, wavB, extra;
	public float same;
	public ArrayList<String> times = new ArrayList<String>();
	public String toString() {
		String t="";
		NumberFormat nf = NumberFormat.getInstance(); 
		nf.setMinimumIntegerDigits(1);
		t=times.remove(0);
		while (times.size()>0) {
			t = t + ',' + times.remove(0);
		}
		return 
			num + " " + wavA + " " + wavB + " " + 
			nf.format(same) + " " + t + " " +
			(extra == null ? "" : " " + extra);
	}
}
