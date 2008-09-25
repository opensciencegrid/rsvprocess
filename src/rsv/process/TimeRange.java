package rsv.process;

import java.util.ArrayList;


//TODO - this algorithm can made to be a lot smarter so that it will 
//create multiple ranges that we really need to update instead of
//a single range containing entire begining -> end.
//I will keep ArrayList although currently we always have single range..
//so that the users of this class doesn't have to be modified when
//in the future we will upgrade this algorithm.
public class TimeRange
{
	public class TimePeriod
	{
		public int start;
		public int end;
		public TimePeriod(int _start, int _end) {
			start = _start;
			end = _end;
		}
	}
	
	private ArrayList<TimePeriod> ranges = new ArrayList<TimePeriod>();

	public void add(int start, int end)
	{
		if(ranges.size() == 0) {
			ranges.add(new TimePeriod(start, end));
		} else {
			TimePeriod range = ranges.get(0);
			if(range.start > start) {
				range.start = start;
			}
			if(range.end < end) {
				range.end = end;
			}
		}
	}
	public ArrayList<TimePeriod> getRanges()
	{
		return ranges;
	}
}