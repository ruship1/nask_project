
import org.orekit.attitudes.Attitude;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.models.earth.EarthShape;
import org.orekit.models.earth.Geoid;
import org.orekit.propagation.analytical.tle.*;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.opencsv.CSVWriter;




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
		final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();
		String[][] sat_info = new String[4000][50];
		
		try
		{
			br = new BufferedReader(new FileReader(csv));
			br.readLine();
			while((line = br.readLine()) != null)
			{
				String[] sat = line.split(split);
				sat_info[counter] = sat;
				TLE temp = new TLE(sat[24],sat[25]);
				Satellites[counter] = temp;
				counter++;
			}
			AbsoluteDate extrapDate = initialDate;
			PVCoordinatesProvider pvCoordinates = null;
			AttitudeProvider attitude = null;
			File file = new File("outputfile.csv");
			FileWriter outputfile = new FileWriter(file);
			CSVWriter writer  = new CSVWriter(outputfile);
			String[] header = {"norad_cat_id","object_name","object_type","classification_type","object_id","object_number","timestamp","location"};
			writer.writeNext(header);
			while(extrapDate.compareTo(endDate) <=0)
			{
				for(int i = 0; i < counter; i++)
				{
				
				
				
				PVCoordinates finalLoc = TLEPropagator.selectExtrapolator(Satellites[i]).getPVCoordinates(extrapDate);
				System.out.print(extrapDate + " ");
				System.out.println(sat_info[i][4]);
				String norad_id = Integer.toString(Satellites[i].getSatelliteNumber());
				String object_name = sat_info[i][4];
				String object_type = sat_info[i][5];
				String class_type = sat_info[i][6];
				String object_id = sat_info[i][26];
				String object_number = sat_info[i][27];
				String time_stamp = extrapDate.toString();
				String location = finalLoc.toString();
				String[] Line = {norad_id,object_name,object_type,class_type,object_id,object_number,time_stamp,location};
				writer.writeNext(Line);
				}
				extrapDate = extrapDate.shiftedBy(600);
			}
			writer.close();
			
			
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
