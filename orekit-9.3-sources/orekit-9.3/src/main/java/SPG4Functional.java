import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.analytical.tle.*;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class SPG4Functional {
	
	public static void main(String[] args)
	{
		//csv file for input
		File orekitData = new File("C:\\Users\\Rushi\\eclipse-workspace\\orekit-9.3-sources");
		DataProvidersManager manager = DataProvidersManager.getInstance();
		manager.addProvider(new DirectoryCrawler(orekitData));
		String csv = "/Users/Rushi/csv/SpacetrackResults.csv";
		String date_time_range = "2008-09-15T15:53:00Z|2008-09-15T17:53:00Z";
		String duration = "PT5H0M0S";
		Functional(csv,date_time_range,duration);
	}
	
	public static void Functional(String csv, String date_time_range,String duration)
	{
		BufferedReader br = null;
		String line = "";
		String split = ",";
		TLE[] Satellites = new TLE[4000];
		int counter = 0;
		int position = date_time_range.indexOf('|');
		String start = date_time_range.substring(0,position);
		String end = date_time_range.substring(position+1);
		DateTimeComponents startTemp = DateTimeComponents.parseDateTime(start);
		DateTimeComponents endTemp = DateTimeComponents.parseDateTime(end);
		TimeScale utc = TimeScalesFactory.getUTC();
		AbsoluteDate initialDate = new AbsoluteDate(startTemp,utc);
		AbsoluteDate endDate = new AbsoluteDate(endTemp,utc);
		Frame inertialFrame = FramesFactory.getEME2000();
		try
		{
			br = new BufferedReader(new FileReader(csv));
			br.readLine();
			while((line = br.readLine()) != null)
			{
				String[] sat = line.split(split);
				TLE temp = new TLE(sat[24],sat[25]);
				Satellites[counter] = temp;
				counter++;
			}
			AbsoluteDate extrapDate = initialDate;
			PVCoordinatesProvider pvCoordinates = null;
			AttitudeProvider attitude = null;
			while(extrapDate.compareTo(endDate) <=0)
			{
				for(int i = 0; i < counter+1; i++)
				{
				pvCoordinates.getPVCoordinates(extrapDate, inertialFrame);
				attitude.getAttitude(pvCoordinates, extrapDate, inertialFrame);
				SGP4 predict = new SGP4(Satellites[i], attitude, 1000.00);
				PVCoordinates finalLoc = predict.getPVCoordinates(extrapDate);
				System.out.println(extrapDate + finalLoc.toString());
				}
			
				
				extrapDate = extrapDate.shiftedBy(600);
			}
			
			
		}
		catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}

	

}
